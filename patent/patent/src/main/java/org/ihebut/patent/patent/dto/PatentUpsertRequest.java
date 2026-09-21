package org.ihebut.patent.patent.dto;

import lombok.Data;

/**
 * 专利写入请求。
 *
 * <p>字段与专利表结构保持一致。</p>
 */
@Data
public class PatentUpsertRequest {
    private String publicNum;
    private String legalStatus;
    private String latestLegalStatus;
    private String status;
    private String title;
    private String type;
    private String abstractText;
    private String appliNum;
    private String appliDate;
    private String publicDate;
    private String applicant;
    private String applicantAddress;
    private String patentee;
    private String patenteeAddress;
    private String inventor;
    private String agent;
    private String ipc;
    private String cpc;
    private String nec;
    private String patentDetails;
}

