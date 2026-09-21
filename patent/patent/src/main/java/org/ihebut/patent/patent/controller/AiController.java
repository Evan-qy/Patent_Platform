package org.ihebut.patent.patent.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.ihebut.patent.patent.dto.AiChatRequest;
import org.ihebut.patent.patent.dto.AiChatResponse;
import org.ihebut.patent.patent.dto.ApiResponse;
import org.ihebut.patent.patent.search.PatentSearchDocument;
import org.ihebut.patent.patent.security.CurrentUser;
import org.ihebut.patent.patent.service.AuditLogService;
import org.ihebut.patent.patent.service.DashScopeChatService;
import org.ihebut.patent.patent.service.PatentEsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/ai")
public class AiController {
    private static final Logger log = LoggerFactory.getLogger(AiController.class);
    private static final int MAX_AI_PATENT_RESULTS = 100;
    private static final Pattern COMPACT_PUBLIC_NUM_PATTERN = Pattern.compile(
            "(?i)(?:CN|US|EP|WO|JP|KR|AU|IN|JO)\\d{4,}[A-Z0-9.\\-/]*?(?=(?:CN|US|EP|WO|JP|KR|AU|IN|JO)\\d{4,}|$)"
    );
    private static final Pattern PUBLIC_NUM_PATTERN = Pattern.compile(
            "(?i)\\b(?:CN|US|EP|WO|JP|KR)?\\d{4,}[A-Z0-9.\\-/]*[A-Z]?\\b"
    );
    private final DashScopeChatService dashScopeChatService;
    private final PatentEsService patentEsService;
    private final AuditLogService auditLogService;
    private final CurrentUser currentUser;

    public AiController(
            DashScopeChatService dashScopeChatService,
            PatentEsService patentEsService,
            AuditLogService auditLogService,
            CurrentUser currentUser
    ) {
        this.dashScopeChatService = dashScopeChatService;
        this.patentEsService = patentEsService;
        this.auditLogService = auditLogService;
        this.currentUser = currentUser;
    }

    @PostMapping("/chat")
    public ApiResponse<AiChatResponse> chat(@RequestBody AiChatRequest request, HttpServletRequest httpRequest) {
        try {
            AiChatResponse response = dashScopeChatService.chat(request);
            logAiAction(httpRequest, "AI 对话", request == null ? null : request.getQuestion(), true);
            return ApiResponse.ok(response);
        } catch (Exception e) {
            logAiAction(httpRequest, "AI 对话", request == null ? null : request.getQuestion(), false);
            return ApiResponse.fail(e.getMessage() == null ? "请求失败" : e.getMessage());
        }
    }

    @PostMapping("/chat/patent")
    public ApiResponse<Map<String, Object>> chatWithPatent(@RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        String requirement = request.get("requirement");
        String sessionId = request.get("sessionId");

        if (requirement == null || requirement.isBlank()) {
            logAiAction(httpRequest, "AI 专利场景搜索", "requirement为空", false);
            return ApiResponse.fail("requirement不能为空");
        }

        try {
            AiChatResponse aiResp = dashScopeChatService.chatWithApp(buildPatentSearchPrompt(requirement), sessionId, true);
            String aiContent = aiResp.getAnswer();

            List<String> publicNums = extractPublicNums(aiContent);
            List<PatentSearchDocument> patents = java.util.Collections.emptyList();
            if (!publicNums.isEmpty()) {
                log.info("AI提取专利号，准备查询ES: {}", publicNums);
                patents = patentEsService.findByPublicNums(publicNums);
                log.info("ES查询结果数量: {}", patents.size());
            }

            String fallbackQuery = (requirement + " " + aiContent).trim();
            int fallbackSize = Math.min(MAX_AI_PATENT_RESULTS, Math.max(30, publicNums.isEmpty() ? 50 : publicNums.size() * 3));
            if (patents.isEmpty()) {
                log.info("AI patent exact match is empty, falling back to ES full-text search, size={}", fallbackSize);
                patents = patentEsService.searchDocuments(null, fallbackQuery, fallbackSize);
                log.info("ES full-text fallback result count: {}", patents.size());
            } else if (patents.size() < MAX_AI_PATENT_RESULTS) {
                log.info("AI patent exact match is limited, supplementing with ES full-text search, size={}", fallbackSize);
                List<PatentSearchDocument> fallbackPatents = patentEsService.searchDocuments(null, fallbackQuery, fallbackSize);
                patents = mergePatents(patents, fallbackPatents, MAX_AI_PATENT_RESULTS);
                log.info("ES merged patent result count: {}", patents.size());
            }

            Set<String> foundNums = patents.stream()
                    .map(PatentSearchDocument::getPublicNum)
                    .map(this::normalizePublicNum)
                    .collect(Collectors.toSet());

            List<String> notFoundPublicNums = publicNums.stream()
                    .filter(num -> !foundNums.contains(normalizePublicNum(num)))
                    .toList();

            Map<String, Object> result = new HashMap<>();
            result.put("aiAnalysis", aiContent);
            result.put("extractedPublicNums", publicNums);
            result.put("patents", patents.stream().map(this::toPatentResult).toList());
            result.put("notFoundPublicNums", notFoundPublicNums);
            result.put("requestId", aiResp.getRequestId());

            logAiAction(httpRequest, "AI 专利场景搜索", requirement, true);
            return ApiResponse.ok(result);
        } catch (Exception e) {
            logAiAction(httpRequest, "AI 专利场景搜索", requirement, false);
            return ApiResponse.fail(e.getMessage() == null ? "请求失败" : e.getMessage());
        }
    }

