package org.ihebut.patent.patent.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "home_content")
@Data
public class HomeContent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hero_title", length = 255)
    private String heroTitle;

    @Column(name = "brand_slogan", length = 255)
    private String brandSlogan;

    @Column(name = "hero_description", columnDefinition = "TEXT")
    private String heroDescription;

    @Column(name = "core_features_title", length = 255)
    private String coreFeaturesTitle;

    @Column(name = "core_features_intro", columnDefinition = "TEXT")
    private String coreFeaturesIntro;

    @Column(name = "hero_metrics_json", columnDefinition = "TEXT")
    private String heroMetricsJson;

    @Column(name = "features_json", columnDefinition = "TEXT")
    private String featuresJson;

    @Column(name = "advantages_json", columnDefinition = "TEXT")
    private String advantagesJson;

    @Column(name = "mobile_operation_tips_json", columnDefinition = "TEXT")
    private String mobileOperationTipsJson;

    @Column(name = "mobile_service_promises_json", columnDefinition = "TEXT")
    private String mobileServicePromisesJson;

    @Column(name = "hero_highlights_json", columnDefinition = "TEXT")
    private String heroHighlightsJson;

    @Column(name = "case_kpis_json", columnDefinition = "TEXT")
    private String caseKpisJson;

    @Column(name = "articles_section_title", length = 255)
    private String articlesSectionTitle;

    @Column(name = "articles_section_subtitle", length = 255)
    private String articlesSectionSubtitle;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
