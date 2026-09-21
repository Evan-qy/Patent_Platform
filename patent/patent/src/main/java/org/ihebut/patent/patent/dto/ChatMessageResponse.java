package org.ihebut.patent.patent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ChatMessageResponse {
    private Long id;
    private String role;
    private String content;
    private LocalDateTime createdAt;
}

