package org.ihebut.patent.patent.controller;

import org.ihebut.patent.patent.dto.AdminPatentDatasetDefinition;
import org.ihebut.patent.patent.dto.ApiResponse;
import org.ihebut.patent.patent.dto.PatentCategoryOptionDto;
import org.ihebut.patent.patent.dto.PatentEsSearchResponse;
import org.ihebut.patent.patent.service.AdminRuntimeService;
import org.ihebut.patent.patent.service.PatentEsService;
import org.ihebut.patent.patent.service.PatentTableService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class PatentMetadataController {
    private final AdminRuntimeService adminRuntimeService;
    private final PatentTableService patentTableService;
    private final PatentEsService patentEsService;

    public PatentMetadataController(
            AdminRuntimeService adminRuntimeService,
            PatentTableService patentTableService,
            PatentEsService patentEsService
    ) {
        this.adminRuntimeService = adminRuntimeService;
        this.patentTableService = patentTableService;
        this.patentEsService = patentEsService;
    }

    @GetMapping("/patent-datasets")
    public ApiResponse<List<Map<String, Object>>> listDatasets() {
        return ApiResponse.ok(adminRuntimeService.listDatasets());
    }

    @GetMapping("/patent-categories")
    public ApiResponse<List<PatentCategoryOptionDto>> listCategories() {
        List<Map<String, Object>> datasets = adminRuntimeService.listDatasets();
        List<PatentCategoryOptionDto> out = new ArrayList<>(datasets.size());
        for (Map<String, Object> ds : datasets) {
            Object codeObj = ds.get("code");
            Object nameObj = ds.get("name");
            Object idObj = ds.get("id");
            if (codeObj == null || idObj == null) {
                continue;
            }
            String value = String.valueOf(codeObj).trim();
            String label = nameObj != null ? String.valueOf(nameObj).trim() : value;
            long id = ((Number) idObj).longValue();
            out.add(new PatentCategoryOptionDto(value, label, List.of(id)));
        }
        return ApiResponse.ok(out);
    }

    @GetMapping("/patent-datasets/{id}/search")
    public ApiResponse<Map<String, Object>> searchDataset(
            @PathVariable("id") long datasetId,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        AdminPatentDatasetDefinition dataset = adminRuntimeService.getDatasetDefinition(datasetId);
        PatentEsSearchResponse result = patentEsService.searchByDataset(
                datasetId,
                query == null ? "" : query.trim(),
                Math.max(page, 0),
                Math.max(size, 1),
                false
        );

        List<Map<String, Object>> content = new ArrayList<>();
        for (PatentEsSearchResponse.Hit hit : result.getHits()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("datasetId", hit.getDatasetId() != null ? hit.getDatasetId() : datasetId);
            item.put("datasetName", hit.getDatasetName() != null ? hit.getDatasetName() : dataset.getName());
            item.put("category", patentTableService.isSupportedCategory(hit.getCategory()) ? hit.getCategory() : "");
            item.put("recordId", hit.getRecordId());
            item.put("publicNum", hit.getPublicNum());
            item.put("title", hit.getTitle());
            item.put("abstractText", hit.getAbstractText());
            item.put("applicant", hit.getApplicant());
            item.put("inventor", hit.getInventor());
            content.add(item);
        }

        int safeSize = Math.max(size, 1);
        int safePage = Math.max(page, 0);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("content", content);
        payload.put("totalElements", result.getTotal());
        payload.put("totalPages", (int) Math.ceil(result.getTotal() / (double) safeSize));
        payload.put("size", safeSize);
        payload.put("number", safePage);
        payload.put("first", safePage == 0);
        payload.put("last", (safePage + 1L) * safeSize >= result.getTotal());
        payload.put("empty", content.isEmpty());
        return ApiResponse.ok(payload);
    }
}
