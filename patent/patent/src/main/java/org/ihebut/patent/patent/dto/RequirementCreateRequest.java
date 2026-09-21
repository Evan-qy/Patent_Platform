package org.ihebut.patent.patent.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class RequirementCreateRequest {
    private String title;
    private String description;
    private String keywords;
    private String techDirection;
    private String cooperationMode;
    private String contactInfo;
    private BigDecimal budget;
    private String deadline;
    private Long requesterOrgId;
}
