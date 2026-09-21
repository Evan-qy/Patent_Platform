package org.ihebut.patent.patent.service;

import org.ihebut.patent.patent.dto.PresenceStatusResponse;
import org.ihebut.patent.patent.dto.PresenceUserSummaryResponse;
import org.ihebut.patent.patent.security.AppAuthPrincipal;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PresenceService {
    private static final Duration ACTIVE_WINDOW = Duration.ofMinutes(2);
    private static final int MAX_VISIBLE_USERS = 8;

    private final Map<String, PresenceEntry> activeClients = new ConcurrentHashMap<>();

    public PresenceStatusResponse heartbeat(String clientId, String page, AppAuthPrincipal principal) {
        cleanupExpired();
        Instant now = Instant.now();
        String normalizedPage = normalizePage(page);
        String resolvedClientId = resolveClientId(clientId, principal);
        activeClients.put(resolvedClientId, buildEntry(principal, normalizedPage, now));
        return status();
    }

    public PresenceStatusResponse status() {
        cleanupExpired();
        List<PresenceEntry> activeEntries = new ArrayList<>(activeClients.values());
        activeEntries.sort(Comparator.comparing(PresenceEntry::lastSeenAt).reversed());

        long authenticatedUsers = activeEntries.stream().filter(entry -> !entry.guest()).count();
        long guestUsers = activeEntries.size() - authenticatedUsers;
        List<PresenceUserSummaryResponse> activeUsers = activeEntries.stream()
                .limit(MAX_VISIBLE_USERS)
                .map(entry -> new PresenceUserSummaryResponse(
                        entry.displayName(),
                        entry.guest() ? "guest" : "user",
                        entry.page(),
                        entry.lastSeenAt().toEpochMilli()
                ))
                .toList();

        return new PresenceStatusResponse(
                activeEntries.size(),
                authenticatedUsers,
                guestUsers,
                ACTIVE_WINDOW.toSeconds(),
                activeUsers
        );
    }

    private PresenceEntry buildEntry(AppAuthPrincipal principal, String page, Instant now) {
        Instant expiresAt = now.plus(ACTIVE_WINDOW);
        if (principal != null && principal.userId() != null) {
            String displayName = principal.username() == null || principal.username().isBlank()
                    ? "用户#" + principal.userId()
                    : principal.username();
            return new PresenceEntry(displayName, false, page, now, expiresAt);
        }
        return new PresenceEntry("访客", true, page, now, expiresAt);
    }

    private String resolveClientId(String clientId, AppAuthPrincipal principal) {
        if (principal != null && principal.userId() != null) {
            return "user:" + principal.userId();
        }
        if (clientId == null || clientId.isBlank()) {
            return "guest:" + UUID.randomUUID();
        }
        return "guest:" + clientId.trim();
    }

    private String normalizePage(String page) {
        if (page == null || page.isBlank()) {
            return "/";
        }
        String trimmed = page.trim();
        return trimmed.length() > 60 ? trimmed.substring(0, 60) : trimmed;
    }

    private void cleanupExpired() {
        Instant now = Instant.now();
        Iterator<Map.Entry<String, PresenceEntry>> iterator = activeClients.entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getValue().expiresAt().isBefore(now)) {
                iterator.remove();
            }
        }
    }

    private record PresenceEntry(
            String displayName,
            boolean guest,
            String page,
            Instant lastSeenAt,
            Instant expiresAt
    ) {
    }
}
