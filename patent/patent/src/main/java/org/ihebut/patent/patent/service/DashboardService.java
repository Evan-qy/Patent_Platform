package org.ihebut.patent.patent.service;

import org.ihebut.patent.patent.dto.DashboardScreenResponse;
import org.ihebut.patent.patent.entity.ExpertProfile;
import org.ihebut.patent.patent.entity.TransformationResult;
import org.ihebut.patent.patent.mapper.ExpertProfileMapper;
import org.ihebut.patent.patent.mapper.OrganizationMapper;
import org.ihebut.patent.patent.mapper.PatentBiomassMapper;
import org.ihebut.patent.patent.mapper.PatentHydrogenMapper;
import org.ihebut.patent.patent.mapper.PatentLilonMapper;
import org.ihebut.patent.patent.mapper.PatentSolarMapper;
import org.ihebut.patent.patent.mapper.PatentWindMapper;
import org.ihebut.patent.patent.mapper.TransformationResultMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Service
public class DashboardService {
    private static final Logger log = LoggerFactory.getLogger(DashboardService.class);

    private final PatentWindMapper patentWindMapper;
    private final PatentSolarMapper patentSolarMapper;
    private final PatentBiomassMapper patentBiomassMapper;
    private final PatentHydrogenMapper patentHydrogenMapper;
    private final PatentLilonMapper patentLilonMapper;
    private final ExpertProfileMapper expertProfileMapper;
    private final OrganizationMapper organizationMapper;
    private final TransformationResultMapper transformationResultMapper;

    public DashboardService(
            PatentWindMapper patentWindMapper,
            PatentSolarMapper patentSolarMapper,
            PatentBiomassMapper patentBiomassMapper,
            PatentHydrogenMapper patentHydrogenMapper,
            PatentLilonMapper patentLilonMapper,
            ExpertProfileMapper expertProfileMapper,
            OrganizationMapper organizationMapper,
            TransformationResultMapper transformationResultMapper
    ) {
        this.patentWindMapper = patentWindMapper;
        this.patentSolarMapper = patentSolarMapper;
        this.patentBiomassMapper = patentBiomassMapper;
        this.patentHydrogenMapper = patentHydrogenMapper;
        this.patentLilonMapper = patentLilonMapper;
        this.expertProfileMapper = expertProfileMapper;
        this.organizationMapper = organizationMapper;
        this.transformationResultMapper = transformationResultMapper;
    }

