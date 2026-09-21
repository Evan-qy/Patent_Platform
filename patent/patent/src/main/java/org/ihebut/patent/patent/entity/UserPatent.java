package org.ihebut.patent.patent.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_patent")
@Data
public class UserPatent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_user_id", nullable = false)
    private Long ownerUserId;

    @Column(nullable = false, length = 32)
    private String category;

    @Column(name = "public_num", length = 32)
    private String publicNum;

    @Column(name = "legal_status", columnDefinition = "text")
    private String legalStatus;

    @Column(name = "latest_legal_status", columnDefinition = "text")
    private String latestLegalStatus;

    @Column(name = "status", columnDefinition = "text")
    private String status;

    @Column(name = "title", columnDefinition = "text")
    private String title;

    @Column(name = "type", columnDefinition = "text")
    private String type;

    @Column(name = "abstract", columnDefinition = "text")
    private String abstractText;

    @Column(name = "appli_num", columnDefinition = "text")
    private String appliNum;

    @Column(name = "appli_date", columnDefinition = "text")
    private String appliDate;

    @Column(name = "public_date", columnDefinition = "text")
    private String publicDate;

    @Column(name = "applicant", columnDefinition = "text")
    private String applicant;

    @Column(name = "applicant_address", columnDefinition = "text")
    private String applicantAddress;

    @Column(name = "patentee", columnDefinition = "text")
    private String patentee;

    @Column(name = "patentee_address", columnDefinition = "text")
    private String patenteeAddress;

    @Column(name = "inventor", columnDefinition = "text")
    private String inventor;

    @Column(name = "agent", columnDefinition = "text")
    private String agent;

    @Column(name = "IPC", columnDefinition = "text")
    private String ipc;

    @Column(name = "CPC", columnDefinition = "text")
    private String cpc;

    @Column(name = "NEC", columnDefinition = "text")
    private String nec;

    @Column(name = "patent_details", columnDefinition = "text")
    private String patentDetails;

    @Column(nullable = false, length = 16)
    private String visibility = "PUBLIC";

    @Column(name = "cooperation_condition", columnDefinition = "text")
    private String cooperationCondition;

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
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
