package org.ihebut.patent.patent.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import org.ihebut.patent.patent.dto.AdminPatentDatasetDefinition;
import org.ihebut.patent.patent.dto.PatentEsSearchResponse;
import org.ihebut.patent.patent.dto.PatentEsStatusResponse;
import org.ihebut.patent.patent.search.PatentEsIndexManager;
import org.ihebut.patent.patent.search.PatentSearchDocument;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Service
public class PatentEsService {
    private static final int REINDEX_BATCH_SIZE = 20;
    private static final int STREAMING_FETCH_SIZE = Integer.MIN_VALUE;
    private final boolean enabled;
    private final String indexName;
    private final ElasticsearchOperations elasticsearchOperations;
    private final ElasticsearchClient elasticsearchClient;
    private final PatentEsIndexManager patentEsIndexManager;
    private final AdminRuntimeService adminRuntimeService;
    private final DataSource dataSource;

    public PatentEsService(
            @Value("${search.es.enabled:false}") boolean enabled,
            @Value("${search.es.index.patent:patents}") String indexName,
            ElasticsearchOperations elasticsearchOperations,
            ElasticsearchClient elasticsearchClient,
            PatentEsIndexManager patentEsIndexManager,
            AdminRuntimeService adminRuntimeService,
            DataSource dataSource
    ) {
        this.enabled = enabled;
        this.indexName = indexName;
        this.elasticsearchOperations = elasticsearchOperations;
        this.elasticsearchClient = elasticsearchClient;
        this.patentEsIndexManager = patentEsIndexManager;
        this.adminRuntimeService = adminRuntimeService;
        this.dataSource = dataSource;
    }

    public void reindexAll() {
        requireEnabled();
        patentEsIndexManager.recreateIndex();
        for (AdminPatentDatasetDefinition dataset : adminRuntimeService.getEnabledDatasetDefinitions()) {
            reindexDataset(dataset.getId());
        }
    }

    public void reindexCategory(String category) {
        requireEnabled();
        if (category == null || category.isBlank()) {
            reindexAll();
            return;
        }
        AdminPatentDatasetDefinition dataset = adminRuntimeService.getEnabledDatasetDefinitions().stream()
                .filter(item -> category.equalsIgnoreCase(item.getCategory()) || category.equalsIgnoreCase(item.getCode()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "未找到启用的数据集分类: " + category));
        reindexDataset(dataset.getId());
    }

