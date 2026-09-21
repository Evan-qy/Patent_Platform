package org.ihebut.patent.patent.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "transformation_result")
@Data
public class TransformationResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "patent_source", nullable = false, length = 16)
    private String patentSource = "EXTERNAL";

    @Column(name = "patent_category", length = 32)
    private String patentCategory;

    @Column(name = "patent_public_num", length = 32)
    private String patentPublicNum;

    @Column(name = "user_patent_id")
    private Long userPatentId;

    @Column(name = "expert_user_id")
    private Long expertUserId;

    @Column(name = "requirement_id")
    private Long requirementId;

    @Column(name = "partner_org_id")
    private Long partnerOrgId;

    @Column(length = 1000)
    private String description;

    @Column(name = "transformation_date")
    private LocalDate transformationDate;

    @Column(length = 64)
    private String status;

    @Column(name = "benefit_amount", precision = 18, scale = 2)
    private BigDecimal benefitAmount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
