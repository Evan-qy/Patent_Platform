package org.ihebut.patent.patent.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.ihebut.patent.patent.entity.AuditLog;
import org.ihebut.patent.patent.entity.UserAccount;
import org.ihebut.patent.patent.entity.UserProfile;
import org.ihebut.patent.patent.mapper.AuditLogMapper;
import org.ihebut.patent.patent.mapper.UserAccountMapper;
import org.ihebut.patent.patent.mapper.UserProfileMapper;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuditLogService {
    private final AuditLogMapper auditLogMapper;
    private final UserAccountMapper userAccountMapper;
    private final UserProfileMapper userProfileMapper;

    public void logUserAction(
            HttpServletRequest request,
            Long userId,
            String eventType,
            String action,
            String resourceType,
            String resourceId,
            String detail,
            boolean success
    ) {
        if (userId == null) {
            return;
        }
        Optional<UserAccount> userOpt = userAccountMapper.findById(userId);
        String username = userOpt.map(UserAccount::getUsername).orElse(null);
        String roleName = userOpt.map(UserAccount::getUserType).orElse("USER");
        String displayName = userProfileMapper.findById(userId)
                .map(UserProfile::getNickname)
                .filter(name -> name != null && !name.isBlank())
                .orElse(username);

        save(buildBaseLog(
                request,
                userId,
                username,
                displayName,
                roleName,
                eventType,
                action,
                resourceType,
                resourceId,
                detail,
                success
        ));
    }

    public void logAdminAction(
            HttpServletRequest request,
            String adminUsername,
            String eventType,
            String action,
            String resourceType,
            String resourceId,
            String detail,
            boolean success
    ) {
        save(buildBaseLog(
                request,
                null,
                adminUsername,
                "系统管理员",
                "ADMIN",
                eventType,
                action,
                resourceType,
                resourceId,
                detail,
                success
        ));
    }

    public void logAnonymousAction(
            HttpServletRequest request,
            String username,
            String eventType,
            String action,
            String detail,
            boolean success
    ) {
        save(buildBaseLog(
                request,
                null,
                username,
                username,
                "ANONYMOUS",
                eventType,
                action,
                null,
                null,
                detail,
                success
        ));
    }

    private AuditLog buildBaseLog(
            HttpServletRequest request,
            Long userId,
            String username,
            String displayName,
            String roleName,
            String eventType,
            String action,
            String resourceType,
            String resourceId,
            String detail,
            boolean success
    ) {
        AuditLog log = new AuditLog();
        log.setUserId(userId);
        log.setUsername(trim(username, 64));
        log.setDisplayName(trim(displayName, 128));
        log.setRoleName(trim(roleName, 64));
        log.setEventType(trim(eventType, 32));
        log.setAction(trim(action, 128));
        log.setResourceType(trim(resourceType, 64));
        log.setResourceId(trim(resourceId, 128));
        log.setDetail(detail);
        log.setRequestMethod(request == null ? null : trim(request.getMethod(), 16));
        log.setRequestPath(request == null ? null : trim(request.getRequestURI(), 255));
        log.setIpAddress(trim(resolveIp(request), 64));
        log.setMacAddress(trim(resolveHeader(request, "X-MAC-Address", "X-Client-Mac", "X-Device-Mac"), 64));
        log.setHostName(trim(resolveHostName(request), 128));
        log.setLocation(trim(resolveLocation(request), 128));
        log.setUserAgent(trim(request == null ? null : request.getHeader("User-Agent"), 512));
        log.setPlatformType(trim(resolvePlatform(request), 32));
        log.setOperationResult(success ? "SUCCESS" : "FAIL");
        return log;
    }

    private void save(AuditLog log) {
        try {
            auditLogMapper.save(log);
        } catch (Exception ignored) {
            // 审计日志失败不能影响主业务
        }
    }

    private String resolveIp(HttpServletRequest request) {
        String forwarded = resolveHeader(request, "X-Forwarded-For", "Proxy-Client-IP", "WL-Proxy-Client-IP", "X-Real-IP");
        if (forwarded != null && !forwarded.isBlank()) {
            int commaIndex = forwarded.indexOf(',');
            return commaIndex >= 0 ? forwarded.substring(0, commaIndex).trim() : forwarded.trim();
        }
        return request == null ? null : request.getRemoteAddr();
    }

    private String resolveHostName(HttpServletRequest request) {
        String headerHost = resolveHeader(request, "X-Client-Host", "X-Forwarded-Host", "Host");
        if (headerHost != null && !headerHost.isBlank()) {
            return headerHost;
        }
        return request == null ? null : request.getRemoteHost();
    }

    private String resolveLocation(HttpServletRequest request) {
        String direct = resolveHeader(request, "X-Geo-Location", "X-Location", "CF-IPCountry");
        if (direct != null && !direct.isBlank()) {
            return direct;
        }
        return "未知";
    }

    private String resolvePlatform(HttpServletRequest request) {
        String ua = request == null ? "" : String.valueOf(request.getHeader("User-Agent"));
        String lower = ua.toLowerCase(Locale.ROOT);
        if (lower.contains("android")) return "ANDROID";
        if (lower.contains("iphone") || lower.contains("ipad") || lower.contains("ios")) return "IOS";
        if (lower.contains("micromessenger")) return "WECHAT";
        if (lower.contains("postman")) return "API";
        return "WEB";
    }

    private String resolveHeader(HttpServletRequest request, String... names) {
        if (request == null || names == null) {
            return null;
        }
        for (String name : names) {
            String value = request.getHeader(name);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String trim(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength);
    }
}
