package org.ihebut.patent.patent.dto;

import lombok.Data;

@Data
public class OrganizationMemberRequest {
    private String relationType;
    private String positionTitle;
    private Boolean isPrimary;
}

