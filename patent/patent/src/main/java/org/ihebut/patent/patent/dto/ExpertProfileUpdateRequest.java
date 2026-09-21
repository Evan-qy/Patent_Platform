package org.ihebut.patent.patent.dto;

import lombok.Data;

@Data
public class ExpertProfileUpdateRequest {
    private String field;
    private String expertise;
    private String achievements;
    private String contactInfo;
}

