package org.ihebut.patent.patent.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.ihebut.patent.patent.dto.ApiResponse;
import org.ihebut.patent.patent.entity.TransformationResult;
import org.ihebut.patent.patent.security.CurrentUser;
import org.ihebut.patent.patent.service.AuditLogService;
import org.ihebut.patent.patent.service.TransformationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/transformations")
public class TransformationController {
    private final TransformationService transformationService;
    private final CurrentUser currentUser;
    private final AuditLogService auditLogService;

    public TransformationController(
            TransformationService transformationService,
            CurrentUser currentUser,
            AuditLogService auditLogService
    ) {
        this.transformationService = transformationService;
        this.currentUser = currentUser;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public ApiResponse<List<TransformationResult>> getAllResults(HttpServletRequest httpRequest) {
        List<TransformationResult> result = transformationService.getAllResults();
        try {
            long userId = currentUser.requireUserId();
            auditLogService.logUserAction(httpRequest, userId, "READ", "查看成果转化列表", "transformation_result", null, "查看成果转化列表", true);
        } catch (Exception ignored) {
            auditLogService.logAnonymousAction(httpRequest, null, "READ", "查看成果转化列表", "查看成果转化列表", true);
        }
        return ApiResponse.ok(result);
    }

    @PostMapping
    public ApiResponse<TransformationResult> createResult(@RequestBody TransformationResult result, HttpServletRequest httpRequest) {
        TransformationResult saved = transformationService.saveResult(result);
        long userId = currentUser.requireUserId();
        auditLogService.logUserAction(httpRequest, userId, "CREATE", "新增成果转化", "transformation_result", String.valueOf(saved.getId()), saved.getPatentPublicNum(), true);
        return ApiResponse.ok(saved);
    }
}
