package org.ihebut.patent.patent.controller;

import org.ihebut.patent.patent.dto.ApiResponse;
import org.ihebut.patent.patent.dto.PatentEsSearchResponse;
import org.ihebut.patent.patent.dto.PatentEsStatusResponse;
import org.ihebut.patent.patent.search.PatentSearchDocument;
import org.ihebut.patent.patent.service.PatentEsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/patents/es")
public class PatentEsController {
    private final PatentEsService patentEsService;

    public PatentEsController(PatentEsService patentEsService) {
        this.patentEsService = patentEsService;
    }

    @GetMapping({"/search", "/search/"})
    public ApiResponse<PatentEsSearchResponse> search(
            @RequestParam(required = false) String category,
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "false") boolean phrase
    ) {
        try {
            return ApiResponse.ok(patentEsService.search(category, query, page, size, phrase));
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            return ApiResponse.fail(e.getMessage() == null ? "请求失败" : e.getMessage());
        }
    }

    @GetMapping({"/status", "/status/"})
    public ApiResponse<PatentEsStatusResponse> status() {
        try {
            return ApiResponse.ok(patentEsService.status());
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            return ApiResponse.fail(e.getMessage() == null ? "请求失败" : e.getMessage());
        }
    }

    @GetMapping({"/detail", "/detail/"})
    public ApiResponse<PatentSearchDocument> detail(
            @RequestParam Long datasetId,
            @RequestParam String recordId
    ) {
        if (datasetId == null) {
            throw new ResponseStatusException(BAD_REQUEST, "datasetId不能为空");
        }
        if (recordId == null || recordId.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "recordId不能为空");
        }
        String normalizedRecordId = recordId.trim();
        PatentSearchDocument document = patentEsService.findDetail(datasetId, normalizedRecordId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "未找到对应专利详情"));
        return ApiResponse.ok(document);
    }

    @PostMapping({"/reindex", "/reindex/"})
    public ApiResponse<Void> reindex(@RequestParam(required = false) String category) {
        try {
            if (category == null || category.isBlank()) {
                patentEsService.reindexAll();
            } else {
                patentEsService.reindexCategory(category);
            }
            return ApiResponse.ok();
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            return ApiResponse.fail(e.getMessage() == null ? "请求失败" : e.getMessage());
        }
    }

    @PostMapping({"/index-one", "/index-one/"})
    public ApiResponse<Void> indexOne(@RequestParam String category, @RequestParam String publicNum) {
        try {
            patentEsService.indexOne(category, publicNum);
            return ApiResponse.ok();
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            return ApiResponse.fail(e.getMessage() == null ? "请求失败" : e.getMessage());
        }
    }
}
