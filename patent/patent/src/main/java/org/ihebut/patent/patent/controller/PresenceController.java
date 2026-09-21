package org.ihebut.patent.patent.controller;

import org.ihebut.patent.patent.dto.ApiResponse;
import org.ihebut.patent.patent.dto.PresenceHeartbeatRequest;
import org.ihebut.patent.patent.dto.PresenceStatusResponse;
import org.ihebut.patent.patent.security.AppAuthPrincipal;
import org.ihebut.patent.patent.service.PresenceService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/presence")
public class PresenceController {
    private final PresenceService presenceService;

    public PresenceController(PresenceService presenceService) {
        this.presenceService = presenceService;
    }

    @GetMapping
    public ApiResponse<PresenceStatusResponse> status() {
        return ApiResponse.ok(presenceService.status());
    }

    @PostMapping("/heartbeat")
    public ApiResponse<PresenceStatusResponse> heartbeat(
            @RequestBody(required = false) PresenceHeartbeatRequest request,
            Authentication authentication
    ) {
        AppAuthPrincipal principal = authentication != null && authentication.getPrincipal() instanceof AppAuthPrincipal authPrincipal
                ? authPrincipal
                : null;
        return ApiResponse.ok(presenceService.heartbeat(
                request == null ? null : request.getClientId(),
                request == null ? null : request.getPage(),
                principal
        ));
    }
}
