package org.ihebut.patent.patent.controller;

import lombok.RequiredArgsConstructor;
import org.ihebut.patent.patent.dto.HomeContentPayload;
import org.ihebut.patent.patent.entity.CmsArticle;
import org.ihebut.patent.patent.service.CmsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CmsController {
    private final CmsService cmsService;

    @GetMapping("/home-content")
    public HomeContentPayload getHomeContent() {
        return cmsService.getHomeContent();
    }

    @GetMapping("/articles")
    public List<CmsArticle> getArticles() {
        return cmsService.getPublishedArticles();
    }

    @GetMapping("/articles/{slug}")
    public CmsArticle getArticle(@PathVariable String slug) {
        return cmsService.getPublishedArticle(slug)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Article not found"));
    }
}
