package org.ihebut.patent.patent.controller;

import org.ihebut.patent.patent.dto.ApiResponse;
import org.ihebut.patent.patent.dto.ValuationCreateRequest;
import org.ihebut.patent.patent.dto.ValuationParamUpdateRequest;
import org.ihebut.patent.patent.dto.ValuationReportResponse;
import org.ihebut.patent.patent.entity.PatentBase;
import org.ihebut.patent.patent.entity.PatentValuationModelParam;
import org.ihebut.patent.patent.entity.PatentValuationReport;
import org.ihebut.patent.patent.entity.UserPatent;
import org.ihebut.patent.patent.mapper.PatentValuationModelParamMapper;
import org.ihebut.patent.patent.mapper.PatentValuationReportMapper;
import org.ihebut.patent.patent.mapper.UserPatentMapper;
import org.ihebut.patent.patent.security.CurrentUser;
import org.ihebut.patent.patent.search.PatentSearchDocument;
import org.ihebut.patent.patent.service.PatentEsService;
import org.ihebut.patent.patent.service.PatentTableService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
@RequestMapping("/api")
public class ValuationController {
    private static final Set<String> VALID_PATENT_CATEGORIES = Set.of("wind", "solar", "biomass", "hydrogen", "lilon");

    private final PatentValuationReportMapper reportMapper;
    private final PatentTableService patentTableService;
    private final PatentEsService patentEsService;
    private final UserPatentMapper userPatentMapper;
    private final PatentValuationModelParamMapper paramMapper;
    private final CurrentUser currentUser;

    public ValuationController(
            PatentValuationReportMapper reportMapper,
            PatentTableService patentTableService,
            PatentEsService patentEsService,
            UserPatentMapper userPatentMapper,
            PatentValuationModelParamMapper paramMapper,
            CurrentUser currentUser
    ) {
        this.reportMapper = reportMapper;
        this.patentTableService = patentTableService;
        this.patentEsService = patentEsService;
        this.userPatentMapper = userPatentMapper;
        this.paramMapper = paramMapper;
        this.currentUser = currentUser;
    }

    @PostMapping("/valuations")
    public ApiResponse<ValuationReportResponse> create(@RequestBody ValuationCreateRequest request) {
        if (request == null || request.getPatentSource() == null || request.getPatentSource().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "patentSource不能为空");
        }
        String source = request.getPatentSource().trim().toUpperCase();
        PatentValuationReport r = new PatentValuationReport();
        r.setPatentSource(source);
        r.setDatasetId(request.getDatasetId());
        r.setRecordId(firstNonBlank(request.getRecordId(), request.getPatentPublicNum()));
        r.setModelVersion(request.getModelVersion());

