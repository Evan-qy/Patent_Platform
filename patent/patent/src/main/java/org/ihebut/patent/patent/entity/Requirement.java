package org.ihebut.patent.patent.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.math.BigDecimal;

/**
 * 需求实体。
 *
 * <p>用于描述企业/机构的技术需求，以便与专利/专家进行匹配。</p>
 */
@Entity
@Table(name = "requirement")
@Data
public class Requirement {
    /**
     * 主键ID。
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 需求标题。
     */
    private String title;
    
    /**
     * 需求详细描述。
     */
    @Column(columnDefinition = "text")
    private String description;
    
    /**
     * 关键词（可用于匹配）。
     */
    @Column(length = 1024)
    private String keywords;

    @Column(name = "tech_direction", length = 255)
    private String techDirection;

    @Column(name = "cooperation_mode", length = 255)
    private String cooperationMode;

    @Column(name = "contact_info", length = 255)
    private String contactInfo;

    @Column(name = "budget", precision = 12, scale = 2)
    private BigDecimal budget;

    @Column(name = "deadline", length = 64)
    private String deadline;

    @Column(name = "requester_user_id", nullable = false)
    private Long requesterUserId;

    @Column(name = "requester_org_id")
    private Long requesterOrgId;
    
    /**
     * 状态（如：PENDING / MATCHED / CLOSED）。
     */
    @Column(nullable = false, length = 32)
    private String status = "PENDING";
    
    /**
     * 创建时间。
     */
    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    /**
     * 持久化前填充默认字段。
     */
    @PrePersist
    public void prePersist() {
        this.createdDate = LocalDateTime.now();
        this.updatedAt = this.createdDate;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
