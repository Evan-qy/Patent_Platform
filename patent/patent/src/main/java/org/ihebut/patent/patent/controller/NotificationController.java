package org.ihebut.patent.patent.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.ihebut.patent.patent.dto.ApiResponse;
import org.ihebut.patent.patent.dto.NotificationCreateRequest;
import org.ihebut.patent.patent.entity.UserNotification;
import org.ihebut.patent.patent.mapper.UserNotificationMapper;
import org.ihebut.patent.patent.security.CurrentUser;
import org.ihebut.patent.patent.service.AuditLogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final CurrentUser currentUser;
    private final UserNotificationMapper userNotificationMapper;
    private final AuditLogService auditLogService;

    public NotificationController(CurrentUser currentUser, UserNotificationMapper userNotificationMapper, AuditLogService auditLogService) {
        this.currentUser = currentUser;
        this.userNotificationMapper = userNotificationMapper;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public ApiResponse<List<UserNotification>> list(@RequestParam(required = false) Boolean read, HttpServletRequest httpRequest) {
        long userId = currentUser.requireUserId();
        List<UserNotification> result = read == null
                ? userNotificationMapper.findByUserId(userId)
                : userNotificationMapper.findByUserIdAndReadFlag(userId, read);
        auditLogService.logUserAction(httpRequest, userId, "READ", "查看通知列表", "notification", null, "read=" + read, true);
        return ApiResponse.ok(result);
    }

    @PostMapping
    public ApiResponse<UserNotification> create(@RequestBody NotificationCreateRequest request, HttpServletRequest httpRequest) {
        currentUser.requireAdmin();
        if (request == null || request.getUserId() == null) {
            throw new ResponseStatusException(BAD_REQUEST, "userId不能为空");
        }
        if (request.getType() == null || request.getType().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "type不能为空");
        }
        UserNotification notification = new UserNotification();
        notification.setUserId(request.getUserId());
        notification.setType(request.getType().trim());
        notification.setTitle(request.getTitle());
        notification.setContent(request.getContent());
        notification.setRelatedRequirementId(request.getRelatedRequirementId());
        UserNotification saved = userNotificationMapper.save(notification);
        auditLogService.logAdminAction(httpRequest, currentUser.requireAdmin().username(), "CREATE", "创建通知", "notification", String.valueOf(saved.getId()), saved.getTitle(), true);
        return ApiResponse.ok(saved);
    }

    @PutMapping("/{id}/read")
    public ApiResponse<UserNotification> markRead(@PathVariable Long id, HttpServletRequest httpRequest) {
        long userId = currentUser.requireUserId();
        UserNotification notification = userNotificationMapper.findById(id).orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "通知不存在"));
        if (!notification.getUserId().equals(userId)) {
            throw new ResponseStatusException(FORBIDDEN, "无权限");
        }
        notification.setReadFlag(true);
        UserNotification saved = userNotificationMapper.save(notification);
        auditLogService.logUserAction(httpRequest, userId, "UPDATE", "标记通知已读", "notification", String.valueOf(id), saved.getTitle(), true);
        return ApiResponse.ok(saved);
    }
}
