package org.ihebut.patent.patent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PresenceUserSummaryResponse {
    private String displayName;
    private String type;
    private String page;
    private long lastSeenAt;
}
