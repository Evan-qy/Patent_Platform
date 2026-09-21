package org.ihebut.patent.patent.dto;

import lombok.Data;

@Data
public class OrganizationCreateRequest {
    private String name;
    private String type;
    private String creditCode;
    private String address;
    private String contactName;
    private String contactPhone;
    private String contactEmail;
}

