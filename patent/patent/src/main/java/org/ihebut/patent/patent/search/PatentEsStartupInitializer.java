package org.ihebut.patent.patent.search;

import org.ihebut.patent.patent.service.PatentEsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "search.es.enabled", havingValue = "true")
public class PatentEsStartupInitializer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(PatentEsStartupInitializer.class);
    private final boolean autoCreateIndex;
    private final boolean autoReindex;
    private final PatentEsIndexManager patentEsIndexManager;
    private final PatentEsService patentEsService;

    public PatentEsStartupInitializer(
            @Value("${search.es.auto-create-index:true}") boolean autoCreateIndex,
            @Value("${search.es.auto-reindex:false}") boolean autoReindex,
            PatentEsIndexManager patentEsIndexManager,
            PatentEsService patentEsService
    ) {
        this.autoCreateIndex = autoCreateIndex;
        this.autoReindex = autoReindex;
        this.patentEsIndexManager = patentEsIndexManager;
        this.patentEsService = patentEsService;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("ES启动初始化 autoCreateIndex={} autoReindex={}", autoCreateIndex, autoReindex);
        if (autoCreateIndex) {
            patentEsIndexManager.ensureIndexExists();
        }
        if (autoReindex) {
            patentEsService.reindexAll();
        }
    }
}