    private void logAiAction(HttpServletRequest request, String action, String detail, boolean success) {
        try {
            long userId = currentUser.requireUserId();
            auditLogService.logUserAction(request, userId, "QUERY", action, "ai_chat", null, detail, success);
        } catch (Exception ignored) {
            auditLogService.logAnonymousAction(request, null, "QUERY", action, detail, success);
        }
    }

    private String buildPatentSearchPrompt(String requirement) {
        String cleanRequirement = requirement == null ? "" : requirement.trim();
        return """
                你是专利检索助手。请围绕用户需求尽可能全面地分析并返回结果。
                要求：
                1. 优先识别并列出尽可能多的相关专利公开号，不要只给少量样例。
                2. 如果无法确定精确公开号，也要尽量给出更多相关技术关键词、核心部件、应用场景、同义词和检索线索。
                3. 输出中把公开号单独清晰列出，避免连写。
                4. 尽量覆盖不同实现路线、上下游部件、控制方法、系统结构、应用场景。

                用户需求：
                """ + cleanRequirement;
    }

    private List<String> extractPublicNums(String content) {
        if (content == null || content.isBlank()) {
            return List.of();
        }
        LinkedHashSet<String> publicNums = new LinkedHashSet<>();
        Matcher matcher = PUBLIC_NUM_PATTERN.matcher(content);
        while (matcher.find()) {
            String normalized = normalizePublicNum(matcher.group());
            if (normalized.length() > 5 && !normalized.matches("\\d{4}")) {
                publicNums.add(normalized);
            }
        }
        Matcher compactMatcher = COMPACT_PUBLIC_NUM_PATTERN.matcher(normalizePublicNum(content));
        while (compactMatcher.find()) {
            String normalized = normalizePublicNum(compactMatcher.group());
            if (normalized.length() > 5 && !normalized.matches("\\d{4}")) {
                publicNums.add(normalized);
            }
        }
        return List.copyOf(publicNums);
    }

    private String normalizePublicNum(String value) {
        return value == null ? "" : value.replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
    }

    private Map<String, Object> toPatentResult(PatentSearchDocument patent) {
        Map<String, Object> item = new HashMap<>();
        item.put("datasetId", patent.getDatasetId());
        item.put("datasetName", patent.getDatasetName());
        item.put("recordId", patent.getRecordId());
        item.put("category", patent.getCategory());
        item.put("publicNum", patent.getPublicNum());
        item.put("title", patent.getTitle());
        item.put("abstractText", patent.getAbstractText());
        item.put("applicant", patent.getApplicant());
        item.put("inventor", patent.getInventor());
        item.put("ipc", patent.getIpc());
        item.put("cpc", patent.getCpc());
        item.put("legalStatus", patent.getLegalStatus());
        item.put("appliDate", patent.getAppliDate());
        item.put("publicDate", patent.getPublicDate());
        return item;
    }

    private List<PatentSearchDocument> mergePatents(List<PatentSearchDocument> exactMatches, List<PatentSearchDocument> fallbackMatches, int limit) {
        List<PatentSearchDocument> merged = new java.util.ArrayList<>();
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        for (PatentSearchDocument patent : exactMatches) {
            addPatent(merged, seen, patent, limit);
        }
        for (PatentSearchDocument patent : fallbackMatches) {
            addPatent(merged, seen, patent, limit);
        }
        return merged;
    }

    private void addPatent(List<PatentSearchDocument> patents, Set<String> seen, PatentSearchDocument patent, int limit) {
        if (patent == null || patents.size() >= limit) {
            return;
        }
        String key = patent.getDatasetId() + ":" + normalizePublicNum(patent.getPublicNum());
        if (seen.add(key)) {
            patents.add(patent);
        }
    }
}
