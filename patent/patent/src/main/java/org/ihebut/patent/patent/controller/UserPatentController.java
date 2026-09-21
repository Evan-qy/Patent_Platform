package org.ihebut.patent.patent.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.ihebut.patent.patent.dto.ApiResponse;
import org.ihebut.patent.patent.dto.UserPatentUpsertRequest;
import org.ihebut.patent.patent.entity.UserPatent;
import org.ihebut.patent.patent.mapper.UserPatentMapper;
import org.ihebut.patent.patent.security.CurrentUser;
import org.ihebut.patent.patent.service.AuditLogService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;

@RestController
@RequestMapping("/api/user-patents")
public class UserPatentController {
    private final CurrentUser currentUser;
    private final UserPatentMapper userPatentMapper;
    private final AuditLogService auditLogService;

    public UserPatentController(CurrentUser currentUser, UserPatentMapper userPatentMapper, AuditLogService auditLogService) {
        this.currentUser = currentUser;
        this.userPatentMapper = userPatentMapper;
        this.auditLogService = auditLogService;
    }

    @PostMapping
    public ApiResponse<UserPatent> create(@RequestBody UserPatentUpsertRequest request, HttpServletRequest httpRequest) {
        long userId = currentUser.requireUserId();
        if (request == null || request.getCategory() == null || request.getCategory().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "category不能为空");
        }
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "title不能为空");
        }
        UserPatent patent = new UserPatent();
        patent.setOwnerUserId(userId);
        apply(patent, request);
        UserPatent saved = userPatentMapper.save(patent);
        auditLogService.logUserAction(httpRequest, userId, "CREATE", "新增用户专利", "user_patent", String.valueOf(saved.getId()), saved.getTitle(), true);
        return ApiResponse.ok(saved);
    }

    @GetMapping
    public ApiResponse<List<UserPatent>> list(
            @RequestParam(required = false) String owner,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String visibility,
            HttpServletRequest httpRequest
    ) {
        long userId = currentUser.requireUserId();
        String q = query == null ? "" : query.trim();
        String cat = category == null ? null : category.trim();
        String vis = visibility == null ? null : visibility.trim();

        List<UserPatent> base;
        if ("me".equalsIgnoreCase(owner)) {
            if (cat != null && !cat.isBlank()) {
                base = userPatentMapper.findByCategoryAndOwnerUserId(cat, userId);
            } else {
                base = userPatentMapper.findByOwnerUserId(userId);
            }
        } else {
            if (cat != null && !cat.isBlank()) {
                base = userPatentMapper.findByCategoryAndVisibility(cat, "PUBLIC");
            } else {
                base = userPatentMapper.findByVisibility("PUBLIC");
            }
        }

        if (vis != null && !vis.isBlank()) {
            List<UserPatent> filtered = new ArrayList<>();
            for (UserPatent patent : base) {
                if (vis.equalsIgnoreCase(patent.getVisibility())) {
                    filtered.add(patent);
                }
            }
            base = filtered;
        }

        if (!q.isEmpty()) {
            List<UserPatent> filtered = new ArrayList<>();
            for (UserPatent patent : base) {
                if (contains(patent.getTitle(), q)
                        || contains(patent.getAbstractText(), q)
                        || contains(patent.getApplicant(), q)
                        || contains(patent.getInventor(), q)
                        || contains(patent.getPublicNum(), q)) {
                    filtered.add(patent);
                }
            }
            base = filtered;
        }

        auditLogService.logUserAction(httpRequest, userId, "QUERY", "查询用户专利列表", "user_patent", null, "owner=" + owner + ", category=" + category + ", query=" + query, true);
        return ApiResponse.ok(base);
    }

    @GetMapping("/{id}")
    public ApiResponse<UserPatent> get(@PathVariable Long id, HttpServletRequest httpRequest) {
        long userId = currentUser.requireUserId();
        UserPatent patent = userPatentMapper.findById(id).orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "记录不存在"));
        if (!isVisibleToUser(patent, userId)) {
            throw new ResponseStatusException(FORBIDDEN, "无权限");
        }
        auditLogService.logUserAction(httpRequest, userId, "READ", "查看用户专利详情", "user_patent", String.valueOf(id), patent.getTitle(), true);
        return ApiResponse.ok(patent);
    }

    @PutMapping("/{id}")
    public ApiResponse<UserPatent> update(@PathVariable Long id, @RequestBody UserPatentUpsertRequest request, HttpServletRequest httpRequest) {
        long userId = currentUser.requireUserId();
        UserPatent patent = userPatentMapper.findById(id).orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "记录不存在"));
        if (!patent.getOwnerUserId().equals(userId)) {
            throw new ResponseStatusException(FORBIDDEN, "无权限");
        }
        apply(patent, request);
        UserPatent saved = userPatentMapper.save(patent);
        auditLogService.logUserAction(httpRequest, userId, "UPDATE", "修改用户专利", "user_patent", String.valueOf(id), saved.getTitle(), true);
        return ApiResponse.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, HttpServletRequest httpRequest) {
        long userId = currentUser.requireUserId();
        UserPatent patent = userPatentMapper.findById(id).orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "记录不存在"));
        if (!patent.getOwnerUserId().equals(userId)) {
            throw new ResponseStatusException(FORBIDDEN, "无权限");
        }
        userPatentMapper.deleteById(id);
        auditLogService.logUserAction(httpRequest, userId, "DELETE", "删除用户专利", "user_patent", String.valueOf(id), patent.getTitle(), true);
        return ApiResponse.ok();
    }

    private void apply(UserPatent patent, UserPatentUpsertRequest request) {
        if (request == null) return;
        if (request.getCategory() != null && !request.getCategory().isBlank()) patent.setCategory(request.getCategory().trim());
        if (request.getPublicNum() != null) patent.setPublicNum(request.getPublicNum());
        if (request.getTitle() != null) patent.setTitle(request.getTitle());
        if (request.getAbstractText() != null) patent.setAbstractText(request.getAbstractText());
        if (request.getIpc() != null) patent.setIpc(request.getIpc());
        if (request.getCpc() != null) patent.setCpc(request.getCpc());
        if (request.getNec() != null) patent.setNec(request.getNec());
        if (request.getApplicant() != null) patent.setApplicant(request.getApplicant());
        if (request.getInventor() != null) patent.setInventor(request.getInventor());
        if (request.getPatentDetails() != null) patent.setPatentDetails(request.getPatentDetails());
        if (request.getVisibility() != null && !request.getVisibility().isBlank()) patent.setVisibility(request.getVisibility().trim().toUpperCase());
        if (request.getCooperationCondition() != null) patent.setCooperationCondition(request.getCooperationCondition());
    }

    private boolean isVisibleToUser(UserPatent patent, long userId) {
        return "PUBLIC".equalsIgnoreCase(patent.getVisibility()) || patent.getOwnerUserId().equals(userId);
    }

    private boolean contains(String source, String query) {
        return source != null && source.contains(query);
    }
}
