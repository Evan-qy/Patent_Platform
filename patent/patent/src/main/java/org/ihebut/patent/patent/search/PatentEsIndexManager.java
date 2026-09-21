package org.ihebut.patent.patent.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.DeleteIndexRequest;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

@Component
public class PatentEsIndexManager {
    private static final Logger log = LoggerFactory.getLogger(PatentEsIndexManager.class);
    private final ElasticsearchClient client;
    private final String indexName;
    private final boolean preferIk;

    public PatentEsIndexManager(
            ElasticsearchClient client,
            @Value("${search.es.index.patent:patents}") String indexName,
            @Value("${search.es.prefer-ik:false}") boolean preferIk
    ) {
        this.client = client;
        this.indexName = indexName;
        this.preferIk = preferIk;
    }

    public void recreateIndex() {
        try {
            if (exists()) {
                log.info("ES索引已存在，准备删除 index={}", indexName);
                client.indices().delete(DeleteIndexRequest.of(d -> d.index(indexName)));
            }
            log.info("ES索引创建开始 index={} preferIk={}", indexName, preferIk);
            createIndex();
            log.info("ES索引创建完成 index={}", indexName);
        } catch (Exception e) {
            throw new IllegalStateException("创建ES索引失败：" + e.getMessage());
        }
    }

    public void ensureIndexExists() {
        try {
            if (exists()) return;
            log.info("ES索引不存在，自动创建 index={} preferIk={}", indexName, preferIk);
            createIndex();
            log.info("ES索引创建完成 index={}", indexName);
        } catch (Exception e) {
            throw new IllegalStateException("创建ES索引失败：" + e.getMessage());
        }
    }

    private void createIndex() throws Exception {
        if (preferIk) {
            try {
                createWithAnalyzer("ik_max_word", "ik_smart");
                return;
            } catch (Exception ignored) {
                log.warn("IK分词不可用，降级使用standard analyzer index={}", indexName);
            }
        }
        createWithAnalyzer("standard", "standard");
    }

    private boolean exists() throws Exception {
        return client.indices().exists(ExistsRequest.of(e -> e.index(indexName))).value();
    }

    private void createWithAnalyzer(String analyzer, String searchAnalyzer) throws Exception {
        Map<String, Object> properties = new HashMap<>();
        properties.put("category", Map.of("type", "keyword"));
        properties.put("public_num", Map.of("type", "keyword"));
        properties.put("title", textWithCopyTo(analyzer, searchAnalyzer, "all_text", 512));
        properties.put("abstract", textWithCopyTo(analyzer, searchAnalyzer, "all_text", 2048));
        properties.put("applicant", textWithCopyTo(analyzer, searchAnalyzer, "all_text", 512));
        properties.put("inventor", textWithCopyTo(analyzer, searchAnalyzer, "all_text", 512));
        properties.put("ipc", textWithCopyTo(analyzer, searchAnalyzer, "all_text", 256));
        properties.put("cpc", textWithCopyTo(analyzer, searchAnalyzer, "all_text", 256));
        properties.put("nec", textWithCopyTo(analyzer, searchAnalyzer, "all_text", 256));
        properties.put("patent_details", textWithCopyTo(analyzer, searchAnalyzer, "all_text", 8192));
        properties.put("all_text", Map.of("type", "text", "analyzer", analyzer, "search_analyzer", searchAnalyzer));

        Map<String, Object> mappings = Map.of("properties", properties);

        CreateIndexRequest req = CreateIndexRequest.of(c -> c
                .index(indexName)
                .mappings(m -> m.withJson(new StringReader(JsonUtil.toJson(mappings))))
        );
        client.indices().create(req);
    }

    private static Map<String, Object> textWithCopyTo(String analyzer, String searchAnalyzer, String copyTo, int ignoreAbove) {
        return Map.of(
                "type", "text",
                "analyzer", analyzer,
                "search_analyzer", searchAnalyzer,
                "copy_to", copyTo,
                "fields", Map.of(
                        "keyword", Map.of("type", "keyword", "ignore_above", ignoreAbove)
                )
        );
    }
}
