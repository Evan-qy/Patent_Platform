package org.ihebut.patent.patent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ChatAskResponse {
    private Long sessionId;
    private String answer;
    private String model;
    private String requestId;
}

