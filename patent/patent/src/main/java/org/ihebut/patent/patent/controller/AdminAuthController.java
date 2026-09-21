package org.ihebut.patent.patent.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.ihebut.patent.patent.dto.ApiResponse;
import org.ihebut.patent.patent.dto.LoginRequest;
import org.ihebut.patent.patent.security.AppAuthPrincipal;
import org.ihebut.patent.patent.security.CurrentUser;
import org.ihebut.patent.patent.security.JwtService;
import org.ihebut.patent.patent.service.AuditLogService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {
    private final CurrentUser currentUser;
    private final JwtService jwtService;
    private final AuditLogService auditLogService;
    private final String adminUsername;
    private final String adminPassword;

    public AdminAuthController(
            CurrentUser currentUser,
            JwtService jwtService,
            AuditLogService auditLogService,
            @Value("${admin.auth.username:admin}") String adminUsername,
            @Value("${admin.auth.password:}") String adminPassword
    ) {
        this.currentUser = currentUser;
        this.jwtService = jwtService;
        this.auditLogService = auditLogService;
        this.adminUsername = adminUsername == null ? "admin" : adminUsername.trim();
        this.adminPassword = adminPassword == null ? "" : adminPassword;
    }

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        if (adminPassword.isBlank()) {
            throw new ResponseStatusException(SERVICE_UNAVAILABLE, "后台账号未配置，请设置环境变量 ADMIN_AUTH_PASSWORD");
        }
        if (request == null || request.getUsername() == null || request.getUsername().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "用户名不能为空");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "密码不能为空");
        }
        if (!adminUsername.equals(request.getUsername().trim()) || !adminPassword.equals(request.getPassword())) {
            auditLogService.logAdminAction(httpRequest, request.getUsername().trim(), "LOGIN", "后台登录", "admin_account", "0", "后台账号或密码错误", false);
            throw new ResponseStatusException(BAD_REQUEST, "后台账号或密码错误");
        }

        auditLogService.logAdminAction(httpRequest, adminUsername, "LOGIN", "后台登录", "admin_account", "0", "后台登录成功", true);

        return ApiResponse.ok(Map.of(
                "token", jwtService.createAdminToken(adminUsername),
                "admin", Map.of(
                        "id", 0L,
                        "username", adminUsername,
                        "displayName", "系统管理员",
                        "status", "ACTIVE"
                )
        ));
    }

    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> me() {
        AppAuthPrincipal principal = currentUser.requireAdmin();
        return ApiResponse.ok(Map.of(
                "id", 0L,
                "username", principal.username(),
                "displayName", "系统管理员",
                "status", "ACTIVE"
        ));
    }
}