    public void reindexDataset(long datasetId) {
        requireEnabled();
        patentEsIndexManager.ensureIndexExists();
        AdminPatentDatasetDefinition dataset = adminRuntimeService.getDatasetDefinition(datasetId);
        if (!dataset.isEnabled()) {
            throw new ResponseStatusException(BAD_REQUEST, "当前数据集未启用");
        }
        List<IndexQuery> batch = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     buildDatasetQuery(dataset),
                     ResultSet.TYPE_FORWARD_ONLY,
                     ResultSet.CONCUR_READ_ONLY
             )) {
            statement.setFetchSize(STREAMING_FETCH_SIZE);
            try (ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                PatentSearchDocument doc = mapRowToDoc(dataset, rs);
                batch.add(new IndexQueryBuilder().withId(doc.getId()).withObject(doc).build());
                if (batch.size() >= REINDEX_BATCH_SIZE) {
                    elasticsearchOperations.bulkIndex(batch, IndexCoordinates.of(indexName));
                    batch.clear();
                }
            }
            if (!batch.isEmpty()) {
                elasticsearchOperations.bulkIndex(batch, IndexCoordinates.of(indexName));
            }
            elasticsearchOperations.indexOps(IndexCoordinates.of(indexName)).refresh();
            }
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(BAD_REQUEST, "重建 ES 索引失败: " + ex.getMessage(), ex);
        }
    }

    public void indexOne(String category, String publicNum) {
        requireEnabled();
        if (publicNum == null || publicNum.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "publicNum不能为空");
        }
        AdminPatentDatasetDefinition dataset = adminRuntimeService.getEnabledDatasetDefinitions().stream()
                .filter(item -> category.equalsIgnoreCase(item.getCategory()) || category.equalsIgnoreCase(item.getCode()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "未找到启用的数据集分类: " + category));
        patentEsIndexManager.ensureIndexExists();
        String query = buildDatasetQuery(dataset) + " WHERE " + quote(dataset.getPublicNumColumn()) + " = '" + escapeSql(publicNum.trim()) + "'";
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(query)) {
            if (!rs.next()) {
                throw new ResponseStatusException(BAD_REQUEST, "未找到该专利: " + publicNum);
            }
            PatentSearchDocument doc = mapRowToDoc(dataset, rs);
            elasticsearchOperations.bulkIndex(List.of(new IndexQueryBuilder().withId(doc.getId()).withObject(doc).build()), IndexCoordinates.of(indexName));
            elasticsearchOperations.indexOps(IndexCoordinates.of(indexName)).refresh();
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(BAD_REQUEST, "单条索引失败: " + ex.getMessage(), ex);
        }
    }

    public Optional<PatentSearchDocument> findByPublicNum(String publicNum) {
        requireEnabled();
        if (publicNum == null || publicNum.isBlank()) {
            return Optional.empty();
        }
        try {
            var resp = elasticsearchClient.search(s -> s
                            .index(indexName)
                            .query(q -> q.term(t -> t.field("public_num").value(publicNum.trim())))
                            .size(1),
                    Map.class
            );
            if (resp.hits().hits().isEmpty()) {
                return Optional.empty();
            }
            Object srcObj = resp.hits().hits().get(0).source();
            if (!(srcObj instanceof Map<?, ?> src)) return Optional.empty();
            return Optional.of(mapToDoc(src));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public List<PatentSearchDocument> findByPublicNums(List<String> publicNums) {
        requireEnabled();
        if (publicNums == null || publicNums.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> validNums = publicNums.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
        if (validNums.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            Query q = Query.of(query -> query.bool(b -> {
                for (String num : validNums) {
                    b.should(s -> s.term(t -> t.field("public_num").value(num)));
                }
                b.minimumShouldMatch("1");
                return b;
            }));

            var resp = elasticsearchClient.search(s -> s
                            .index(indexName)
                            .query(q)
                            .size(validNums.size()),
                    Map.class
            );

            return resp.hits().hits().stream()
                    .map(h -> h.source())
                    .filter(Objects::nonNull)
                    .filter(src -> src instanceof Map<?, ?>)
                    .map(src -> mapToDoc((Map<?, ?>) src))
                    .toList();
        } catch (Exception e) {
            throw new ResponseStatusException(BAD_REQUEST, "ES批量查询失败: " + e.getMessage());
        }
    }

    public Optional<PatentSearchDocument> findByDatasetRecord(Long datasetId, String recordId) {
        requireEnabled();
        if (datasetId == null || recordId == null || recordId.isBlank()) {
            return Optional.empty();
        }
        try {
            Query q = Query.of(query -> query.bool(b -> b
                    .must(m -> m.term(t -> t.field("dataset_id").value(datasetId)))
                    .must(m -> m.term(t -> t.field("record_id").value(recordId.trim())))
            ));
            var resp = elasticsearchClient.search(s -> s
                            .index(indexName)
                            .query(q)
                            .size(1),
                    Map.class
            );
            if (resp.hits().hits().isEmpty()) {
                return Optional.empty();
            }
            Object srcObj = resp.hits().hits().get(0).source();
            if (!(srcObj instanceof Map<?, ?> src)) {
                return Optional.empty();
            }
            return Optional.of(mapToDoc(src));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public Optional<PatentSearchDocument> findDetail(Long datasetId, String recordIdOrPublicNum) {
        requireEnabled();
        if (datasetId == null || recordIdOrPublicNum == null || recordIdOrPublicNum.isBlank()) {
            return Optional.empty();
        }
        String normalized = recordIdOrPublicNum.trim();

        Optional<PatentSearchDocument> exact = findByDatasetRecord(datasetId, normalized);
        if (exact.isPresent()) {
            return exact;
        }

        try {
            PatentEsSearchResponse byDataset = searchByDataset(datasetId, normalized, 0, 10, true);
            Optional<PatentSearchDocument> datasetCandidate = byDataset.getHits().stream()
                    .filter(hit -> normalized.equalsIgnoreCase(firstNonBlank(hit.getRecordId(), hit.getPublicNum())))
                    .findFirst()
                    .map(this::toDocument);
            if (datasetCandidate.isPresent()) {
                return datasetCandidate;
            }
        } catch (Exception ignored) {
            // Fall back to a broader ES query below.
        }

        Optional<PatentSearchDocument> byPublicNum = findByPublicNum(normalized);
        if (byPublicNum.isPresent()) {
            return byPublicNum;
        }

        try {
            PatentEsSearchResponse global = search(null, normalized, 0, 10, true);
            return global.getHits().stream()
                    .filter(hit -> normalized.equalsIgnoreCase(firstNonBlank(hit.getRecordId(), hit.getPublicNum())))
                    .findFirst()
                    .map(this::toDocument);
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    public PatentEsStatusResponse status() {
        requireEnabled();
        try {
            boolean exists = elasticsearchClient.indices().exists(e -> e.index(indexName)).value();
            long count = exists ? elasticsearchClient.count(c -> c.index(indexName)).count() : 0;
            return new PatentEsStatusResponse(indexName, exists, count, adminRuntimeService.getEnabledDatasetDefinitions().size());
        } catch (Exception e) {
            throw new ResponseStatusException(BAD_REQUEST, "ES状态查询失败: " + e.getMessage());
        }
    }

    public PatentEsSearchResponse search(String category, String query, int page, int size, boolean phrase) {
        requireEnabled();
        patentEsIndexManager.ensureIndexExists();
        if (query == null || query.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "query不能为空");
        }
        if (page < 0) page = 0;
        if (size <= 0) size = 10;
        if (size > 50) size = 50;

        int finalPage = page;
        int finalSize = size;
        Query q = buildQuery(category, query.trim(), phrase);

        try {
            var resp = elasticsearchClient.search(s -> {
                        s.index(indexName);
                        s.from(finalPage * finalSize);
                        s.size(finalSize);
                        s.query(q);
                        s.highlight(h -> h
                                .preTags("<em>")
                                .postTags("</em>")
                                .fields("title", f -> f)
                                .fields("abstract", f -> f)
                                .fields("patent_details", f -> f)
                        );
                        return s;
                    },
                    Map.class
            );

            List<PatentEsSearchResponse.Hit> out = new ArrayList<>();
            for (var h : resp.hits().hits()) {
                Object srcObj = h.source();
                if (!(srcObj instanceof Map<?, ?> src)) continue;
                Map<String, List<String>> hl = h.highlight() == null ? Collections.emptyMap() : h.highlight();
                out.add(new PatentEsSearchResponse.Hit(
                        getLong(src, "dataset_id", "datasetId"),
                        firstNonBlank(getString(src, "dataset_name"), getString(src, "datasetName")),
                        firstNonBlank(getString(src, "record_id"), getString(src, "recordId")),
                        getString(src, "category"),
                        firstNonBlank(getString(src, "public_num"), getString(src, "publicNum")),
                        getString(src, "title"),
                        firstNonBlank(getString(src, "abstract"), getString(src, "abstractText")),
                        getString(src, "applicant"),
                        getString(src, "inventor"),
                        h.score(),
                        joinHighlight(hl.get("title")),
                        joinHighlight(hl.get("abstract")),
                        joinHighlight(hl.get("patent_details"))
                ));
            }

            long total = resp.hits().total() == null ? out.size() : resp.hits().total().value();
            return new PatentEsSearchResponse(total, out);
        } catch (Exception e) {
            throw new ResponseStatusException(BAD_REQUEST, "ES查询失败: " + e.getMessage());
        }
    }

    public PatentEsSearchResponse searchByDataset(Long datasetId, String query, int page, int size, boolean phrase) {
        requireEnabled();
        patentEsIndexManager.ensureIndexExists();
        if (datasetId == null) {
            throw new ResponseStatusException(BAD_REQUEST, "datasetId涓嶈兘涓虹┖");
        }
        if (query == null || query.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "query涓嶈兘涓虹┖");
        }
        if (page < 0) page = 0;
        if (size <= 0) size = 10;
        if (size > 50) size = 50;

        int finalPage = page;
        int finalSize = size;
        Query q = buildQuery(null, datasetId, query.trim(), phrase);

        try {
            var resp = elasticsearchClient.search(s -> {
                        s.index(indexName);
                        s.from(finalPage * finalSize);
                        s.size(finalSize);
                        s.query(q);
                        s.highlight(h -> h
                                .preTags("<em>")
                                .postTags("</em>")
                                .fields("title", f -> f)
                                .fields("abstract", f -> f)
                                .fields("patent_details", f -> f)
                        );
                        return s;
                    },
                    Map.class
            );

            List<PatentEsSearchResponse.Hit> out = new ArrayList<>();
            for (var h : resp.hits().hits()) {
                Object srcObj = h.source();
                if (!(srcObj instanceof Map<?, ?> src)) continue;
                Map<String, List<String>> hl = h.highlight() == null ? Collections.emptyMap() : h.highlight();
                out.add(new PatentEsSearchResponse.Hit(
                        getLong(src, "dataset_id", "datasetId"),
                        firstNonBlank(getString(src, "dataset_name"), getString(src, "datasetName")),
                        firstNonBlank(getString(src, "record_id"), getString(src, "recordId")),
                        getString(src, "category"),
                        firstNonBlank(getString(src, "public_num"), getString(src, "publicNum")),
                        getString(src, "title"),
                        firstNonBlank(getString(src, "abstract"), getString(src, "abstractText")),
                        getString(src, "applicant"),
                        getString(src, "inventor"),
                        h.score(),
                        joinHighlight(hl.get("title")),
                        joinHighlight(hl.get("abstract")),
                        joinHighlight(hl.get("patent_details"))
                ));
            }

            long total = resp.hits().total() == null ? out.size() : resp.hits().total().value();
            return new PatentEsSearchResponse(total, out);
        } catch (Exception e) {
            throw new ResponseStatusException(BAD_REQUEST, "ES鏌ヨ澶辫触: " + e.getMessage());
        }
    }

    public List<PatentSearchDocument> searchDocuments(String category, String query, int size) {
        PatentEsSearchResponse response = search(category, query, 0, size, false);
        return response.getHits().stream()
                .map(this::toDocument)
                .toList();
    }

    private PatentSearchDocument toDocument(PatentEsSearchResponse.Hit hit) {
        PatentSearchDocument doc = new PatentSearchDocument();
        doc.setDatasetId(hit.getDatasetId());
        doc.setDatasetName(hit.getDatasetName());
        doc.setRecordId(hit.getRecordId());
        doc.setCategory(hit.getCategory());
        doc.setPublicNum(hit.getPublicNum());
        doc.setTitle(hit.getTitle());
        doc.setAbstractText(hit.getAbstractText());
        doc.setApplicant(hit.getApplicant());
        doc.setInventor(hit.getInventor());
        return doc;
    }

    private Query buildQuery(String category, String query, boolean phrase) {
        List<String> fields = List.of(
                "public_num^6",
                "title^4",
                "abstract^2",
                "applicant^1",
                "inventor^1",
                "patent_details^1",
                "ipc^1",
                "cpc^1",
                "legal_status^1",
                "all_text"
        );

        Query base = Query.of(q -> q.multiMatch(m -> {
            m.query(query);
            m.fields(fields);
            m.operator(Operator.Or);
            if (phrase) {
                m.type(TextQueryType.Phrase);
                m.slop(2);
            }
            return m;
        }));
        if (category == null || category.isBlank()) {
            return base;
        }
        return Query.of(q -> q.bool(b -> b
                .must(base)
                .filter(f -> f.term(t -> t.field("category").value(category.toLowerCase(Locale.ROOT))))
        ));
    }

    private Query buildQuery(String category, Long datasetId, String query, boolean phrase) {
        Query base = buildQuery(category, query, phrase);
        if (datasetId == null) {
            return base;
        }
        return Query.of(q -> q.bool(b -> b
                .must(base)
                .filter(f -> f.term(t -> t.field("dataset_id").value(datasetId)))
        ));
    }

    private String buildDatasetQuery(AdminPatentDatasetDefinition dataset) {
        return "SELECT * FROM " + quote(dataset.getTableName()) +
                " ORDER BY " + quote(firstNonBlank(dataset.getDefaultSortColumn(), dataset.getPrimaryKeyColumn())) + " DESC";
    }

    private PatentSearchDocument mapRowToDoc(AdminPatentDatasetDefinition dataset, ResultSet rs) throws Exception {
        PatentSearchDocument doc = new PatentSearchDocument();
        String category = firstNonBlank(dataset.getCategory(), dataset.getCode()).toLowerCase(Locale.ROOT);
        String publicNum = firstNonBlank(
                getOptionalColumnValue(rs, dataset.getPublicNumColumn()),
                firstNonBlank(getOptionalColumnValue(rs, "public_no"),
                        firstNonBlank(getOptionalColumnValue(rs, "type"), getOptionalColumnValue(rs, "detail_id")))
        );
        doc.setDatasetId(dataset.getId());
        doc.setDatasetName(dataset.getName());
        doc.setRecordId(publicNum);
        doc.setCategory(category);
        doc.setPublicNum(publicNum);
        doc.setId(category + ":" + publicNum);
        doc.setTitle(getColumnValue(rs, dataset.getTitleColumn()));
        doc.setAbstractText(getColumnValue(rs, dataset.getAbstractColumn()));
        doc.setApplicant(getColumnValue(rs, dataset.getApplicantColumn()));
        doc.setInventor(getColumnValue(rs, dataset.getInventorColumn()));
        doc.setIpc(getColumnValue(rs, dataset.getIpcColumn()));
        doc.setCpc(getColumnValue(rs, dataset.getCpcColumn()));
        doc.setAppliDate(parseDate(rs.getObject(dataset.getAppliDateColumn())));
        doc.setPublicDate(parseDate(rs.getObject(dataset.getPublicDateColumn())));
        doc.setLegalStatus(getColumnValue(rs, dataset.getLegalStatusColumn()));
        doc.setStatus(getColumnValue(rs, dataset.getStatusColumn()));
        doc.setPatentDetails(buildPatentDetails(doc));
        doc.setAllText(buildAllText(doc));
        return doc;
    }

    private String buildPatentDetails(PatentSearchDocument doc) {
        return String.join(" ", List.of(
                nullSafe(doc.getTitle()),
                nullSafe(doc.getAbstractText()),
                nullSafe(doc.getApplicant()),
                nullSafe(doc.getInventor()),
                nullSafe(doc.getIpc()),
                nullSafe(doc.getCpc()),
                nullSafe(doc.getLegalStatus()),
                nullSafe(doc.getStatus())
        )).trim();
    }

    private String buildAllText(PatentSearchDocument doc) {
        return String.join(" ", List.of(
                nullSafe(doc.getPublicNum()),
                nullSafe(doc.getTitle()),
                nullSafe(doc.getAbstractText()),
                nullSafe(doc.getApplicant()),
                nullSafe(doc.getInventor()),
                nullSafe(doc.getPatentDetails())
        )).trim();
    }

    private static String getColumnValue(ResultSet rs, String column) throws Exception {
        if (column == null || column.isBlank()) {
            return null;
        }
        Object value = rs.getObject(column);
        return value == null ? null : String.valueOf(value);
    }

    private static String getOptionalColumnValue(ResultSet rs, String column) {
        try {
            return getColumnValue(rs, column);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String quote(String name) {
        return "`" + name.replace("`", "") + "`";
    }

    private static String escapeSql(String value) {
        return value.replace("'", "''");
    }

    private static String parseDate(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof java.sql.Date sqlDate) {
            return sqlDate.toLocalDate().toString();
        }
        if (value instanceof java.sql.Timestamp ts) {
            return ts.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().toString();
        }
        if (value instanceof LocalDate localDate) {
            return localDate.toString();
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            return null;
        }
        text = text.replace('.', '-').replace('/', '-');
        int dateTimeSeparator = text.indexOf(' ');
        if (dateTimeSeparator > 0) {
            text = text.substring(0, dateTimeSeparator);
        }
        try {
            return LocalDate.parse(text).toString();
        } catch (Exception ignored) {
            // Fall through and try a looser numeric parse.
        }
        String[] parts = text.split("-");
        if (parts.length == 3) {
            try {
                int year = Integer.parseInt(parts[0]);
                int month = Integer.parseInt(parts[1]);
                int day = Integer.parseInt(parts[2]);
                return LocalDate.of(year, month, day).toString();
            } catch (Exception ignored) {
                return null;
            }
        }
        return null;
    }

    private static PatentSearchDocument mapToDoc(Map<?, ?> src) {
        PatentSearchDocument doc = new PatentSearchDocument();
        doc.setId(firstNonBlank(getString(src, "id"), ""));
        doc.setDatasetId(getLong(src, "dataset_id", "datasetId"));
        doc.setDatasetName(firstNonBlank(getString(src, "dataset_name"), getString(src, "datasetName")));
        doc.setRecordId(firstNonBlank(getString(src, "record_id"), getString(src, "recordId")));
        doc.setCategory(getString(src, "category"));
        doc.setPublicNum(firstNonBlank(getString(src, "public_num"), getString(src, "publicNum")));
        doc.setTitle(getString(src, "title"));
        doc.setAbstractText(firstNonBlank(getString(src, "abstract"), getString(src, "abstractText")));
        doc.setApplicant(getString(src, "applicant"));
        doc.setInventor(getString(src, "inventor"));
        doc.setIpc(getString(src, "ipc"));
        doc.setCpc(getString(src, "cpc"));
        doc.setPatentDetails(getString(src, "patent_details"));
        doc.setAllText(getString(src, "all_text"));
        doc.setAppliDate(getString(src, "appli_date"));
        doc.setPublicDate(getString(src, "public_date"));
        doc.setLegalStatus(getString(src, "legal_status"));
        doc.setStatus(getString(src, "status"));
        return doc;
    }

    private static String getString(Map<?, ?> src, String key) {
        Object value = src.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private static Long getLong(Map<?, ?> src, String... keys) {
        for (String key : keys) {
            Object value = src.get(key);
            if (value instanceof Number number) {
                return number.longValue();
            }
            if (value instanceof String text && !text.isBlank()) {
                try {
                    return Long.parseLong(text.trim());
                } catch (NumberFormatException ignored) {
                    // Try next key.
                }
            }
        }
        return null;
    }

    private static String joinHighlight(List<String> items) {
        if (items == null || items.isEmpty()) {
            return null;
        }
        return String.join(" ... ", items);
    }

    private static String firstNonBlank(String first, String fallback) {
        return first != null && !first.isBlank() ? first : fallback;
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private void requireEnabled() {
        if (!enabled) {
            throw new ResponseStatusException(BAD_REQUEST, "ES 未启用");
        }
    }
}
