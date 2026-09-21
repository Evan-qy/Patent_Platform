package org.ihebut.patent.patent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ExpertMatchResult {
    private Long userId;
    private String field;
    private String expertise;
    private String contactInfo;
}

