package org.ihebut.patent.patent.dto;

import lombok.Data;

@Data
public class ValuationCreateRequest {
    private String patentSource;
    private String patentCategory;
    private String patentPublicNum;
    private Long userPatentId;
    // 前端生成评估时会传 datasetId + recordId，用于在报告中保留定位信息
    private Long datasetId;
    private String recordId;
    private String modelVersion;

    private ValuationWeights weights;

    @Data
    public static class ValuationWeights {
        private Integer technologicalInnovation;
        private Integer marketPotential;
        private Integer legalStatus;
        private Integer economicValue;
    }
}

