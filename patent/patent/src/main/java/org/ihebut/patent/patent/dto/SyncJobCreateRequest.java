package org.ihebut.patent.patent.dto;

import lombok.Data;

@Data
public class SyncJobCreateRequest {
    private String jobType;
    private String targetCategory;
    private String source;
}

