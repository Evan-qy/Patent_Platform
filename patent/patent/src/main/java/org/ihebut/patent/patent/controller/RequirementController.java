package org.ihebut.patent.patent.controller;

import org.ihebut.patent.patent.dto.*;
import org.ihebut.patent.patent.entity.*;
import org.ihebut.patent.patent.mapper.*;
import org.ihebut.patent.patent.security.CurrentUser;
import org.ihebut.patent.patent.service.AuditLogService;
import org.ihebut.patent.patent.service.PatentTableService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
@RequestMapping("/api/requirements")
public class RequirementController {
    private final CurrentUser currentUser;
    private final RequirementMapper requirementMapper;
    private final PatentTableService patentTableService;
    private final UserPatentMapper userPatentMapper;
    private final ExpertProfileMapper expertProfileMapper;
    private final RequirementPatentMatchMapper requirementPatentMatchMapper;
    private final RequirementExpertMatchMapper requirementExpertMatchMapper;
    private final AuditLogService auditLogService;

    public RequirementController(
            CurrentUser currentUser,
            RequirementMapper requirementMapper,
            PatentTableService patentTableService,
            UserPatentMapper userPatentMapper,
            ExpertProfileMapper expertProfileMapper,
            RequirementPatentMatchMapper requirementPatentMatchMapper,
            RequirementExpertMatchMapper requirementExpertMatchMapper,
            AuditLogService auditLogService
    ) {
        this.currentUser = currentUser;
        this.requirementMapper = requirementMapper;
        this.patentTableService = patentTableService;
        this.userPatentMapper = userPatentMapper;
        this.expertProfileMapper = expertProfileMapper;
        this.requirementPatentMatchMapper = requirementPatentMatchMapper;
        this.requirementExpertMatchMapper = requirementExpertMatchMapper;
        this.auditLogService = auditLogService;
    }

