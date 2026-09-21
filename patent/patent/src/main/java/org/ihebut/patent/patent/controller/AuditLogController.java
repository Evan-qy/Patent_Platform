package org.ihebut.patent.patent.controller;

import org.ihebut.patent.patent.dto.ApiResponse;
import org.ihebut.patent.patent.entity.AuditLog;
import org.ihebut.patent.patent.mapper.AuditLogMapper;
import org.ihebut.patent.patent.security.CurrentUser;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
@RequestMapping({"/api/admin/audit-logs", "/api/audit-logs"})
public class AuditLogController {
    private final CurrentUser currentUser;
    private final AuditLogMapper auditLogMapper;

    public AuditLogController(CurrentUser currentUser, AuditLogMapper auditLogMapper) {
        this.currentUser = currentUser;
        this.auditLogMapper = auditLogMapper;
    }

    @GetMapping
    public ApiResponse<List<AuditLog>> list(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String requestMethod,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String operationResult,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to
    ) {
        currentUser.requireAdmin();

        LocalDateTime fromDt = parseTime(from);
        LocalDateTime toDt = parseTime(to);

        Specification<AuditLog> spec = Specification.where(null);
        if (userId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("userId"), userId));
        }
        if (username != null && !username.isBlank()) {
            String keyword = "%" + username.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("username")), keyword));
        }
        if (eventType != null && !eventType.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(cb.upper(root.get("eventType")), eventType.trim().toUpperCase()));
        }
        if (action != null && !action.isBlank()) {
            String keyword = "%" + action.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("action")), keyword));
        }
        if (requestMethod != null && !requestMethod.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(cb.upper(root.get("requestMethod")), requestMethod.trim().toUpperCase()));
        }
        if (resourceType != null && !resourceType.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(cb.lower(root.get("resourceType")), resourceType.trim().toLowerCase()));
        }
        if (operationResult != null && !operationResult.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(cb.upper(root.get("operationResult")), operationResult.trim().toUpperCase()));
        }
        if (fromDt != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), fromDt));
        }
        if (toDt != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), toDt));
        }

        return ApiResponse.ok(auditLogMapper.findAll(spec, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    private LocalDateTime parseTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value.trim());
        } catch (DateTimeParseException e) {
            throw new ResponseStatusException(BAD_REQUEST, "时间格式不合法");
        }
    }
}
