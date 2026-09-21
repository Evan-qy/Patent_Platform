package org.ihebut.patent.patent.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.ihebut.patent.patent.dto.AdminPatentDatasetDefinition;
import org.ihebut.patent.patent.dto.ApiResponse;
import org.ihebut.patent.patent.security.CurrentUser;
import org.ihebut.patent.patent.service.AdminRuntimeService;
import org.ihebut.patent.patent.service.AuditLogService;
import org.ihebut.patent.patent.service.PatentEsService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminRuntimeController {
    private final AdminRuntimeService adminRuntimeService;
    private final PatentEsService patentEsService;
    private final CurrentUser currentUser;
    private final AuditLogService auditLogService;

    @GetMapping("/patent-sources")
    public ApiResponse<List<Map<String, Object>>> listSources(HttpServletRequest request) {
        String adminName = currentUser.requireAdmin().username();
        auditLogService.logAdminAction(request, adminName, "READ", "查看专利数据源", "patent_source", null, "查看专利数据源列表", true);
        return ApiResponse.ok(adminRuntimeService.listSources());
    }

    @GetMapping("/patent-sources/{id}/tables")
    public ApiResponse<List<Map<String, Object>>> listTables(@PathVariable long id, HttpServletRequest request) {
        String adminName = currentUser.requireAdmin().username();
        auditLogService.logAdminAction(request, adminName, "READ", "查看数据源表", "patent_source", String.valueOf(id), "查看数据源表结构", true);
        return ApiResponse.ok(adminRuntimeService.listTables(id));
    }

    @GetMapping("/patent-sources/{id}/tables/{tableName}/columns")
    public ApiResponse<List<Map<String, Object>>> listColumns(@PathVariable long id, @PathVariable String tableName, HttpServletRequest request) {
        String adminName = currentUser.requireAdmin().username();
        auditLogService.logAdminAction(request, adminName, "READ", "查看表字段", "patent_table", tableName, "查看表字段", true);
        return ApiResponse.ok(adminRuntimeService.listColumns(id, tableName));
    }

    @GetMapping("/patent-datasets")
    public ApiResponse<List<Map<String, Object>>> listDatasets(HttpServletRequest request) {
        String adminName = currentUser.requireAdmin().username();
        auditLogService.logAdminAction(request, adminName, "READ", "查看专利数据集", "patent_dataset", null, "查看专利数据集列表", true);
        return ApiResponse.ok(adminRuntimeService.listDatasets());
    }

    @PostMapping("/patent-datasets/discover")
    public ApiResponse<List<Map<String, Object>>> discoverDatasets(HttpServletRequest request) {
        String adminName = currentUser.requireAdmin().username();
        auditLogService.logAdminAction(request, adminName, "QUERY", "发现新数据集", "patent_dataset", null, "自动发现新表", true);
        return ApiResponse.ok(adminRuntimeService.discoverDatasets());
    }

    @PostMapping("/patent-datasets")
    public ApiResponse<Map<String, Object>> createDataset(@RequestBody AdminPatentDatasetDefinition dto, HttpServletRequest request) {
        String adminName = currentUser.requireAdmin().username();
        Map<String, Object> saved = adminRuntimeService.saveDataset(dto, null);
        auditLogService.logAdminAction(request, adminName, "CREATE", "新增专利数据集", "patent_dataset", String.valueOf(saved.get("id")), String.valueOf(saved.get("name")), true);
        return ApiResponse.ok(saved);
    }

    @PutMapping("/patent-datasets/{id}")
    public ApiResponse<Map<String, Object>> updateDataset(@PathVariable long id, @RequestBody AdminPatentDatasetDefinition dto, HttpServletRequest request) {
        String adminName = currentUser.requireAdmin().username();
        Map<String, Object> saved = adminRuntimeService.saveDataset(dto, id);
        auditLogService.logAdminAction(request, adminName, "UPDATE", "修改专利数据集", "patent_dataset", String.valueOf(id), String.valueOf(saved.get("name")), true);
        return ApiResponse.ok(saved);
    }

    @DeleteMapping("/patent-datasets/{id}")
    public ApiResponse<Void> deleteDataset(@PathVariable long id, HttpServletRequest request) {
        String adminName = currentUser.requireAdmin().username();
        adminRuntimeService.deleteDataset(id);
        auditLogService.logAdminAction(request, adminName, "DELETE", "删除专利数据集", "patent_dataset", String.valueOf(id), "删除数据集", true);
        return ApiResponse.ok();
    }

    @PostMapping("/patent-datasets/{id}/reindex")
    public ApiResponse<Void> reindexDataset(@PathVariable long id, HttpServletRequest request) {
        String adminName = currentUser.requireAdmin().username();
        patentEsService.reindexDataset(id);
        auditLogService.logAdminAction(request, adminName, "UPDATE", "重建数据集索引", "patent_dataset", String.valueOf(id), "重建 ES 索引", true);
        return ApiResponse.ok();
    }

    @PostMapping("/sql/execute")
    public ApiResponse<Map<String, Object>> executeSql(@RequestBody SqlExecuteRequest request, HttpServletRequest httpRequest) {
        String adminName = currentUser.requireAdmin().username();
        long sourceId = request.dataSourceId() == null ? 1L : request.dataSourceId();
        Map<String, Object> result = adminRuntimeService.executeSql(0L, sourceId, request.sql());
        auditLogService.logAdminAction(httpRequest, adminName, "UPDATE", "执行 SQL", "sql_console", String.valueOf(sourceId), request.sql(), true);
        return ApiResponse.ok(result);
    }

    @GetMapping("/sql/history")
    public ApiResponse<List<Map<String, Object>>> getSqlHistory(HttpServletRequest request) {
        String adminName = currentUser.requireAdmin().username();
        auditLogService.logAdminAction(request, adminName, "READ", "查看 SQL 历史", "sql_console", null, "查看 SQL 执行历史", true);
        return ApiResponse.ok(adminRuntimeService.getSqlHistory());
    }

    public record SqlExecuteRequest(Long dataSourceId, String sql) {}
}
