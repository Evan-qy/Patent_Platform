package org.ihebut.patent.patent.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ihebut.patent.patent.dto.HomeContentPayload;
import org.ihebut.patent.patent.entity.CmsArticle;
import org.ihebut.patent.patent.entity.HomeContent;
import org.ihebut.patent.patent.mapper.CmsArticleMapper;
import org.ihebut.patent.patent.mapper.HomeContentMapper;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CmsService {
    private static final Map<String, String> LEGACY_TEXT_MAP = Map.ofEntries(
            Map.entry("Patent service platform", "专利资产全生命周期管理平台"),
            Map.entry("Professional, intelligent and efficient patent services", "专业、智能、高效的专利成果转化服务"),
            Map.entry("Search, evaluate and transform patent assets in one workflow.", "围绕专利检索、需求发布、价值评估与成果转化，提供一体化数字化运营能力。"),
            Map.entry("Core capabilities", "核心功能模块"),
            Map.entry("Patent management, expert matching, demand docking and mobile services.", "面向高校、科研团队和企业场景，覆盖专利管理、需求对接、专家匹配与转化协同。"),
            Map.entry("Industry articles", "行业资讯与政策解读"),
            Map.entry("News, policies and patent transformation insights.", "关注专利转化动态，掌握政策前沿与典型案例。"),
            Map.entry("Partner universities", "服务高校"),
            Map.entry("Covers universities, labs and operators.", "覆盖重点院校、科研团队与成果运营机构。"),
            Map.entry("Patent assets", "入库专利成果"),
            Map.entry("Supports patent, demand and valuation data.", "持续沉淀专利、需求与评估报告数据。"),
            Map.entry("Higher efficiency", "匹配效率提升"),
            Map.entry("Speeds up search, evaluation and matching.", "加快检索、评估与成果转化推进节奏。")
    );

    private final CmsArticleMapper articleMapper;
    private final HomeContentMapper homeContentMapper;
    private final ObjectMapper objectMapper;

    @jakarta.annotation.PostConstruct
    public void init() {
        try {
            if (homeContentMapper.count() > 0) {
                return;
            }

            HomeContent entity = new HomeContent();
            entity.setHeroTitle("专利资产全生命周期管理平台");
            entity.setBrandSlogan("专业、智能、高效的专利成果转化服务");
            entity.setHeroDescription("围绕专利检索、需求发布、价值评估与成果转化，提供一体化数字化运营能力。");
            entity.setCoreFeaturesTitle("核心功能模块");
            entity.setCoreFeaturesIntro("面向高校、科研团队和企业场景，覆盖专利管理、需求对接、专家匹配与转化协同。");
            entity.setArticlesSectionTitle("行业资讯与政策解读");
            entity.setArticlesSectionSubtitle("关注专利转化动态，掌握政策前沿与典型案例。");

            List<HomeContentPayload.MetricItem> metrics = new ArrayList<>();
            metrics.add(createMetric("120+", "服务高校", "覆盖重点院校、科研团队与成果运营机构。"));
            metrics.add(createMetric("3200+", "入库专利成果", "持续沉淀专利、需求与评估报告数据。"));
            metrics.add(createMetric("40%", "匹配效率提升", "加快检索、评估与成果转化推进节奏。"));

            entity.setHeroMetricsJson(writeJson(metrics));
            entity.setFeaturesJson("[]");
            entity.setAdvantagesJson("[]");
            entity.setMobileOperationTipsJson("[]");
            entity.setMobileServicePromisesJson("[]");
            entity.setHeroHighlightsJson("[]");
            entity.setCaseKpisJson("[]");

            homeContentMapper.save(entity);
            log.info("Initialized default CMS home content");
        } catch (JsonProcessingException ex) {
            log.error("Failed to build default CMS home content JSON", ex);
        } catch (DataAccessException ex) {
            log.warn("CMS tables are unavailable during startup. Application will continue without CMS data. Root cause: {}",
                    rootMessage(ex));
        }
    }

    private HomeContentPayload.MetricItem createMetric(String value, String label, String detail) {
        HomeContentPayload.MetricItem item = new HomeContentPayload.MetricItem();
        item.setValue(value);
        item.setLabel(label);
        item.setDetail(detail);
        return item;
    }

    public List<CmsArticle> getPublishedArticles() {
        try {
            return articleMapper.findAllByStatusOrderBySortOrderAscCreatedAtDesc("PUBLISHED");
        } catch (DataAccessException ex) {
            log.warn("CMS article table is unavailable, returning empty public article list. Root cause: {}", rootMessage(ex));
            return List.of();
        }
    }

    public Optional<CmsArticle> getPublishedArticle(String slug) {
        try {
            return articleMapper.findBySlug(slug).filter(article -> "PUBLISHED".equals(article.getStatus()));
        } catch (DataAccessException ex) {
            log.warn("CMS article table is unavailable, returning empty article detail. Root cause: {}", rootMessage(ex));
            return Optional.empty();
        }
    }

    public List<CmsArticle> getAllArticles() {
        try {
            return articleMapper.findAll();
        } catch (DataAccessException ex) {
            throw cmsUnavailable(ex);
        }
    }

    public CmsArticle saveArticle(CmsArticle article) {
        try {
            if (article.getId() == null) {
                article.setCreatedAt(LocalDateTime.now());
            } else {
                articleMapper.findById(article.getId()).ifPresent(existing -> {
                    if (article.getCreatedAt() == null) {
                        article.setCreatedAt(existing.getCreatedAt());
                    }
                    if (article.getPublishedAt() == null) {
                        article.setPublishedAt(existing.getPublishedAt());
                    }
                });
            }
            if ("PUBLISHED".equals(article.getStatus()) && article.getPublishedAt() == null) {
                article.setPublishedAt(LocalDateTime.now());
            }
            return articleMapper.save(article);
        } catch (DataAccessException ex) {
            throw cmsUnavailable(ex);
        }
    }

    public void deleteArticle(Long id) {
        try {
            articleMapper.deleteById(id);
        } catch (DataAccessException ex) {
            throw cmsUnavailable(ex);
        }
    }

    public HomeContentPayload getHomeContent() {
        try {
            HomeContent entity = homeContentMapper.findAll().stream().findFirst().orElse(null);
            if (entity == null) {
                return defaultHomeContent();
            }
            return entityToDto(entity);
        } catch (DataAccessException ex) {
            log.warn("CMS home content table is unavailable, returning default public content. Root cause: {}", rootMessage(ex));
            return defaultHomeContent();
        }
    }

    @Transactional
    public HomeContentPayload updateHomeContent(HomeContentPayload dto) {
        try {
            HomeContent entity = homeContentMapper.findAll().stream().findFirst().orElse(new HomeContent());
            dtoToEntity(dto, entity);
            homeContentMapper.save(entity);
            return entityToDto(entity);
        } catch (DataAccessException ex) {
            throw cmsUnavailable(ex);
        }
    }

    private HomeContentPayload entityToDto(HomeContent entity) {
        HomeContentPayload dto = defaultHomeContent();
        dto.setHeroTitle(entity.getHeroTitle());
        dto.setBrandSlogan(entity.getBrandSlogan());
        dto.setHeroDescription(entity.getHeroDescription());
        dto.setCoreFeaturesTitle(entity.getCoreFeaturesTitle());
        dto.setCoreFeaturesIntro(entity.getCoreFeaturesIntro());
        dto.setArticlesSectionTitle(entity.getArticlesSectionTitle());
        dto.setArticlesSectionSubtitle(entity.getArticlesSectionSubtitle());

        try {
            if (entity.getHeroMetricsJson() != null) {
                dto.setHeroMetrics(objectMapper.readValue(entity.getHeroMetricsJson(), new TypeReference<>() {}));
            }
            if (entity.getFeaturesJson() != null) {
                dto.setFeatures(objectMapper.readValue(entity.getFeaturesJson(), new TypeReference<>() {}));
            }
            if (entity.getAdvantagesJson() != null) {
                dto.setAdvantages(objectMapper.readValue(entity.getAdvantagesJson(), new TypeReference<>() {}));
            }
            if (entity.getMobileOperationTipsJson() != null) {
                dto.setMobileOperationTips(objectMapper.readValue(entity.getMobileOperationTipsJson(), new TypeReference<>() {}));
            }
            if (entity.getMobileServicePromisesJson() != null) {
                dto.setMobileServicePromises(objectMapper.readValue(entity.getMobileServicePromisesJson(), new TypeReference<>() {}));
            }
            if (entity.getHeroHighlightsJson() != null) {
                dto.setHeroHighlights(objectMapper.readValue(entity.getHeroHighlightsJson(), new TypeReference<>() {}));
            }
            if (entity.getCaseKpisJson() != null) {
                dto.setCaseKpis(objectMapper.readValue(entity.getCaseKpisJson(), new TypeReference<>() {}));
            }
        } catch (JsonProcessingException ex) {
            log.error("Failed to parse CMS JSON content", ex);
        }

        return normalizeLegacyHomeContent(dto);
    }

    private void dtoToEntity(HomeContentPayload dto, HomeContent entity) {
        entity.setHeroTitle(dto.getHeroTitle());
        entity.setBrandSlogan(dto.getBrandSlogan());
        entity.setHeroDescription(dto.getHeroDescription());
        entity.setCoreFeaturesTitle(dto.getCoreFeaturesTitle());
        entity.setCoreFeaturesIntro(dto.getCoreFeaturesIntro());
        entity.setArticlesSectionTitle(dto.getArticlesSectionTitle());
        entity.setArticlesSectionSubtitle(dto.getArticlesSectionSubtitle());

        try {
            entity.setHeroMetricsJson(writeJson(dto.getHeroMetrics()));
            entity.setFeaturesJson(writeJson(dto.getFeatures()));
            entity.setAdvantagesJson(writeJson(dto.getAdvantages()));
            entity.setMobileOperationTipsJson(writeJson(dto.getMobileOperationTips()));
            entity.setMobileServicePromisesJson(writeJson(dto.getMobileServicePromises()));
            entity.setHeroHighlightsJson(writeJson(dto.getHeroHighlights()));
            entity.setCaseKpisJson(writeJson(dto.getCaseKpis()));
        } catch (JsonProcessingException ex) {
            log.error("Failed to serialize CMS JSON content", ex);
        }
    }

    private HomeContentPayload defaultHomeContent() {
        HomeContentPayload dto = new HomeContentPayload();
        dto.setHeroTitle("专利资产全生命周期管理平台");
        dto.setBrandSlogan("专业、智能、高效的专利成果转化服务");
        dto.setHeroDescription("围绕专利检索、需求发布、价值评估与成果转化，提供一体化数字化运营能力。");
        dto.setCoreFeaturesTitle("核心功能模块");
        dto.setCoreFeaturesIntro("面向高校、科研团队和企业场景，覆盖专利管理、需求对接、专家匹配与转化协同。");
        dto.setArticlesSectionTitle("行业资讯与政策解读");
        dto.setArticlesSectionSubtitle("关注专利转化动态，掌握政策前沿与典型案例。");

        List<HomeContentPayload.MetricItem> metrics = new ArrayList<>();
        metrics.add(createMetric("120+", "服务高校", "覆盖重点院校、科研团队与成果运营机构。"));
        metrics.add(createMetric("3200+", "入库专利成果", "持续沉淀专利、需求与评估报告数据。"));
        metrics.add(createMetric("40%", "匹配效率提升", "加快检索、评估与成果转化推进节奏。"));
        dto.setHeroMetrics(metrics);
        dto.setFeatures(new ArrayList<>());
        dto.setAdvantages(new ArrayList<>());
        dto.setMobileOperationTips(new ArrayList<>());
        dto.setMobileServicePromises(new ArrayList<>());
        dto.setHeroHighlights(new ArrayList<>());
        dto.setCaseKpis(new ArrayList<>());
        return dto;
    }

    private HomeContentPayload normalizeLegacyHomeContent(HomeContentPayload dto) {
        dto.setHeroTitle(normalizeLegacyText(dto.getHeroTitle()));
        dto.setBrandSlogan(normalizeLegacyText(dto.getBrandSlogan()));
        dto.setHeroDescription(normalizeLegacyText(dto.getHeroDescription()));
        dto.setCoreFeaturesTitle(normalizeLegacyText(dto.getCoreFeaturesTitle()));
        dto.setCoreFeaturesIntro(normalizeLegacyText(dto.getCoreFeaturesIntro()));
        dto.setArticlesSectionTitle(normalizeLegacyText(dto.getArticlesSectionTitle()));
        dto.setArticlesSectionSubtitle(normalizeLegacyText(dto.getArticlesSectionSubtitle()));

        if (dto.getHeroMetrics() != null && !dto.getHeroMetrics().isEmpty()) {
            List<HomeContentPayload.MetricItem> normalizedMetrics = new ArrayList<>();
            for (HomeContentPayload.MetricItem metric : dto.getHeroMetrics()) {
                HomeContentPayload.MetricItem normalized = new HomeContentPayload.MetricItem();
                normalized.setValue(metric.getValue());
                normalized.setLabel(normalizeLegacyText(metric.getLabel()));
                normalized.setDetail(normalizeLegacyText(metric.getDetail()));
                normalizedMetrics.add(normalized);
            }
            dto.setHeroMetrics(normalizedMetrics);
        }

        return dto;
    }

    private String normalizeLegacyText(String value) {
        if (value == null) {
            return null;
        }
        return LEGACY_TEXT_MAP.getOrDefault(value, value);
    }

    private ResponseStatusException cmsUnavailable(DataAccessException ex) {
        log.warn("CMS schema is unavailable. Root cause: {}", rootMessage(ex));
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "CMS 数据表尚未初始化");
    }

    private String writeJson(Object value) throws JsonProcessingException {
        return objectMapper.writeValueAsString(value == null ? List.of() : value);
    }

    private String rootMessage(Throwable throwable) {
        Throwable root = throwable;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        return root.getMessage();
    }
}
