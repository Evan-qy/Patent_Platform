package org.ihebut.patent.patent.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import org.ihebut.patent.patent.search.PatentEsIndexManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

import javax.sql.DataSource;
import java.util.function.Function;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PatentEsServiceTest {

    @Mock private ElasticsearchOperations elasticsearchOperations;
    @Mock private ElasticsearchClient elasticsearchClient;
    @Mock private PatentEsIndexManager patentEsIndexManager;
    @Mock private AdminRuntimeService adminRuntimeService;
    @Mock private DataSource dataSource;
    @Mock private co.elastic.clients.elasticsearch.core.SearchResponse searchResponse;
    @Mock private co.elastic.clients.elasticsearch.core.search.HitsMetadata hitsMetadata;

    private PatentEsService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new PatentEsService(
                true, "patents",
                elasticsearchOperations, elasticsearchClient, patentEsIndexManager,
                adminRuntimeService, dataSource
        );
    }

    @Test
    void search_ShouldInvokeElasticsearchClient() throws Exception {
        when(elasticsearchClient.search(any(Function.class), any(Class.class)))
                .thenReturn(searchResponse);
        when(searchResponse.hits()).thenReturn(hitsMetadata);
        when(hitsMetadata.hits()).thenReturn(java.util.Collections.emptyList());

        service.search("lilon", "test", 0, 10, false);

        verify(elasticsearchClient).search(any(Function.class), any(Class.class));
    }
}
