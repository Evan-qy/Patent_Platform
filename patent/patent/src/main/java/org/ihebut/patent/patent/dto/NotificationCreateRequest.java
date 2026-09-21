package org.ihebut.patent.patent.dto;

import lombok.Data;

@Data
public class NotificationCreateRequest {
    private Long userId;
    private String type;
    private String title;
    private String content;
    private Long relatedRequirementId;
}

