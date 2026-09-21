package org.ihebut.patent.patent.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "expert_profile")
@Data
public class ExpertProfile {
    @Id
    @Column(name = "user_id")
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private UserAccount user;

    @Column(length = 255)
    private String field;

    @Column(columnDefinition = "text")
    private String expertise;

    @Column(columnDefinition = "text")
    private String achievements;

    @Column(name = "contact_info", length = 255)
    private String contactInfo;

    @Column(name = "cert_status", nullable = false, length = 32)
    private String certStatus = "PENDING";

    @Column(name = "cert_submit_at")
    private LocalDateTime certSubmitAt;

    @Column(name = "cert_audit_at")
    private LocalDateTime certAuditAt;

    @Column(name = "cert_audit_by")
    private Long certAuditBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        this.updatedAt = now;
        if (this.certSubmitAt == null && "PENDING".equals(this.certStatus)) {
            this.certSubmitAt = now;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