        String title = "";
        String abs = "";
        if ("EXTERNAL".equals(source)) {
            PatentSearchDocument p = resolveExternalPatent(request).orElse(null);
            if (p == null) {
                throw new ResponseStatusException(BAD_REQUEST, "专利不存在");
            }
            r.setDatasetId(firstNonBlankLong(request.getDatasetId(), p.getDatasetId()));
            r.setRecordId(firstNonBlank(request.getRecordId(), p.getRecordId(), p.getPublicNum()));
            r.setPatentCategory(firstNonBlank(normalizePatentCategory(p.getCategory()), normalizePatentCategory(request.getPatentCategory())));
            r.setPatentPublicNum(firstNonBlank(p.getPublicNum(), request.getPatentPublicNum(), request.getRecordId()));
            title = safe(p.getTitle());
            abs = safe(firstNonBlank(p.getAbstractText(), ""));
        } else if ("USER".equals(source)) {
            if (request.getUserPatentId() == null) {
                throw new ResponseStatusException(BAD_REQUEST, "userPatentId不能为空");
            }
            UserPatent p = userPatentMapper.findById(request.getUserPatentId())
                    .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "用户专利不存在"));
            r.setUserPatentId(p.getId());
            r.setPatentCategory(p.getCategory());
            r.setPatentPublicNum(p.getPublicNum());
            title = safe(p.getTitle());
            abs = safe(p.getAbstractText());
        } else {
            throw new ResponseStatusException(BAD_REQUEST, "patentSource不合法");
        }

        // ---------------------------------------------
        // 智能评分：四维（0~100）+ 前端权重（默认均等）
        // ---------------------------------------------
        Map<String, Double> weightMap = normalizeWeights(request.getWeights());

        double techScore = computeTechnologicalInnovationScore(title, abs);
        double marketScore = computeMarketPotentialScore(title, abs);
        // 法律状态与经济价值使用启发式关键词从 title/abs 中推断（不改库结构）
        double legalScore = computeLegalStatusScore(title + " " + abs);
        double economicScore = computeEconomicValueScore(title, abs);

        double overallPercent =
                (techScore * weightMap.getOrDefault("technologicalInnovation", 25d)
                        + marketScore * weightMap.getOrDefault("marketPotential", 25d)
                        + legalScore * weightMap.getOrDefault("legalStatus", 25d)
                        + economicScore * weightMap.getOrDefault("economicValue", 25d))
                        / 100d;
        overallPercent = clamp(overallPercent, 0d, 100d);

        double predictedValueYuan = (overallPercent / 100d) * 1_000_000d;

        // 实体落库：旧字段仍按 0~1 保存（前端以 reportJson 展示四维结构）
        r.setTechValueScore(to01Score(techScore));
        r.setMarketValueScore(to01Score(marketScore));
        r.setTransformationPotentialScore(to01Score(economicScore));
        r.setOverallScore(BigDecimal.valueOf(overallPercent / 100d).setScale(3, RoundingMode.HALF_UP));
        r.setPredictedValue(BigDecimal.valueOf(predictedValueYuan).setScale(2, RoundingMode.HALF_UP));

        // 构造前端展示结构
        LocalDateTime now = LocalDateTime.now();
        String valuationDate = DateTimeFormatter.ISO_DATE_TIME.format(now);
        String reportTitle = "智能评估报告 - " + safe(r.getPatentPublicNum());

        ValuationReportResponse response = new ValuationReportResponse();
        response.setPatentSource(source);
        response.setDatasetId(request.getDatasetId());
        response.setRecordId(request.getRecordId());
        response.setUserPatentId(request.getUserPatentId());
        response.setPatentCategory(r.getPatentCategory());
        response.setPatentPublicNum(r.getPatentPublicNum());
        response.setReportTitle(reportTitle);
        response.setValuationDate(valuationDate);
        response.setModelVersion(request.getModelVersion());
        response.setPredictedValue(predictedValueYuan);
        response.setComprehensiveScore(overallPercent);
        response.setWeights(toIntWeightMap(weightMap));

        ValuationReportResponse.DimensionScores dimScores = buildDimensionScores(techScore, marketScore, legalScore, economicScore);
        response.setDimensionScores(dimScores);
        response.setConclusion(buildConclusion(dimScores));
        response.setPatentInfo(buildPatentInfo(r.getPatentPublicNum(), title, "", "", abs, ""));

        // 保存 reportJson：列表接口可还原四维/结论
        try {
            Map<String, Object> reportContent = new LinkedHashMap<>();
            reportContent.put("method", "smart-heuristic");
            reportContent.put("reportTitle", reportTitle);
            reportContent.put("valuationDate", valuationDate);
            reportContent.put("dimensionScores", response.getDimensionScores());
            reportContent.put("conclusion", response.getConclusion());
            reportContent.put("patentInfo", response.getPatentInfo());
            reportContent.put("weights", response.getWeights());
            reportContent.put("datasetId", request.getDatasetId());
            reportContent.put("recordId", request.getRecordId());
            reportContent.put("overallPercent", overallPercent);
            reportContent.put("predictedValue", predictedValueYuan);

            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            r.setReportJson(mapper.writeValueAsString(reportContent));
        } catch (Exception ignored) {
            r.setReportJson("{\"method\":\"smart-heuristic\",\"overallPercent\":" + overallPercent + "}");
        }

        PatentValuationReport saved = reportMapper.save(r);
        response.setId(saved.getId());
        return ApiResponse.ok(response);
    }

    @GetMapping("/valuations")
    public ApiResponse<List<ValuationReportResponse>> list(
            @RequestParam String patentSource,
            @RequestParam(required = false) Long datasetId,
            @RequestParam(required = false) String recordId,
            @RequestParam(required = false) String patentCategory,
            @RequestParam(required = false) String patentPublicNum,
            @RequestParam(required = false) Long userPatentId
    ) {
        if (patentSource == null || patentSource.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "patentSource不能为空");
        }
        String source = patentSource.trim().toUpperCase();
        if ("EXTERNAL".equals(source)) {
            boolean hasDatasetRecord = datasetId != null && recordId != null && !recordId.isBlank();
            if (hasDatasetRecord) {
                return ApiResponse.ok(
                        reportMapper
                                .findByPatentSourceAndDatasetIdAndRecordId(source, datasetId, recordId.trim())
                                .stream()
                                .map(this::toResponse)
                                .toList()
                );
            }
            String normalizedCategory = normalizePatentCategory(patentCategory);
            boolean hasCat = normalizedCategory != null;
            boolean hasPub = patentPublicNum != null && !patentPublicNum.isBlank();
            if (hasCat && hasPub) {
                return ApiResponse.ok(
                        reportMapper
                                .findByPatentSourceAndPatentCategoryAndPatentPublicNum(source, normalizedCategory, patentPublicNum.trim())
                                .stream()
                                .map(this::toResponse)
                                .toList()
                );
            }
            // 列表页默认不带筛选条件：返回全部 EXTERNAL 记录
            return ApiResponse.ok(reportMapper.findByPatentSource(source).stream().map(this::toResponse).toList());
        }
        if ("USER".equals(source)) {
            if (userPatentId == null) {
                throw new ResponseStatusException(BAD_REQUEST, "userPatentId不能为空");
            }
            return ApiResponse.ok(
                    reportMapper.findByPatentSourceAndUserPatentId(source, userPatentId)
                            .stream()
                            .map(this::toResponse)
                            .toList()
            );
        }
        throw new ResponseStatusException(BAD_REQUEST, "patentSource不合法");
    }

    @PutMapping("/valuation-params/{key}")
    @Transactional
    public ApiResponse<PatentValuationModelParam> updateParam(@PathVariable String key, @RequestBody ValuationParamUpdateRequest request) {
        long userId = currentUser.requireUserId();
        if (key == null || key.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "key不能为空");
        }
        PatentValuationModelParam param = paramMapper.findByParamKey(key).orElse(null);
        if (param == null) {
            param = new PatentValuationModelParam();
            param.setParamKey(key);
        }
        if (request != null) {
            if (request.getParamValue() != null) param.setParamValue(request.getParamValue());
        }
        param.setUpdatedBy(userId);
        return ApiResponse.ok(paramMapper.save(param));
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private Optional<PatentSearchDocument> resolveExternalPatent(ValuationCreateRequest request) {
        if (request.getDatasetId() != null && request.getRecordId() != null && !request.getRecordId().isBlank()) {
            Optional<PatentSearchDocument> exact = patentEsService.findByDatasetRecord(request.getDatasetId(), request.getRecordId().trim());
            if (exact.isPresent()) {
                return exact;
            }
        }

        String publicNum = firstNonBlank(request.getPatentPublicNum(), request.getRecordId());
        if (publicNum != null) {
            Optional<PatentSearchDocument> byPublicNum = patentEsService.findByPublicNum(publicNum);
            if (byPublicNum.isPresent()) {
                return byPublicNum;
            }
        }

        String normalizedCategory = normalizePatentCategory(request.getPatentCategory());
        if (normalizedCategory != null && request.getPatentPublicNum() != null && !request.getPatentPublicNum().isBlank()) {
            PatentBase fallback = patentTableService.get(normalizedCategory, request.getPatentPublicNum().trim());
            if (fallback != null) {
                PatentSearchDocument doc = new PatentSearchDocument();
                doc.setCategory(normalizedCategory);
                doc.setPublicNum(request.getPatentPublicNum().trim());
                doc.setTitle(fallback.getTitle());
                doc.setAbstractText(fallback.getAbstractText());
                doc.setApplicant(fallback.getApplicant());
                doc.setInventor(fallback.getInventor());
                return Optional.of(doc);
            }
        }

        return Optional.empty();
    }

    private static String normalizePatentCategory(String category) {
        if (category == null) {
            return null;
        }
        String normalized = category.trim().toLowerCase();
        return VALID_PATENT_CATEGORIES.contains(normalized) ? normalized : null;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private static Long firstNonBlankLong(Long... values) {
        if (values == null) {
            return null;
        }
        for (Long value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static BigDecimal score(int value, int min, int max) {
        if (value <= min) return BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP);
        if (value >= max) return BigDecimal.ONE.setScale(3, RoundingMode.HALF_UP);
        BigDecimal d = BigDecimal.valueOf(value - min).divide(BigDecimal.valueOf(max - min), 3, RoundingMode.HALF_UP);
        return d.max(BigDecimal.ZERO).min(BigDecimal.ONE);
    }

    private static int keywordHits(String text) {
        String t = text == null ? "" : text.toLowerCase();
        int hits = 0;
        for (String k : new String[]{"转化", "许可", "产业", "市场", "降本", "增效", "ai", "算法", "预测", "维护"}) {
            if (t.contains(k)) hits++;
        }
        return hits;
    }

    private static int keywordHits(String text, String[] keywords) {
        String t = text == null ? "" : text.toLowerCase();
        int hits = 0;
        for (String k : keywords) {
            if (k == null || k.isBlank()) continue;
            if (t.contains(k.toLowerCase())) hits++;
        }
        return hits;
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    private static BigDecimal to01Score(double percentScore) {
        double v = clamp(percentScore, 0d, 100d) / 100d;
        return BigDecimal.valueOf(v).setScale(3, RoundingMode.HALF_UP);
    }

    private static Map<String, Double> normalizeWeights(ValuationCreateRequest.ValuationWeights weights) {
        // 默认：四维均等
        Map<String, Double> out = new HashMap<>();
        out.put("technologicalInnovation", 25d);
        out.put("marketPotential", 25d);
        out.put("legalStatus", 25d);
        out.put("economicValue", 25d);

        if (weights == null) return out;

        double ti = weights.getTechnologicalInnovation() == null ? 0d : weights.getTechnologicalInnovation();
        double mp = weights.getMarketPotential() == null ? 0d : weights.getMarketPotential();
        double ls = weights.getLegalStatus() == null ? 0d : weights.getLegalStatus();
        double ev = weights.getEconomicValue() == null ? 0d : weights.getEconomicValue();
        double sum = ti + mp + ls + ev;
        if (sum <= 0.0001d) return out;

        out.put("technologicalInnovation", ti * 100d / sum);
        out.put("marketPotential", mp * 100d / sum);
        out.put("legalStatus", ls * 100d / sum);
        out.put("economicValue", ev * 100d / sum);
        return out;
    }

    private static Map<String, Integer> toIntWeightMap(Map<String, Double> weightMap) {
        Map<String, Integer> out = new LinkedHashMap<>();
        out.put("technologicalInnovation", weightMap.getOrDefault("technologicalInnovation", 25d).intValue());
        out.put("marketPotential", weightMap.getOrDefault("marketPotential", 25d).intValue());
        out.put("legalStatus", weightMap.getOrDefault("legalStatus", 25d).intValue());
        out.put("economicValue", weightMap.getOrDefault("economicValue", 25d).intValue());
        return out;
    }

    private static double computeTechnologicalInnovationScore(String title, String abs) {
        int baseLen = title == null ? 0 : title.length();
        BigDecimal tech01 = score(baseLen, 20, 120);

        int hits = keywordHits(title + " " + abs, new String[]{
                "高分子", "复合", "制备", "工艺", "催化", "膜", "装置", "电解液", "碳酸酯", "沸石", "聚合物", "方法", "制备方法", "装置"
        });
        double boost = hits * 4d; // 0~多个关键词
        return clamp(tech01.doubleValue() * 100d * 0.85 + boost, 0d, 100d);
    }

    private static double computeMarketPotentialScore(String title, String abs) {
        int len = abs == null ? 0 : abs.length();
        BigDecimal market01 = score(len, 50, 600);
        int hits = keywordHits(title + " " + abs, new String[]{"市场", "产业", "应用", "规模", "需求", "转化", "工程", "落地", "产值", "产量"});
        double boost = hits * 3.5d;
        return clamp(market01.doubleValue() * 100d * 0.8 + boost, 0d, 100d);
    }

    private static double computeLegalStatusScore(String legalText) {
        String t = legalText == null ? "" : legalText;
        if (t.trim().isEmpty()) return 30d;

        int pos = keywordHits(t, new String[]{"有效", "维持", "授权", "仍有效", "许可", "专利权"});
        int neg = keywordHits(t, new String[]{"无效", "撤销", "终止", "到期", "失效", "被宣告无效", "无权"});
        double net = pos - neg;
        double percent = 50d + net * 12d - Math.max(0, neg - 1) * 8d;
        return clamp(percent, 0d, 100d);
    }

    private static double computeEconomicValueScore(String title, String abs) {
        String t = (title == null ? "" : title) + " " + (abs == null ? "" : abs);
        int hits = keywordHits(t, new String[]{"转化", "许可", "产业化", "收益", "利润", "增值", "投资", "合同额", "分成", "预付款", "付款", "成本", "产值"});
        int numHits = keywordNumberHits(t);
        double percent = 35d + hits * 7.5d + numHits * 10d;
        return clamp(percent, 0d, 100d);
    }

    private static int keywordNumberHits(String text) {
        if (text == null) return 0;
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            if (Character.isDigit(text.charAt(i))) {
                count++;
                while (i + 1 < text.length() && Character.isDigit(text.charAt(i + 1))) i++;
            }
        }
        return Math.min(5, count);
    }

    private static ValuationReportResponse.DimensionScores buildDimensionScores(
            double tech, double market, double legal, double economic
    ) {
        ValuationReportResponse.DimensionScores ds = new ValuationReportResponse.DimensionScores();
        ds.setTechnologicalInnovation(buildDimensionScore(tech, "基于标题/关键词的技术创新潜力推断。"));
        ds.setMarketPotential(buildDimensionScore(market, "基于摘要长度与市场/转化相关关键词推断。"));
        ds.setLegalStatus(buildDimensionScore(legal, "基于法律有效/终止等关键词命中程度的启发式推断。"));
        ds.setEconomicValue(buildDimensionScore(economic, "基于许可/转化/收益相关关键词与数字信息推断经济价值。"));
        return ds;
    }

    private static ValuationReportResponse.DimensionScore buildDimensionScore(double score, String analysis) {
        ValuationReportResponse.DimensionScore ds = new ValuationReportResponse.DimensionScore();
        ds.setScore(Math.round(score * 10d) / 10d);
        ds.setFullScore(100d);
        ds.setAnalysis(analysis);
        ds.setKeyPoints(List.of(analysis, "建议结合材料补充与尽调，提升结论可信度。"));
        return ds;
    }

    private static ValuationReportResponse.Conclusion buildConclusion(ValuationReportResponse.DimensionScores ds) {
        double tech = ds.getTechnologicalInnovation() == null ? 0d : ds.getTechnologicalInnovation().getScore();
        double market = ds.getMarketPotential() == null ? 0d : ds.getMarketPotential().getScore();
        double legal = ds.getLegalStatus() == null ? 0d : ds.getLegalStatus().getScore();
        double economic = ds.getEconomicValue() == null ? 0d : ds.getEconomicValue().getScore();

        ValuationReportResponse.Conclusion c = new ValuationReportResponse.Conclusion();
        c.setSummary("综合评估显示该专利具备一定转化与许可价值，建议优先推进市场验证与法律有效性核验后开展合作。");

        // strengths：取前两项
        double[] arr = new double[]{tech, market, legal, economic};
        String[] keys = new String[]{"technologicalInnovation", "marketPotential", "legalStatus", "economicValue"};
        int[] idx = new int[]{0, 1, 2, 3};
        // 简化排序：按分数降序取前两项
        for (int i = 0; i < idx.length; i++) {
            for (int j = i + 1; j < idx.length; j++) {
                if (arr[idx[j]] > arr[idx[i]]) {
                    int tmp = idx[i];
                    idx[i] = idx[j];
                    idx[j] = tmp;
                }
            }
        }
        c.setStrengths(List.of(labelOf(keys[idx[0]]), labelOf(keys[idx[1]])));

        // risks：任一低于 50 认为风险
        java.util.ArrayList<String> risks = new java.util.ArrayList<>();
        if (tech < 50) risks.add("技术创新证据与对比材料需要加强。");
        if (market < 50) risks.add("市场潜力与应用场景需要进一步验证。");
        if (legal < 50) risks.add("法律有效性与权利稳定性需补充尽调。");
        if (economic < 50) risks.add("经济价值测算与收益模型需补充。");
        c.setRisks(risks.isEmpty() ? List.of("各维度评分相对均衡，整体风险可控。") : risks);

        // recommendations：根据风险维度给建议
        c.setRecommendations(List.of(
                "优先完成权利有效性核验与法律状态更新。",
                "同步开展市场需求调研与潜在合作方筛选。",
                "将关键技术点与可验证数据补齐，用于支撑谈判。"
        ));
        c.setSuggestion("建议按“法律核验 -> 市场验证 -> 合作谈判”的顺序推进，提升转化落地概率。");
        return c;
    }

    private static String labelOf(String key) {
        return switch (key) {
            case "technologicalInnovation" -> "技术创新";
            case "marketPotential" -> "市场潜力";
            case "legalStatus" -> "法律状态";
            case "economicValue" -> "经济价值";
            default -> key;
        };
    }

    private static ValuationReportResponse.PatentInfo buildPatentInfo(
            String publicNum,
            String title,
            String applicant,
            String inventor,
            String abstractText,
            String ipcClass
    ) {
        ValuationReportResponse.PatentInfo pi = new ValuationReportResponse.PatentInfo();
        pi.setPublicNum(publicNum);
        pi.setTitle(title);
        pi.setApplicant(applicant);
        pi.setInventor(inventor);
        pi.setAbstractText(abstractText);
        pi.setIpcClass(ipcClass);
        return pi;
    }

    private ValuationReportResponse toResponse(PatentValuationReport entity) {
        ValuationReportResponse response = new ValuationReportResponse();
        response.setId(entity.getId());
        response.setPatentSource(entity.getPatentSource());
        response.setPatentCategory(entity.getPatentCategory());
        response.setPatentPublicNum(entity.getPatentPublicNum());
        response.setUserPatentId(entity.getUserPatentId());
        response.setModelVersion(entity.getModelVersion());
        response.setPredictedValue(entity.getPredictedValue() == null ? 0d : entity.getPredictedValue().doubleValue());
        response.setComprehensiveScore(entity.getOverallScore() == null ? 0d : entity.getOverallScore().doubleValue() * 100d);
        if (entity.getCreatedAt() != null) {
            response.setValuationDate(DateTimeFormatter.ISO_DATE_TIME.format(entity.getCreatedAt()));
        }
        response.setReportTitle("智能评估报告 - " + safe(entity.getPatentPublicNum()));

        // 尝试还原 reportJson
        try {
            if (entity.getReportJson() != null && entity.getReportJson().trim().startsWith("{")) {
                var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                var root = mapper.readTree(entity.getReportJson());

                if (root.hasNonNull("reportTitle")) response.setReportTitle(root.get("reportTitle").asText());
                if (root.hasNonNull("valuationDate")) response.setValuationDate(root.get("valuationDate").asText());
                if (root.has("datasetId") && !root.get("datasetId").isNull()) response.setDatasetId(root.get("datasetId").asLong());
                if (root.hasNonNull("recordId")) response.setRecordId(root.get("recordId").asText());

                if (root.has("dimensionScores") && !root.get("dimensionScores").isNull()) {
                    response.setDimensionScores(mapper.treeToValue(root.get("dimensionScores"), ValuationReportResponse.DimensionScores.class));
                }
                if (root.has("conclusion") && !root.get("conclusion").isNull()) {
                    response.setConclusion(mapper.treeToValue(root.get("conclusion"), ValuationReportResponse.Conclusion.class));
                }
                if (root.has("patentInfo") && !root.get("patentInfo").isNull()) {
                    response.setPatentInfo(mapper.treeToValue(root.get("patentInfo"), ValuationReportResponse.PatentInfo.class));
                }
                if (root.has("weights") && !root.get("weights").isNull()) {
                    response.setWeights(mapper.convertValue(root.get("weights"), Map.class));
                }
            }
        } catch (Exception ignored) {
        }

        if (response.getDimensionScores() == null) {
            double tech = entity.getTechValueScore() == null ? 0d : entity.getTechValueScore().doubleValue() * 100d;
            double market = entity.getMarketValueScore() == null ? 0d : entity.getMarketValueScore().doubleValue() * 100d;
            double economic = entity.getTransformationPotentialScore() == null ? 0d : entity.getTransformationPotentialScore().doubleValue() * 100d;
            double legal = 30d;
            response.setDimensionScores(buildDimensionScores(tech, market, legal, economic));
        }
        if (response.getConclusion() == null) {
            response.setConclusion(buildConclusion(response.getDimensionScores()));
        }
        if (response.getPatentInfo() == null) {
            response.setPatentInfo(buildPatentInfo(
                    entity.getPatentPublicNum(),
                    "",
                    "",
                    "",
                    "",
                    ""
            ));
        }
        return response;
    }
}
