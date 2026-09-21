package org.ihebut.patent.patent.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.ihebut.patent.patent.dto.AdminPatentDatasetDefinition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class AdminRuntimeService {
    private static final long PRIMARY_SOURCE_ID = 1L;
    private static final int SQL_PREVIEW_LIMIT = 200;
    private static final Pattern READ_ONLY_SQL_PATTERN =
            Pattern.compile("^(SELECT|SHOW|DESC|DESCRIBE|EXPLAIN)\\b", Pattern.CASE_INSENSITIVE);

    private final DataSource dataSource;
    private final Environment environment;
    private final ObjectMapper objectMapper;

    @Value("${admin.dataset.storage-path:./config/admin-patent-datasets.json}")
    private String datasetStoragePath;

    private final AtomicLong sqlHistoryId = new AtomicLong(1);
    private final Deque<Map<String, Object>> sqlHistory = new ConcurrentLinkedDeque<>();
    private final AtomicLong datasetIdSequence = new AtomicLong(1000);

    private List<AdminPatentDatasetDefinition> datasetCatalog = new ArrayList<>();

    @PostConstruct
    synchronized void initCatalog() {
        datasetCatalog = migrateLoadedDatasets(loadDatasets());
        persistDatasets();
        datasetIdSequence.set(datasetCatalog.stream()
                .map(AdminPatentDatasetDefinition::getId)
                .filter(Objects::nonNull)
                .mapToLong(Long::longValue)
                .max()
                .orElse(1000L) + 1L);
    }

    public synchronized List<Map<String, Object>> listSources() {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String jdbcUrl = firstNonBlank(metaData.getURL(), environment.getProperty("spring.datasource.url"));
            String username = firstNonBlank(metaData.getUserName(), environment.getProperty("spring.datasource.username"));

            Map<String, Object> source = new LinkedHashMap<>();
            source.put("id", PRIMARY_SOURCE_ID);
            source.put("code", "primary-local");
            source.put("name", "当前 patent 主库");
            source.put("sourceType", "PRIMARY");
            source.put("jdbcUrl", jdbcUrl);
            source.put("databaseName", extractDatabaseName(jdbcUrl));
            source.put("username", username);
            source.put("enabled", true);
            source.put("builtIn", true);
            source.put("remarks", "当前应用正在使用的主业务数据库");
            return List.of(source);
        } catch (Exception ex) {
            throw new ResponseStatusException(BAD_REQUEST, "读取数据源信息失败: " + ex.getMessage(), ex);
        }
    }

    public synchronized List<Map<String, Object>> listTables(long sourceId) {
        assertPrimarySource(sourceId);
        try (Connection connection = dataSource.getConnection()) {
            String catalog = connection.getCatalog();
            List<Map<String, Object>> tables = new ArrayList<>();
            String sql = """
                    SELECT TABLE_NAME, TABLE_TYPE
                    FROM information_schema.TABLES
                    WHERE TABLE_SCHEMA = ?
                    ORDER BY TABLE_NAME
                    """;
            try (var statement = connection.prepareStatement(sql)) {
                statement.setString(1, catalog);
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("name", rs.getString("TABLE_NAME"));
                        item.put("type", rs.getString("TABLE_TYPE"));
                        item.put("remarks", "");
                        tables.add(item);
                    }
                }
            }
            tables.sort(Comparator.comparing(item -> String.valueOf(item.get("name")), String.CASE_INSENSITIVE_ORDER));
            return tables;
        } catch (Exception ex) {
            throw new ResponseStatusException(BAD_REQUEST, "读取数据表列表失败: " + ex.getMessage(), ex);
        }
    }

    public synchronized List<Map<String, Object>> listColumns(long sourceId, String tableName) {
        assertPrimarySource(sourceId);
        if (tableName == null || tableName.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "表名不能为空");
        }

        try (Connection connection = dataSource.getConnection()) {
            String catalog = connection.getCatalog();
            List<Map<String, Object>> columns = new ArrayList<>();
            String sql = """
                    SELECT COLUMN_NAME, COLUMN_TYPE, CHARACTER_MAXIMUM_LENGTH, IS_NULLABLE, COLUMN_COMMENT
                    FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?
                    ORDER BY ORDINAL_POSITION
                    """;
            try (var statement = connection.prepareStatement(sql)) {
                statement.setString(1, catalog);
                statement.setString(2, tableName);
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("name", rs.getString("COLUMN_NAME"));
                        item.put("type", rs.getString("COLUMN_TYPE"));
                        item.put("size", rs.getObject("CHARACTER_MAXIMUM_LENGTH"));
                        item.put("nullable", "YES".equalsIgnoreCase(rs.getString("IS_NULLABLE")));
                        item.put("remarks", rs.getString("COLUMN_COMMENT"));
                        columns.add(item);
                    }
                }
            }

            if (columns.isEmpty()) {
                throw new ResponseStatusException(NOT_FOUND, "未找到数据表: " + tableName);
            }

            return columns;
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(BAD_REQUEST, "读取字段信息失败: " + ex.getMessage(), ex);
        }
    }

    public synchronized List<Map<String, Object>> listDatasets() {
        return datasetCatalog.stream().map(this::toDatasetMap).toList();
    }

    public synchronized List<Map<String, Object>> discoverDatasets() {
        LinkedHashSet<String> configuredTables = datasetCatalog.stream()
                .map(AdminPatentDatasetDefinition::getTableName)
                .filter(Objects::nonNull)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll);

        List<Map<String, Object>> candidates = new ArrayList<>();
        for (Map<String, Object> table : listTables(PRIMARY_SOURCE_ID)) {
            String tableName = String.valueOf(table.get("name"));
            if (configuredTables.contains(tableName.toLowerCase(Locale.ROOT))) {
                continue;
            }
            AdminPatentDatasetDefinition definition = buildSuggestedDataset(tableName);
            Map<String, Object> row = toDatasetMap(definition);
            row.put("discovered", true);
            candidates.add(row);
        }
        return candidates;
    }

    public synchronized Map<String, Object> saveDataset(AdminPatentDatasetDefinition request, Long id) {
        AdminPatentDatasetDefinition dataset = normalizeDataset(request, id);
        validateDataset(dataset, id);

        if (id == null) {
            dataset.setId(datasetIdSequence.getAndIncrement());
            datasetCatalog.add(dataset);
        } else {
            int index = findDatasetIndex(id);
            datasetCatalog.set(index, dataset);
        }

        persistDatasets();
        return toDatasetMap(dataset);
    }

    public synchronized void deleteDataset(long id) {
        int index = findDatasetIndex(id);
        AdminPatentDatasetDefinition existing = datasetCatalog.get(index);
        if (existing.isBuiltIn()) {
            throw new ResponseStatusException(BAD_REQUEST, "内置数据集不允许删除");
        }
        datasetCatalog.remove(index);
        persistDatasets();
    }

    public synchronized AdminPatentDatasetDefinition getDatasetDefinition(long id) {
        return datasetCatalog.stream()
                .filter(item -> Objects.equals(item.getId(), id))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "未找到数据集"));
    }

    public synchronized List<AdminPatentDatasetDefinition> getEnabledDatasetDefinitions() {
        return datasetCatalog.stream()
                .filter(AdminPatentDatasetDefinition::isEnabled)
                .map(this::copyDataset)
                .toList();
    }

    public synchronized Map<String, Object> executeSql(long adminId, long sourceId, String sql) {
        assertPrimarySource(sourceId);
        String normalizedSql = normalizeSql(sql);
        String statementType = detectStatementType(normalizedSql);
        if (!READ_ONLY_SQL_PATTERN.matcher(statementType).find()) {
            throw new ResponseStatusException(BAD_REQUEST, "SQL 控制台仅允许执行 SELECT、SHOW、DESC、DESCRIBE、EXPLAIN 语句");
        }

        long startedAt = System.currentTimeMillis();
        List<String> columns = new ArrayList<>();
        List<Map<String, Object>> rows = new ArrayList<>();

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.setMaxRows(SQL_PREVIEW_LIMIT);
            boolean hasResultSet = statement.execute(normalizedSql);
            if (!hasResultSet) {
                throw new ResponseStatusException(BAD_REQUEST, "当前 SQL 没有返回结果集");
            }

            try (ResultSet resultSet = statement.getResultSet()) {
                ResultSetMetaData metaData = resultSet.getMetaData();
                for (int i = 1; i <= metaData.getColumnCount(); i++) {
                    columns.add(metaData.getColumnLabel(i));
                }
                while (resultSet.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (String column : columns) {
                        row.put(column, resultSet.getObject(column));
                    }
                    rows.add(row);
                }
            }
        } catch (ResponseStatusException ex) {
            appendHistory(adminId, sourceId, statementType, normalizedSql, false, null, ex.getReason(), 0);
            throw ex;
        } catch (Exception ex) {
            appendHistory(adminId, sourceId, statementType, normalizedSql, false, null, ex.getMessage(), 0);
            throw new ResponseStatusException(BAD_REQUEST, "SQL 执行失败: " + ex.getMessage(), ex);
        }

        long durationMs = System.currentTimeMillis() - startedAt;
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("statementType", statementType);
        response.put("affectedRows", rows.size());
        response.put("columns", columns);
        response.put("rows", rows);
        response.put("durationMs", durationMs);

        appendHistory(adminId, sourceId, statementType, normalizedSql, true, rows.size(), "返回 " + rows.size() + " 行记录", durationMs);
        return response;
    }

    public synchronized List<Map<String, Object>> getSqlHistory() {
        return sqlHistory.stream().toList();
    }

    private AdminPatentDatasetDefinition normalizeDataset(AdminPatentDatasetDefinition request, Long id) {
        if (request == null) {
            throw new ResponseStatusException(BAD_REQUEST, "数据集配置不能为空");
        }
        AdminPatentDatasetDefinition base = id == null ? new AdminPatentDatasetDefinition() : copyDataset(getDatasetDefinition(id));
        base.setId(id == null ? base.getId() : id);
        base.setDataSourceId(request.getDataSourceId() == null ? PRIMARY_SOURCE_ID : request.getDataSourceId());
        base.setCode(trimToNull(request.getCode()));
        base.setName(trimToNull(request.getName()));
        base.setCategory(trimToNull(request.getCategory()));
        base.setTableName(trimToNull(request.getTableName()));
        base.setPrimaryKeyColumn(firstNonBlank(trimToNull(request.getPrimaryKeyColumn()), "id"));
        base.setPublicNumColumn(trimToNull(request.getPublicNumColumn()));
        base.setTitleColumn(trimToNull(request.getTitleColumn()));
        base.setAbstractColumn(trimToNull(request.getAbstractColumn()));
        base.setApplicantColumn(trimToNull(request.getApplicantColumn()));
        base.setInventorColumn(trimToNull(request.getInventorColumn()));
        base.setIpcColumn(trimToNull(request.getIpcColumn()));
        base.setCpcColumn(trimToNull(request.getCpcColumn()));
        base.setAppliDateColumn(trimToNull(request.getAppliDateColumn()));
        base.setPublicDateColumn(trimToNull(request.getPublicDateColumn()));
        base.setLegalStatusColumn(trimToNull(request.getLegalStatusColumn()));
        base.setStatusColumn(trimToNull(request.getStatusColumn()));
        base.setDefaultSortColumn(trimToNull(request.getDefaultSortColumn()));
        base.setDefaultSortDirection(firstNonBlank(trimToNull(request.getDefaultSortDirection()), "DESC"));
        base.setSearchFieldsJson(firstNonBlank(trimToNull(request.getSearchFieldsJson()), "[]"));
        base.setEnabled(request.isEnabled());
        base.setBuiltIn(id != null && getDatasetDefinition(id).isBuiltIn());
        base.setRemarks(trimToNull(request.getRemarks()));
        return base;
    }

    private void validateDataset(AdminPatentDatasetDefinition dataset, Long currentId) {
        if (!Objects.equals(dataset.getDataSourceId(), PRIMARY_SOURCE_ID)) {
            throw new ResponseStatusException(BAD_REQUEST, "当前版本只支持 patent 主库");
        }
        if (dataset.getCode() == null || dataset.getName() == null || dataset.getTableName() == null) {
            throw new ResponseStatusException(BAD_REQUEST, "数据集编码、名称、表名不能为空");
        }
        if (dataset.getPublicNumColumn() == null || dataset.getTitleColumn() == null) {
            throw new ResponseStatusException(BAD_REQUEST, "至少需要配置公开号字段和标题字段");
        }
        boolean tableExists = listTables(PRIMARY_SOURCE_ID).stream()
                .map(item -> String.valueOf(item.get("name")))
                .anyMatch(name -> name.equalsIgnoreCase(dataset.getTableName()));
        if (!tableExists) {
            throw new ResponseStatusException(BAD_REQUEST, "数据表不存在: " + dataset.getTableName());
        }
        for (AdminPatentDatasetDefinition item : datasetCatalog) {
            if (currentId != null && Objects.equals(item.getId(), currentId)) {
                continue;
            }
            if (item.getCode() != null && item.getCode().equalsIgnoreCase(dataset.getCode())) {
                throw new ResponseStatusException(BAD_REQUEST, "数据集编码已存在");
            }
        }
    }

    private int findDatasetIndex(long id) {
        for (int i = 0; i < datasetCatalog.size(); i++) {
            if (Objects.equals(datasetCatalog.get(i).getId(), id)) {
                return i;
            }
        }
        throw new ResponseStatusException(NOT_FOUND, "未找到数据集");
    }

    private synchronized List<AdminPatentDatasetDefinition> loadDatasets() {
        try {
            Path path = resolveDatasetStoragePath();
            Files.createDirectories(path.getParent());
            if (!Files.exists(path) || Files.size(path) == 0L) {
                List<AdminPatentDatasetDefinition> defaults = defaultDatasets();
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), defaults);
                return defaults;
            }
            List<AdminPatentDatasetDefinition> loaded = objectMapper.readValue(path.toFile(), new TypeReference<>() {});
            return loaded == null || loaded.isEmpty() ? defaultDatasets() : loaded;
        } catch (Exception ex) {
            throw new ResponseStatusException(BAD_REQUEST, "加载数据集配置失败: " + ex.getMessage(), ex);
        }
    }

    private synchronized void persistDatasets() {
        try {
            Path path = resolveDatasetStoragePath();
            Files.createDirectories(path.getParent());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), datasetCatalog);
        } catch (Exception ex) {
            throw new ResponseStatusException(BAD_REQUEST, "保存数据集配置失败: " + ex.getMessage(), ex);
        }
    }

    private Path resolveDatasetStoragePath() {
        Path path = Paths.get(datasetStoragePath == null || datasetStoragePath.isBlank()
                ? "./config/admin-patent-datasets.json"
                : datasetStoragePath.trim());
        return path.isAbsolute() ? path : path.toAbsolutePath().normalize();
    }

    private List<AdminPatentDatasetDefinition> defaultDatasets() {
        return List.of(
                buildDataset(101L, "wind", "风能专利库", "wind", "patent_wind"),
                buildDataset(102L, "solar", "光伏专利库", "solar", "patent_solar"),
                buildDataset(103L, "biomass", "生物质专利库", "biomass", "patent_biomass"),
                buildDataset(104L, "hydrogen", "氢能专利库", "hydrogen", "patent_hydrogen"),
                buildDataset(105L, "lilon", "锂电专利库", "lilon", "patent_lilon")
        );
    }

    private AdminPatentDatasetDefinition buildDataset(Long id, String code, String name, String category, String tableName) {
        AdminPatentDatasetDefinition dataset = new AdminPatentDatasetDefinition();
        dataset.setId(id);
        dataset.setDataSourceId(PRIMARY_SOURCE_ID);
        dataset.setCode(code);
        dataset.setName(name);
        dataset.setCategory(category);
        dataset.setTableName(tableName);
        dataset.setPrimaryKeyColumn("public_num");
        dataset.setPublicNumColumn("public_num");
        dataset.setTitleColumn("title");
        dataset.setAbstractColumn("abstract");
        dataset.setApplicantColumn("applicant");
        dataset.setInventorColumn("inventor");
        dataset.setIpcColumn("IPC");
        dataset.setCpcColumn("CPC");
        dataset.setAppliDateColumn("appli_date");
        dataset.setPublicDateColumn("public_date");
        dataset.setLegalStatusColumn("legal_status");
        dataset.setStatusColumn("status");
        dataset.setDefaultSortColumn("public_date");
        dataset.setDefaultSortDirection("DESC");
        dataset.setSearchFieldsJson("[\"public_num\",\"title\",\"abstract\",\"applicant\",\"inventor\"]");
        dataset.setEnabled(true);
        dataset.setBuiltIn(true);
        dataset.setRemarks("系统内置专利数据集");
        return dataset;
    }

    private AdminPatentDatasetDefinition buildSuggestedDataset(String tableName) {
        AdminPatentDatasetDefinition dataset = new AdminPatentDatasetDefinition();
        dataset.setDataSourceId(PRIMARY_SOURCE_ID);
        dataset.setCode(toCode(tableName));
        dataset.setCategory(toCode(tableName));
        dataset.setName(tableName + " 数据集");
        dataset.setTableName(tableName);
        Map<String, String> guessed = guessColumns(tableName);
        dataset.setPrimaryKeyColumn(guessPrimaryKey(guessed));
        dataset.setPublicNumColumn(guessed.get("public_num"));
        dataset.setTitleColumn(guessed.get("title"));
        dataset.setAbstractColumn(guessed.get("abstract"));
        dataset.setApplicantColumn(guessed.get("applicant"));
        dataset.setInventorColumn(guessed.get("inventor"));
        dataset.setIpcColumn(guessed.get("ipc"));
        dataset.setCpcColumn(guessed.get("cpc"));
        dataset.setAppliDateColumn(guessed.get("appli_date"));
        dataset.setPublicDateColumn(guessed.get("public_date"));
        dataset.setLegalStatusColumn(guessed.get("legal_status"));
        dataset.setStatusColumn(guessed.get("status"));
        dataset.setDefaultSortColumn(firstNonBlank(dataset.getPublicDateColumn(), dataset.getPrimaryKeyColumn()));
        dataset.setDefaultSortDirection("DESC");
        dataset.setSearchFieldsJson(buildSearchFieldsJson(dataset));
        dataset.setEnabled(false);
        dataset.setBuiltIn(false);
        dataset.setRemarks("自动发现的新表候选配置");
        return dataset;
    }

    private Map<String, String> guessColumns(String tableName) {
        List<Map<String, Object>> columns = listColumns(PRIMARY_SOURCE_ID, tableName);
        Map<String, String> byLower = new LinkedHashMap<>();
        for (Map<String, Object> column : columns) {
            String name = String.valueOf(column.get("name"));
            byLower.put(name.toLowerCase(Locale.ROOT), name);
        }
        Map<String, String> guessed = new LinkedHashMap<>();
        guessed.put("public_num", pickColumn(byLower, "public_num", "public_no", "publicno", "public_number", "publication_no"));
        guessed.put("title", pickColumn(byLower, "title", "patent_title", "name"));
        guessed.put("abstract", pickColumn(byLower, "abstract_text", "abstract", "summary"));
        guessed.put("applicant", pickColumn(byLower, "applicant", "applicants", "assignee"));
        guessed.put("inventor", pickColumn(byLower, "inventor", "inventors", "author"));
        guessed.put("ipc", pickColumn(byLower, "ipc"));
        guessed.put("cpc", pickColumn(byLower, "cpc"));
        guessed.put("appli_date", pickColumn(byLower, "appli_date", "apply_date", "application_date"));
        guessed.put("public_date", pickColumn(byLower, "public_date", "publication_date", "publish_date"));
        guessed.put("legal_status", pickColumn(byLower, "legal_status", "law_state"));
        guessed.put("status", pickColumn(byLower, "status"));
        guessed.put("detail_id", pickColumn(byLower, "detail_id", "id"));
        return guessed;
    }

    private String guessPrimaryKey(Map<String, String> guessed) {
        return firstNonBlank(guessed.get("public_num"), firstNonBlank(guessed.get("detail_id"), "id"));
    }

    private String buildSearchFieldsJson(AdminPatentDatasetDefinition dataset) {
        List<String> fields = new ArrayList<>();
        addIfPresent(fields, dataset.getPublicNumColumn());
        addIfPresent(fields, dataset.getTitleColumn());
        addIfPresent(fields, dataset.getAbstractColumn());
        addIfPresent(fields, dataset.getApplicantColumn());
        addIfPresent(fields, dataset.getInventorColumn());
        try {
            return objectMapper.writeValueAsString(fields);
        } catch (Exception ex) {
            return "[]";
        }
    }

    private List<AdminPatentDatasetDefinition> migrateLoadedDatasets(List<AdminPatentDatasetDefinition> loaded) {
        List<AdminPatentDatasetDefinition> migrated = new ArrayList<>();
        for (AdminPatentDatasetDefinition dataset : loaded) {
            migrated.add(migrateDatasetDefinition(dataset));
        }
        return migrated;
    }

    private AdminPatentDatasetDefinition migrateDatasetDefinition(AdminPatentDatasetDefinition original) {
        AdminPatentDatasetDefinition dataset = copyDataset(original);
        if (dataset.getTableName() == null || dataset.getTableName().isBlank()) {
            return dataset;
        }

        Map<String, String> guessed = guessColumns(dataset.getTableName());
        dataset.setPrimaryKeyColumn(resolveColumn(dataset.getTableName(), dataset.getPrimaryKeyColumn(), guessPrimaryKey(guessed)));
        dataset.setPublicNumColumn(resolveColumn(dataset.getTableName(), dataset.getPublicNumColumn(), guessed.get("public_num")));
        dataset.setTitleColumn(resolveColumn(dataset.getTableName(), dataset.getTitleColumn(), guessed.get("title")));
        dataset.setAbstractColumn(resolveColumn(dataset.getTableName(), dataset.getAbstractColumn(), guessed.get("abstract")));
        dataset.setApplicantColumn(resolveColumn(dataset.getTableName(), dataset.getApplicantColumn(), guessed.get("applicant")));
        dataset.setInventorColumn(resolveColumn(dataset.getTableName(), dataset.getInventorColumn(), guessed.get("inventor")));
        dataset.setIpcColumn(resolveColumn(dataset.getTableName(), dataset.getIpcColumn(), guessed.get("ipc")));
        dataset.setCpcColumn(resolveColumn(dataset.getTableName(), dataset.getCpcColumn(), guessed.get("cpc")));
        dataset.setAppliDateColumn(resolveColumn(dataset.getTableName(), dataset.getAppliDateColumn(), guessed.get("appli_date")));
        dataset.setPublicDateColumn(resolveColumn(dataset.getTableName(), dataset.getPublicDateColumn(), guessed.get("public_date")));
        dataset.setLegalStatusColumn(resolveColumn(dataset.getTableName(), dataset.getLegalStatusColumn(), guessed.get("legal_status")));
        dataset.setStatusColumn(resolveColumn(dataset.getTableName(), dataset.getStatusColumn(), guessed.get("status")));
        dataset.setDefaultSortColumn(resolveColumn(dataset.getTableName(), dataset.getDefaultSortColumn(), firstNonBlank(dataset.getPublicDateColumn(), dataset.getPrimaryKeyColumn())));
        dataset.setSearchFieldsJson(buildSearchFieldsJson(dataset));
        return dataset;
    }

    private String resolveColumn(String tableName, String current, String fallback) {
        if (current == null || current.isBlank() || "id".equalsIgnoreCase(current) || "abstract_text".equalsIgnoreCase(current)) {
            return fallback;
        }
        if (!columnExists(tableName, current) && fallback != null && !fallback.isBlank()) {
            return fallback;
        }
        return current;
    }

    private boolean columnExists(String tableName, String columnName) {
        if (tableName == null || tableName.isBlank() || columnName == null || columnName.isBlank()) {
            return false;
        }
        return listColumns(PRIMARY_SOURCE_ID, tableName).stream()
                .map(item -> String.valueOf(item.get("name")))
                .anyMatch(name -> name.equalsIgnoreCase(columnName));
    }

    private Map<String, Object> toDatasetMap(AdminPatentDatasetDefinition dataset) {
        return objectMapper.convertValue(dataset, new TypeReference<>() {});
    }

    private AdminPatentDatasetDefinition copyDataset(AdminPatentDatasetDefinition dataset) {
        return objectMapper.convertValue(dataset, AdminPatentDatasetDefinition.class);
    }

    private void assertPrimarySource(long sourceId) {
        if (sourceId != PRIMARY_SOURCE_ID) {
            throw new ResponseStatusException(NOT_FOUND, "未找到指定的数据源");
        }
    }

    private String extractDatabaseName(String jdbcUrl) {
        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            return "未知数据库";
        }
        int slashIndex = jdbcUrl.lastIndexOf('/');
        if (slashIndex < 0 || slashIndex == jdbcUrl.length() - 1) {
            return "未知数据库";
        }
        String tail = jdbcUrl.substring(slashIndex + 1);
        int queryIndex = tail.indexOf('?');
        return queryIndex >= 0 ? tail.substring(0, queryIndex) : tail;
    }

    private String normalizeSql(String sql) {
        if (sql == null || sql.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "SQL 不能为空");
        }
        String normalized = sql.trim();
        if (normalized.endsWith(";")) {
            normalized = normalized.substring(0, normalized.length() - 1).trim();
        }
        if (normalized.contains(";")) {
            throw new ResponseStatusException(BAD_REQUEST, "一次只允许执行一条 SQL 语句");
        }
        return normalized;
    }

    private String detectStatementType(String sql) {
        String upper = sql.toUpperCase(Locale.ROOT);
        if (upper.startsWith("DESCRIBE ")) {
            return "DESCRIBE";
        }
        if (upper.startsWith("DESC ")) {
            return "DESC";
        }
        int blank = upper.indexOf(' ');
        return blank > 0 ? upper.substring(0, blank) : upper;
    }

    private void appendHistory(long adminId, long sourceId, String statementType, String sqlText, boolean success, Integer affectedRows, String preview, long durationMs) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", sqlHistoryId.getAndIncrement());
        item.put("adminId", adminId);
        item.put("dataSourceId", sourceId);
        item.put("statementType", statementType);
        item.put("sqlText", sqlText);
        item.put("successFlag", success);
        item.put("affectedRows", affectedRows);
        item.put("resultPreview", success ? preview : null);
        item.put("errorMessage", success ? null : preview);
        item.put("durationMs", durationMs);
        item.put("executedAt", OffsetDateTime.now().toString());
        sqlHistory.addFirst(item);
        while (sqlHistory.size() > 20) {
            sqlHistory.removeLast();
        }
    }

    private static String pickColumn(Map<String, String> columns, String... candidates) {
        for (String candidate : candidates) {
            String found = columns.get(candidate.toLowerCase(Locale.ROOT));
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static String toCode(String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
    }

    private static void addIfPresent(List<String> fields, String value) {
        if (value != null && !value.isBlank()) {
            fields.add(value);
        }
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String firstNonBlank(String first, String fallback) {
        return (first != null && !first.isBlank()) ? first : Objects.toString(fallback, "");
    }
}
