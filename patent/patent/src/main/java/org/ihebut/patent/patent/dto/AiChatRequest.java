package org.ihebut.patent.patent.dto;

import lombok.Data;

@Data
public class AiChatRequest {
    private String question;
    private String model;
    private Double temperature;
    private Integer maxTokens;
}
