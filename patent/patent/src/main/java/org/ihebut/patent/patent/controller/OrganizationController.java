package org.ihebut.patent.patent.controller;

import org.ihebut.patent.patent.dto.ApiResponse;
import org.ihebut.patent.patent.dto.OrganizationCreateRequest;
import org.ihebut.patent.patent.dto.OrganizationMemberRequest;
import org.ihebut.patent.patent.entity.Organization;
import org.ihebut.patent.patent.entity.UserOrganization;
import org.ihebut.patent.patent.mapper.OrganizationMapper;
import org.ihebut.patent.patent.mapper.UserOrganizationMapper;
import org.ihebut.patent.patent.security.CurrentUser;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
@RequestMapping("/api/organizations")
public class OrganizationController {
    private final CurrentUser currentUser;
    private final OrganizationMapper organizationMapper;
    private final UserOrganizationMapper userOrganizationMapper;

    public OrganizationController(CurrentUser currentUser, OrganizationMapper organizationMapper, UserOrganizationMapper userOrganizationMapper) {
        this.currentUser = currentUser;
        this.organizationMapper = organizationMapper;
        this.userOrganizationMapper = userOrganizationMapper;
    }

    @PostMapping
    public ApiResponse<Organization> create(@RequestBody OrganizationCreateRequest request) {
        if (request == null || request.getName() == null || request.getName().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "name不能为空");
        }
        if (request.getType() == null || request.getType().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "type不能为空");
        }
        Organization org = new Organization();
        org.setName(request.getName().trim());
        org.setType(request.getType().trim());
        org.setCreditCode(request.getCreditCode());
        org.setAddress(request.getAddress());
        org.setContactName(request.getContactName());
        org.setContactPhone(request.getContactPhone());
        org.setContactEmail(request.getContactEmail());
        return ApiResponse.ok(organizationMapper.save(org));
    }

    @GetMapping
    public ApiResponse<List<Organization>> search(@RequestParam(required = false) String query, @RequestParam(required = false) String type) {
        String q = query == null ? "" : query.trim();
        if (type != null && !type.isBlank()) {
            if (q.isEmpty()) {
                return ApiResponse.ok(organizationMapper.findByType(type.trim()));
            }
            return ApiResponse.ok(organizationMapper.findByTypeAndNameContaining(type.trim(), q));
        }
        if (q.isEmpty()) {
            return ApiResponse.ok(organizationMapper.findAll());
        }
        return ApiResponse.ok(organizationMapper.findByNameContaining(q));
    }

    @PostMapping("/{orgId}/members")
    public ApiResponse<UserOrganization> join(@PathVariable Long orgId, @RequestBody OrganizationMemberRequest request) {
        long userId = currentUser.requireUserId();
        if (!organizationMapper.existsById(orgId)) {
            throw new ResponseStatusException(BAD_REQUEST, "机构不存在");
        }
        UserOrganization rel = userOrganizationMapper.findByUserIdAndOrgId(userId, orgId).orElse(null);
        if (rel == null) {
            rel = new UserOrganization();
            rel.setUserId(userId);
            rel.setOrgId(orgId);
        }
        if (request != null) {
            if (request.getRelationType() != null && !request.getRelationType().isBlank()) {
                rel.setRelationType(request.getRelationType().trim());
            }
            if (request.getPositionTitle() != null) {
                rel.setPositionTitle(request.getPositionTitle());
            }
            if (request.getIsPrimary() != null) {
                rel.setIsPrimary(request.getIsPrimary());
            }
        }
        if (Boolean.TRUE.equals(rel.getIsPrimary())) {
            List<UserOrganization> existingRels = userOrganizationMapper.findByUserId(userId);
            for (UserOrganization existing : existingRels) {
                if (existing.getId() != null
                        && rel.getId() != null
                        && existing.getId().equals(rel.getId())) {
                    continue;
                }
                if (Boolean.TRUE.equals(existing.getIsPrimary())) {
                    existing.setIsPrimary(false);
                    userOrganizationMapper.save(existing);
                }
            }
        }
        return ApiResponse.ok(userOrganizationMapper.save(rel));
    }
}
