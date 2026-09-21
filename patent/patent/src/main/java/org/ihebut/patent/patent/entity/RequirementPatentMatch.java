package org.ihebut.patent.patent.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "requirement_patent_match")
@Data
public class RequirementPatentMatch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "requirement_id", nullable = false)
    private Long requirementId;

    @Column(name = "patent_source", nullable = false, length = 16)
    private String patentSource = "EXTERNAL";

    @Column(name = "patent_category", length = 32)
    private String patentCategory;

    @Column(name = "patent_public_num", length = 32)
    private String patentPublicNum;

    @Column(name = "user_patent_id")
    private Long userPatentId;

    @Column(name = "match_score", precision = 6, scale = 3)
    private BigDecimal matchScore;

    @Column(name = "match_reason", columnDefinition = "text")
    private String matchReason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