    @PostMapping
    public ApiResponse<Requirement> create(@RequestBody RequirementCreateRequest request, HttpServletRequest httpRequest) {
        long userId = currentUser.requireUserId();
        if (request == null || request.getTitle() == null || request.getTitle().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "title不能为空");
        }
        Requirement r = new Requirement();
        r.setTitle(request.getTitle().trim());
        r.setDescription(request.getDescription());
        r.setKeywords(request.getKeywords());
        r.setTechDirection(request.getTechDirection());
        r.setCooperationMode(request.getCooperationMode());
        r.setContactInfo(request.getContactInfo());
        r.setBudget(request.getBudget());
        r.setDeadline(request.getDeadline());
        r.setRequesterUserId(userId);
        r.setRequesterOrgId(request.getRequesterOrgId());
        Requirement saved = requirementMapper.save(r);
        auditLogService.logUserAction(httpRequest, userId, "CREATE", "新增技术需求", "requirement", String.valueOf(saved.getId()), saved.getTitle(), true);
        return ApiResponse.ok(saved);
    }

    @GetMapping
    public ApiResponse<Page<Requirement>> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String query,
            @RequestParam(required = false, defaultValue = "false") boolean mine,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest httpRequest
    ) {
        PageRequest pageable = PageRequest.of(page, size);
        if (mine) {
            long userId = currentUser.requireUserId();
            Page<Requirement> result = requirementMapper.findByRequesterUserIdOrderByCreatedDateDesc(userId, pageable);
            auditLogService.logUserAction(httpRequest, userId, "READ", "查看我的需求列表", "requirement", null, "page=" + page + ", size=" + size, true);
            return ApiResponse.ok(result);
        }
        // techDirection 对应 category
        String techDirection = (category != null && !category.isBlank()) ? category.trim() : null;
        String q = (query != null && !query.isBlank()) ? query.trim() : null;
        Page<Requirement> result = requirementMapper.search(techDirection, q, pageable);
        auditLogService.logAnonymousAction(httpRequest, null, "QUERY", "检索需求列表", "category=" + techDirection + ", query=" + q, true);
        return ApiResponse.ok(result);
    }

    @GetMapping("/{id}/match-patents")
    public ApiResponse<List<PatentMatchResult>> matchPatents(@PathVariable Long id) {
        long userId = currentUser.requireUserId();
        Requirement req = requirementMapper.findById(id).orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "需求不存在"));
        String keywords = (req.getKeywords() != null && !req.getKeywords().isBlank()) ? req.getKeywords() : req.getTitle();
        String query = keywords == null ? "" : keywords.trim();

        List<PatentMatchResult> results = new ArrayList<>();

        if (!query.isEmpty()) {
            results.addAll(toExternal("wind", patentTableService.search("wind", query)));
            results.addAll(toExternal("solar", patentTableService.search("solar", query)));
            results.addAll(toExternal("biomass", patentTableService.search("biomass", query)));
            results.addAll(toExternal("hydrogen", patentTableService.search("hydrogen", query)));
            results.addAll(toExternal("lilon", patentTableService.search("lilon", query)));

            for (UserPatent p : userPatentMapper.findByVisibility("PUBLIC")) {
                if (contains(p.getTitle(), query)
                        || contains(p.getAbstractText(), query)
                        || contains(p.getApplicant(), query)
                        || contains(p.getInventor(), query)) {
                    results.add(new PatentMatchResult("USER", p.getCategory(), p.getPublicNum(), p.getId(), p.getTitle(), p.getApplicant(), p.getInventor()));
                }
            }

            for (UserPatent p : userPatentMapper.findByOwnerUserId(userId)) {
                if (!"PUBLIC".equalsIgnoreCase(p.getVisibility())
                        && (contains(p.getTitle(), query)
                        || contains(p.getAbstractText(), query)
                        || contains(p.getApplicant(), query)
                        || contains(p.getInventor(), query))) {
                    results.add(new PatentMatchResult("USER", p.getCategory(), p.getPublicNum(), p.getId(), p.getTitle(), p.getApplicant(), p.getInventor()));
                }
            }
        }

        return ApiResponse.ok(results);
    }

    @GetMapping("/{id}/match-experts")
    public ApiResponse<List<ExpertMatchResult>> matchExperts(@PathVariable Long id) {
        Requirement req = requirementMapper.findById(id).orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "需求不存在"));
        String keywords = (req.getKeywords() != null && !req.getKeywords().isBlank()) ? req.getKeywords() : req.getTitle();
        String q = keywords == null ? "" : keywords.trim();

        List<ExpertMatchResult> result = new ArrayList<>();
        if (q.isEmpty()) return ApiResponse.ok(result);

        List<ExpertProfile> matches = expertProfileMapper
                .findByCertStatusAndFieldContainingOrCertStatusAndExpertiseContaining("APPROVED", q, "APPROVED", q);
        for (ExpertProfile p : matches) {
            result.add(new ExpertMatchResult(p.getUserId(), p.getField(), p.getExpertise(), p.getContactInfo()));
        }
        return ApiResponse.ok(result);
    }

    @PostMapping("/{id}/match-patents/persist")
    @Transactional
    public ApiResponse<Void> persistPatentMatches(@PathVariable Long id, @RequestBody RequirementPatentMatchPersistRequest request) {
        if (!requirementMapper.existsById(id)) {
            throw new ResponseStatusException(BAD_REQUEST, "需求不存在");
        }
        if (request != null && request.getItems() != null) {
            for (RequirementPatentMatchPersistRequest.Item item : request.getItems()) {
                String source = "EXTERNAL";
                // 检查是否已存在
                var existing = requirementPatentMatchMapper.findByRequirementIdAndPatentSourceAndPatentCategoryAndPatentPublicNum(
                        id, source, item.getPatentCategory(), item.getPatentPublicNum()
                );

                if (existing.isPresent()) {
                    // 更新分数和理由
                    RequirementPatentMatch m = existing.get();
                    m.setMatchScore(toScore(item.getMatchScore()));
                    m.setMatchReason(item.getMatchReason());
                    requirementPatentMatchMapper.save(m);
                } else {
                    // 插入新记录
                    RequirementPatentMatch m = new RequirementPatentMatch();
                    m.setRequirementId(id);
                    m.setPatentSource(source);
                    m.setPatentCategory(item.getPatentCategory());
                    m.setPatentPublicNum(item.getPatentPublicNum());
                    m.setMatchScore(toScore(item.getMatchScore()));
                    m.setMatchReason(item.getMatchReason());
                    requirementPatentMatchMapper.save(m);
                }
            }
        }
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/match-experts/persist")
    @Transactional
    public ApiResponse<Void> persistExpertMatches(@PathVariable Long id, @RequestBody RequirementExpertMatchPersistRequest request) {
        if (!requirementMapper.existsById(id)) {
            throw new ResponseStatusException(BAD_REQUEST, "需求不存在");
        }
        if (request != null && request.getItems() != null) {
            for (RequirementExpertMatchPersistRequest.Item item : request.getItems()) {
                RequirementExpertMatch m = new RequirementExpertMatch();
                m.setRequirementId(id);
                m.setExpertUserId(item.getExpertId());
                m.setMatchScore(toScore(item.getMatchScore()));
                m.setMatchReason(item.getMatchReason());
                requirementExpertMatchMapper.save(m);
            }
        }
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ApiResponse<Void> delete(@PathVariable Long id, HttpServletRequest httpRequest) {
        long userId = currentUser.requireUserId();
        Requirement req = requirementMapper.findById(id)
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "需求不存在"));
        
        if (!req.getRequesterUserId().equals(userId)) {
            throw new ResponseStatusException(BAD_REQUEST, "无权删除此需求");
        }
        
        requirementPatentMatchMapper.deleteByRequirementId(id);
        requirementExpertMatchMapper.deleteByRequirementId(id);
        requirementMapper.deleteById(id);
        renumberRequirements();
        auditLogService.logUserAction(httpRequest, userId, "DELETE", "删除技术需求", "requirement", String.valueOf(id), req.getTitle(), true);
        return ApiResponse.ok();
    }

    private void renumberRequirements() {
        List<Requirement> allRequirements = requirementMapper.findAllByOrderByIdAsc();
        for (int i = 0; i < allRequirements.size(); i++) {
            Requirement req = allRequirements.get(i);
            Long newId = (long) (i + 1);
            if (!req.getId().equals(newId)) {
                Long oldId = req.getId();
                
                List<RequirementPatentMatch> patentMatches = requirementPatentMatchMapper.findByRequirementId(oldId);
                for (RequirementPatentMatch match : patentMatches) {
                    match.setRequirementId(newId);
                    requirementPatentMatchMapper.save(match);
                }
                
                List<RequirementExpertMatch> expertMatches = requirementExpertMatchMapper.findByRequirementId(oldId);
                for (RequirementExpertMatch match : expertMatches) {
                    match.setRequirementId(newId);
                    requirementExpertMatchMapper.save(match);
                }
                
                req.setId(newId);
                requirementMapper.save(req);
            }
        }
    }

    private List<PatentMatchResult> toExternal(String category, List<?> patents) {
        List<PatentMatchResult> items = new ArrayList<>();
        for (Object p : patents) {
            if (p instanceof PatentBase patent) {
                items.add(new PatentMatchResult(
                        "EXTERNAL",
                        category,
                        patent.getPublicNum(),
                        null,
                        patent.getTitle(),
                        patent.getApplicant(),
                        patent.getInventor()
                ));
            }
        }
        return items;
    }

    private boolean contains(String s, String q) {
        return s != null && s.contains(q);
    }

    private static BigDecimal toScore(Double v) {
        if (v == null) return null;
        return BigDecimal.valueOf(v).setScale(3, RoundingMode.HALF_UP);
    }
}
