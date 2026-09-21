package org.ihebut.patent.patent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PatentMatchResult {
    private String patentSource;
    private String category;
    private String publicNum;
    private Long userPatentId;
    private String title;
    private String applicant;
    private String inventor;
}

