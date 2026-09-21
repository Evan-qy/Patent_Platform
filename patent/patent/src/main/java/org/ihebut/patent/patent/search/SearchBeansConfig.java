package org.ihebut.patent.patent.search;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SearchBeansConfig {
    @Bean(name = "patentIndexName")
    public String patentIndexName(@Value("${search.es.index.patent:patents}") String indexName) {
        return indexName;
    }
}

