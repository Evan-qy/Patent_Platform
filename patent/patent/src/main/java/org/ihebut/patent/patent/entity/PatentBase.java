package org.ihebut.patent.patent.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;

/**
 * 专利基础字段映射。
 *
 * <p>用于映射风能/光伏/生物质/氢能/锂电等专利表的公共结构。</p>
 */
@MappedSuperclass
@Data
public abstract class PatentBase {
    /**
     * 公开号（主键）。
     */
    @Id
    @Column(name = "public_num", length = 32, nullable = false)
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
}