    public DashboardScreenResponse getScreenData() {
        List<DashboardScreenResponse.DashboardPatentCategoryItem> categories = buildPatentCategories();
        long patentTotal = categories.stream().mapToLong(DashboardScreenResponse.DashboardPatentCategoryItem::count).sum();

        List<TransformationResult> transformations = safeFetchTransformations();
        BigDecimal transformationBenefitTotal = transformations.stream()
                .map(TransformationResult::getBenefitAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long expertTotal = safeCount(expertProfileMapper::count, "expert_profile");
        long enterpriseTotal = safeCount(organizationMapper::count, "organization");

        return new DashboardScreenResponse(
                OffsetDateTime.now().toString(),
                new DashboardScreenResponse.DashboardMetricSummary(
                        patentTotal,
                        transformationBenefitTotal,
                        expertTotal,
                        enterpriseTotal
                ),
                categories,
                buildPatentTrend(patentTotal, transformations),
                buildDatasetRanking(categories),
                buildExpertFields(),
                buildLatestTransformations(transformations)
        );
    }

    private List<DashboardScreenResponse.DashboardPatentCategoryItem> buildPatentCategories() {
        List<DashboardScreenResponse.DashboardPatentCategoryItem> items = new ArrayList<>();
        items.add(new DashboardScreenResponse.DashboardPatentCategoryItem("wind", "风能", safeCount(patentWindMapper::count, "patent_wind")));
        items.add(new DashboardScreenResponse.DashboardPatentCategoryItem("solar", "光伏", safeCount(patentSolarMapper::count, "patent_solar")));
        items.add(new DashboardScreenResponse.DashboardPatentCategoryItem("biomass", "生物质", safeCount(patentBiomassMapper::count, "patent_biomass")));
        items.add(new DashboardScreenResponse.DashboardPatentCategoryItem("hydrogen", "氢能", safeCount(patentHydrogenMapper::count, "patent_hydrogen")));
        items.add(new DashboardScreenResponse.DashboardPatentCategoryItem("lilon", "锂电", safeCount(patentLilonMapper::count, "patent_lilon")));
        return items;
    }

    private List<DashboardScreenResponse.DashboardTrendItem> buildPatentTrend(long patentTotal, List<TransformationResult> transformations) {
        Map<YearMonth, Long> transformationByMonth = new LinkedHashMap<>();
        YearMonth current = YearMonth.now();
        for (int i = 5; i >= 0; i--) {
            transformationByMonth.put(current.minusMonths(i), 0L);
        }

        for (TransformationResult item : transformations) {
            LocalDate date = item.getTransformationDate();
            if (date == null) {
                continue;
            }
            YearMonth ym = YearMonth.from(date);
            if (transformationByMonth.containsKey(ym)) {
                transformationByMonth.put(ym, transformationByMonth.get(ym) + 1);
            }
        }

        long estimatedMonthlyPatentCount = patentTotal <= 0
                ? 0
                : Math.max(1, Math.round((double) patentTotal / transformationByMonth.size()));

        List<DashboardScreenResponse.DashboardTrendItem> items = new ArrayList<>();
        transformationByMonth.forEach((month, count) -> items.add(
                new DashboardScreenResponse.DashboardTrendItem(month.toString(), estimatedMonthlyPatentCount, count)
        ));
        return items;
    }

    private List<DashboardScreenResponse.DashboardDatasetRankingItem> buildDatasetRanking(
            List<DashboardScreenResponse.DashboardPatentCategoryItem> categories
    ) {
        List<DashboardScreenResponse.DashboardDatasetRankingItem> items = new ArrayList<>();
        long datasetId = 1;
        for (DashboardScreenResponse.DashboardPatentCategoryItem item : categories) {
            items.add(new DashboardScreenResponse.DashboardDatasetRankingItem(
                    datasetId++,
                    item.category(),
                    item.label() + "专利库",
                    item.count()
            ));
        }
        items.sort((a, b) -> Long.compare(b.count(), a.count()));
        return items;
    }

    private List<DashboardScreenResponse.DashboardExpertFieldItem> buildExpertFields() {
        Map<String, Long> fieldCounts = new LinkedHashMap<>();
        try {
            for (ExpertProfile profile : expertProfileMapper.findAll()) {
                String field = profile.getField();
                if (field == null || field.isBlank()) {
                    field = "未分类";
                }
                fieldCounts.put(field, fieldCounts.getOrDefault(field, 0L) + 1);
            }
        } catch (Exception e) {
            log.warn("Failed to aggregate expert fields", e);
        }

        if (fieldCounts.isEmpty()) {
            fieldCounts.put("未分类", 0L);
        }

        return fieldCounts.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(6)
                .map(entry -> new DashboardScreenResponse.DashboardExpertFieldItem(entry.getKey(), entry.getValue()))
                .toList();
    }

    private List<DashboardScreenResponse.DashboardTransformationItem> buildLatestTransformations(List<TransformationResult> transformations) {
        return transformations.stream()
                .limit(8)
                .map(item -> new DashboardScreenResponse.DashboardTransformationItem(
                        item.getId() == null ? 0L : item.getId(),
                        buildTransformationTitle(item),
                        item.getBenefitAmount(),
                        item.getTransformationDate() == null ? null : item.getTransformationDate().toString()
                ))
                .toList();
    }

    private List<TransformationResult> safeFetchTransformations() {
        try {
            return transformationResultMapper.findAll(Sort.by(
                    Sort.Order.desc("transformationDate"),
                    Sort.Order.desc("createdAt")
            ));
        } catch (Exception e) {
            log.warn("Failed to load transformation results", e);
            return List.of();
        }
    }

    private long safeCount(Supplier<Long> supplier, String sourceName) {
        try {
            Long value = supplier.get();
            return value == null ? 0L : value;
        } catch (Exception e) {
            log.warn("Failed to count {}", sourceName, e);
            return 0L;
        }
    }

    private String buildTransformationTitle(TransformationResult item) {
        if (item.getDescription() != null && !item.getDescription().isBlank()) {
            return item.getDescription().length() > 32
                    ? item.getDescription().substring(0, 32)
                    : item.getDescription();
        }
        if (item.getPatentPublicNum() != null && !item.getPatentPublicNum().isBlank()) {
            return "专利转化 " + item.getPatentPublicNum();
        }
        return "转化项目 #" + item.getId();
    }
}
