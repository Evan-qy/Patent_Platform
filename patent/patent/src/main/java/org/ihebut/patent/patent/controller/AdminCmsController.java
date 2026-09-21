package org.ihebut.patent.patent.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.ihebut.patent.patent.dto.HomeContentPayload;
import org.ihebut.patent.patent.entity.CmsArticle;
import org.ihebut.patent.patent.security.CurrentUser;
import org.ihebut.patent.patent.service.AuditLogService;
import org.ihebut.patent.patent.service.CmsService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminCmsController {
    private final CmsService cmsService;
    private final CurrentUser currentUser;
    private final AuditLogService auditLogService;

    @GetMapping("/home-content")
    public HomeContentPayload getHomeContent(HttpServletRequest request) {
        String adminName = currentUser.requireAdmin().username();
        auditLogService.logAdminAction(request, adminName, "READ", "查看首页配置", "home_content", "home", "查看首页内容配置", true);
        return cmsService.getHomeContent();
    }

    @PostMapping("/home-content")
    public HomeContentPayload updateHomeContent(@RequestBody HomeContentPayload dto, HttpServletRequest request) {
        String adminName = currentUser.requireAdmin().username();
        HomeContentPayload saved = cmsService.updateHomeContent(dto);
        auditLogService.logAdminAction(request, adminName, "UPDATE", "更新首页配置", "home_content", "home", "更新首页内容配置", true);
        return saved;
    }

    @GetMapping("/articles")
    public List<CmsArticle> getAllArticles(HttpServletRequest request) {
        String adminName = currentUser.requireAdmin().username();
        auditLogService.logAdminAction(request, adminName, "READ", "查看文章列表", "cms_article", null, "查看文章列表", true);
        return cmsService.getAllArticles();
    }

    @GetMapping("/articles/{id}")
    public CmsArticle getArticle(@PathVariable Long id, HttpServletRequest request) {
        String adminName = currentUser.requireAdmin().username();
        CmsArticle article = cmsService.getAllArticles().stream()
                .filter(item -> id.equals(item.getId()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "文章不存在"));
        auditLogService.logAdminAction(request, adminName, "READ", "查看文章详情", "cms_article", String.valueOf(id), article.getTitle(), true);
        return article;
    }

    @PostMapping("/articles")
    public CmsArticle saveArticle(@RequestBody CmsArticle article, HttpServletRequest request) {
        String adminName = currentUser.requireAdmin().username();
        CmsArticle saved = cmsService.saveArticle(article);
        auditLogService.logAdminAction(request, adminName, "CREATE", "新增文章", "cms_article", String.valueOf(saved.getId()), saved.getTitle(), true);
        return saved;
    }

    @PutMapping("/articles/{id}")
    public CmsArticle updateArticle(@PathVariable Long id, @RequestBody CmsArticle article, HttpServletRequest request) {
        String adminName = currentUser.requireAdmin().username();
        article.setId(id);
        CmsArticle saved = cmsService.saveArticle(article);
        auditLogService.logAdminAction(request, adminName, "UPDATE", "修改文章", "cms_article", String.valueOf(id), saved.getTitle(), true);
        return saved;
    }

    @DeleteMapping("/articles/{id}")
    public void deleteArticle(@PathVariable Long id, HttpServletRequest request) {
        String adminName = currentUser.requireAdmin().username();
        cmsService.deleteArticle(id);
        auditLogService.logAdminAction(request, adminName, "DELETE", "删除文章", "cms_article", String.valueOf(id), "删除文章", true);
    }
}
