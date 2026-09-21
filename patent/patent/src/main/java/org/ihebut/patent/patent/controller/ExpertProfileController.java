package org.ihebut.patent.patent.controller;

import org.ihebut.patent.patent.dto.ApiResponse;
import org.ihebut.patent.patent.dto.ExpertAuditRequest;
import org.ihebut.patent.patent.dto.ExpertCreateRequest;
import org.ihebut.patent.patent.dto.ExpertProfileUpdateRequest;
import org.ihebut.patent.patent.entity.ExpertProfile;
import org.ihebut.patent.patent.entity.UserAccount;
import org.ihebut.patent.patent.mapper.ExpertProfileMapper;
import org.ihebut.patent.patent.mapper.UserAccountMapper;
import org.ihebut.patent.patent.security.CurrentUser;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
@RequestMapping("/api")
public class ExpertProfileController {
    private final CurrentUser currentUser;
    private final ExpertProfileMapper expertProfileMapper;
    private final UserAccountMapper userAccountMapper;

    public ExpertProfileController(CurrentUser currentUser, ExpertProfileMapper expertProfileMapper, UserAccountMapper userAccountMapper) {
        this.currentUser = currentUser;
        this.expertProfileMapper = expertProfileMapper;
        this.userAccountMapper = userAccountMapper;
    }

    @PostMapping("/experts")
    @Transactional
    public ApiResponse<ExpertProfile> create(@RequestBody ExpertCreateRequest request) {
        long auditorId = currentUser.requireUserId();
        if (request == null || request.getUserId() == null) {
            throw new ResponseStatusException(BAD_REQUEST, "userId不能为空");
        }
        UserAccount user = userAccountMapper.findById(request.getUserId()).orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "用户不存在"));
        ExpertProfile profile = expertProfileMapper.findById(request.getUserId()).orElse(null);
        if (profile == null) {
            profile = new ExpertProfile();
            profile.setUser(user);
        }
        if (request.getField() != null) profile.setField(request.getField());
        if (request.getExpertise() != null) profile.setExpertise(request.getExpertise());
        if (request.getAchievements() != null) profile.setAchievements(request.getAchievements());
        if (request.getContactInfo() != null) profile.setContactInfo(request.getContactInfo());

        String status = (request.getCertStatus() == null || request.getCertStatus().isBlank()) ? "APPROVED" : request.getCertStatus().trim().toUpperCase();
        if (!status.equals("APPROVED") && !status.equals("REJECTED") && !status.equals("PENDING")) {
            throw new ResponseStatusException(BAD_REQUEST, "certStatus不合法");
        }
        profile.setCertStatus(status);
        profile.setCertSubmitAt(LocalDateTime.now());
        if (!"PENDING".equals(status)) {
            profile.setCertAuditAt(LocalDateTime.now());
            profile.setCertAuditBy(auditorId);
        } else {
            profile.setCertAuditAt(null);
            profile.setCertAuditBy(null);
        }
        return ApiResponse.ok(expertProfileMapper.save(profile));
    }

    @GetMapping("/experts")
    public ApiResponse<List<ExpertProfile>> list(@RequestParam(required = false) String query) {
        String q = query == null ? "" : query.trim();
        List<ExpertProfile> list;
        if (q.isEmpty()) {
            list = expertProfileMapper.findByCertStatus("APPROVED");
        } else {
            list = expertProfileMapper.searchExperts("APPROVED", q);
        }
        return ApiResponse.ok(list);
    }

    @PutMapping("/experts/me")
    @Transactional
    public ApiResponse<ExpertProfile> upsertMe(@RequestBody ExpertProfileUpdateRequest request) {
        long userId = currentUser.requireUserId();
        UserAccount user = userAccountMapper.findById(userId).orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "用户不存在"));
        ExpertProfile profile = expertProfileMapper.findById(userId).orElse(null);
        if (profile == null) {
            profile = new ExpertProfile();
            profile.setUser(user);
        }
        if (request != null) {
            if (request.getField() != null) profile.setField(request.getField());
            if (request.getExpertise() != null) profile.setExpertise(request.getExpertise());
            if (request.getAchievements() != null) profile.setAchievements(request.getAchievements());
            if (request.getContactInfo() != null) profile.setContactInfo(request.getContactInfo());
        }
        profile.setCertStatus("PENDING");
        profile.setCertSubmitAt(LocalDateTime.now());
        profile.setCertAuditAt(null);
        profile.setCertAuditBy(null);
        return ApiResponse.ok(expertProfileMapper.save(profile));
    }

    @PostMapping("/admin/experts/{userId}/audit")
    @Transactional
    public ApiResponse<ExpertProfile> audit(@PathVariable Long userId, @RequestBody ExpertAuditRequest request) {
        long auditorId = currentUser.requireUserId();
        ExpertProfile profile = expertProfileMapper.findById(userId).orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "专家资料不存在"));
        if (request == null || request.getCertStatus() == null || request.getCertStatus().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "certStatus不能为空");
        }
        String status = request.getCertStatus().trim().toUpperCase();
        if (!status.equals("APPROVED") && !status.equals("REJECTED") && !status.equals("PENDING")) {
            throw new ResponseStatusException(BAD_REQUEST, "certStatus不合法");
        }
        profile.setCertStatus(status);
        profile.setCertAuditAt(LocalDateTime.now());
        profile.setCertAuditBy(auditorId);
        return ApiResponse.ok(expertProfileMapper.save(profile));
    }
}
