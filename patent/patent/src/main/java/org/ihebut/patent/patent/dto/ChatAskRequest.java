package org.ihebut.patent.patent.dto;

import lombok.Data;

@Data
public class ChatAskRequest {
    private Long sessionId;
    private String question;
    private String model;
    private Double temperature;
    private Integer maxTokens;
    private Integer historyLimit;
}

