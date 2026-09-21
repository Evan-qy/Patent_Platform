package org.ihebut.patent.patent.dto;

import lombok.Data;

@Data
public class ExpertCreateRequest {
    private Long userId;
    private String field;
    private String expertise;
    private String achievements;
    private String contactInfo;
    private String certStatus;
}
