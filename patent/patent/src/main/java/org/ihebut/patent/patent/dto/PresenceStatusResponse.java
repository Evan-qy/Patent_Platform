package org.ihebut.patent.patent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class PresenceStatusResponse {
    private long onlineUsers;
    private long authenticatedUsers;
    private long guestUsers;
    private long heartbeatWindowSeconds;
    private List<PresenceUserSummaryResponse> activeUsers;
}
