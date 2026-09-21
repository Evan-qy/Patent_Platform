package org.ihebut.patent.patent.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "patent_valuation_report")
@Data
public class PatentValuationReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "patent_source", nullable = false, length = 16)
    private String patentSource = "EXTERNAL";

    @Column(name = "dataset_id")
    private Long datasetId;

    @Column(name = "record_id", length = 128)
    private String recordId;

    @Column(name = "patent_category", length = 32)
    private String patentCategory;

    @Column(name = "patent_public_num", length = 32)
    private String patentPublicNum;

    @Column(name = "user_patent_id")
    private Long userPatentId;

    @Column(name = "tech_value_score", precision = 6, scale = 3)
    private BigDecimal techValueScore;

    @Column(name = "market_value_score", precision = 6, scale = 3)
    private BigDecimal marketValueScore;

    @Column(name = "transformation_potential_score", precision = 6, scale = 3)
    private BigDecimal transformationPotentialScore;

    @Column(name = "overall_score", precision = 6, scale = 3)
    private BigDecimal overallScore;

    @Column(name = "predicted_value", precision = 18, scale = 2)
    private BigDecimal predictedValue;

    @Column(name = "report_json", columnDefinition = "text")
    private String reportJson;

    @Column(name = "model_version", length = 64)
    private String modelVersion;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
