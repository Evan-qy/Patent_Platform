package org.ihebut.patent.patent.dto;

import lombok.Data;

@Data
public class UserPatentUpsertRequest {
    private String category;
    private String publicNum;
    private String title;
    private String abstractText;
    private String ipc;
    private String cpc;
    private String nec;
    private String applicant;
    private String inventor;
    private String patentDetails;
    private String visibility;
    private String cooperationCondition;
}

