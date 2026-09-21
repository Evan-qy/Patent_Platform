package org.ihebut.patent.patent.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.ihebut.patent.patent.dto.ApiResponse;
import org.ihebut.patent.patent.dto.UserProfileUpdateRequest;
import org.ihebut.patent.patent.entity.ExpertProfile;
import org.ihebut.patent.patent.entity.Organization;
import org.ihebut.patent.patent.entity.UserAccount;
import org.ihebut.patent.patent.entity.UserOrganization;
import org.ihebut.patent.patent.entity.UserProfile;
import org.ihebut.patent.patent.mapper.ExpertProfileMapper;
import org.ihebut.patent.patent.mapper.OrganizationMapper;
import org.ihebut.patent.patent.mapper.UserAccountMapper;
import org.ihebut.patent.patent.mapper.UserOrganizationMapper;
import org.ihebut.patent.patent.mapper.UserProfileMapper;
import org.ihebut.patent.patent.security.CurrentUser;
import org.ihebut.patent.patent.service.AuditLogService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final CurrentUser currentUser;
    private final UserAccountMapper userAccountMapper;
    private final UserProfileMapper userProfileMapper;
    private final ExpertProfileMapper expertProfileMapper;
    private final UserOrganizationMapper userOrganizationMapper;
    private final OrganizationMapper organizationMapper;
    private final AuditLogService auditLogService;

    public UserController(
            CurrentUser currentUser,
            UserAccountMapper userAccountMapper,
            UserProfileMapper userProfileMapper,
            ExpertProfileMapper expertProfileMapper,
            UserOrganizationMapper userOrganizationMapper,
            OrganizationMapper organizationMapper,
            AuditLogService auditLogService
    ) {
        this.currentUser = currentUser;
        this.userAccountMapper = userAccountMapper;
        this.userProfileMapper = userProfileMapper;
        this.expertProfileMapper = expertProfileMapper;
        this.userOrganizationMapper = userOrganizationMapper;
        this.organizationMapper = organizationMapper;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> me(HttpServletRequest httpRequest) {
        long userId = currentUser.requireUserId();

        UserAccount user = userAccountMapper.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "用户不存在"));

        Map<String, Object> userView = new HashMap<>();
        userView.put("id", user.getId());
        userView.put("username", user.getUsername());
        userView.put("phone", user.getPhone());
        userView.put("email", user.getEmail());
        userView.put("status", user.getStatus());
        userView.put("userType", user.getUserType());
        userView.put("createdAt", user.getCreatedAt());
        userView.put("updatedAt", user.getUpdatedAt());

        Map<String, Object> profileView = new HashMap<>();
        Optional<UserProfile> profileOpt = userProfileMapper.findById(userId);
        if (profileOpt.isPresent()) {
            UserProfile profile = profileOpt.get();
            profileView.put("userId", profile.getUserId());
            profileView.put("nickname", profile.getNickname());
            profileView.put("avatarUrl", profile.getAvatarUrl());
            profileView.put("realName", profile.getRealName());
            profileView.put("idNumber", profile.getIdNumber());
            profileView.put("createdAt", profile.getCreatedAt());
            profileView.put("updatedAt", profile.getUpdatedAt());
        }

        Map<String, Object> expertProfileView = new HashMap<>();
        Optional<ExpertProfile> expertProfileOpt = expertProfileMapper.findById(userId);
        if (expertProfileOpt.isPresent()) {
            ExpertProfile expertProfile = expertProfileOpt.get();
            expertProfileView.put("userId", expertProfile.getUserId());
            expertProfileView.put("field", expertProfile.getField());
            expertProfileView.put("expertise", expertProfile.getExpertise());
            expertProfileView.put("achievements", expertProfile.getAchievements());
            expertProfileView.put("contactInfo", expertProfile.getContactInfo());
            expertProfileView.put("certStatus", expertProfile.getCertStatus());
            expertProfileView.put("certSubmitAt", expertProfile.getCertSubmitAt());
            expertProfileView.put("certAuditAt", expertProfile.getCertAuditAt());
            expertProfileView.put("certAuditBy", expertProfile.getCertAuditBy());
            expertProfileView.put("createdAt", expertProfile.getCreatedAt());
            expertProfileView.put("updatedAt", expertProfile.getUpdatedAt());
        }

        Map<String, Object> primaryOrg = new HashMap<>();
        Optional<UserOrganization> relOpt = userOrganizationMapper.findFirstByUserIdAndIsPrimaryTrue(userId);
        if (relOpt.isPresent()) {
            UserOrganization rel = relOpt.get();
            Optional<Organization> orgOpt = organizationMapper.findById(rel.getOrgId());
            if (orgOpt.isPresent()) {
                Organization org = orgOpt.get();
                primaryOrg.put("orgId", org.getId());
                primaryOrg.put("name", org.getName());
                primaryOrg.put("type", org.getType());
                primaryOrg.put("relationType", rel.getRelationType());
                primaryOrg.put("positionTitle", rel.getPositionTitle());
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("user", userView);
        data.put("profile", profileView.isEmpty() ? null : profileView);
        data.put("expertProfile", expertProfileView.isEmpty() ? null : expertProfileView);
        data.put("primaryOrganization", primaryOrg.isEmpty() ? null : primaryOrg);

        auditLogService.logUserAction(httpRequest, userId, "READ", "查看个人资料", "user_profile", String.valueOf(userId), "查看个人中心资料", true);
        return ApiResponse.ok(data);
    }

    @PutMapping("/me/profile")
    @Transactional
    public ApiResponse<UserProfile> updateProfile(@RequestBody UserProfileUpdateRequest request, HttpServletRequest httpRequest) {
        long userId = currentUser.requireUserId();

        userAccountMapper.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "用户不存在"));

        UserProfile profile = userProfileMapper.findById(userId).orElse(new UserProfile());
        if (profile.getUserId() == null) {
            profile.setUserId(userId);
        }

        if (request != null) {
            if (request.getNickname() != null && !request.getNickname().isBlank()) {
                profile.setNickname(request.getNickname().trim());
            }
            if (request.getAvatarUrl() != null) {
                profile.setAvatarUrl(request.getAvatarUrl());
            }
            if (request.getRealName() != null && !request.getRealName().isBlank()) {
                profile.setRealName(request.getRealName().trim());
            }
            if (request.getIdNumber() != null && !request.getIdNumber().isBlank()) {
                profile.setIdNumber(request.getIdNumber().trim());
            }
        }

        UserProfile savedProfile = userProfileMapper.save(profile);
        auditLogService.logUserAction(httpRequest, userId, "UPDATE", "修改个人资料", "user_profile", String.valueOf(userId), "更新昵称/头像/实名信息", true);
        return ApiResponse.ok(savedProfile);
    }
}
