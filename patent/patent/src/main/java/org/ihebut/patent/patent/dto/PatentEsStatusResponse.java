package org.ihebut.patent.patent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PatentEsStatusResponse {
    private String indexName;
    private boolean exists;
    private long docCount;
    private int enabledDatasetCount;
}

