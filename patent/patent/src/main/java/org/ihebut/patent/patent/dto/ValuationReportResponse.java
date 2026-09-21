package org.ihebut.patent.patent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 用于前端展示的智能评估报告返回结构。
 *
 * 前端期望字段包含：dimensionScores、conclusion、predictedValue、valuationDate 等。
 * 其中部分字段不落库，统一从生成时的启发式计算/报告 JSON 还原。
 */
@Data
public class ValuationReportResponse {
    private Long id;
    private String patentSource;

    private Long datasetId;
    private String recordId;
    private Long userPatentId;

    private String patentCategory;
    private String patentPublicNum;

    private String reportTitle;

    // 前端使用：predictedValue（万元展示）
    private Double predictedValue;
    private String currency = "CNY";
    private String valuationDate;
    private String modelVersion;

    // 综合评分（0~100）
    private Double comprehensiveScore;

    private DimensionScores dimensionScores;

    private Conclusion conclusion;

    private Map<String, Integer> weights;

    private PatentInfo patentInfo;

    @Data
    public static class DimensionScores {
        private DimensionScore technologicalInnovation;
        private DimensionScore marketPotential;
        private DimensionScore legalStatus;
        private DimensionScore economicValue;
    }

    @Data
    public static class DimensionScore {
        // 0~100
        private Double score;
        private Double fullScore = 100d;
        private String analysis;
        private List<String> keyPoints;
    }

    @Data
    public static class Conclusion {
        private String summary;
        private List<String> strengths;
        private List<String> risks;
        private List<String> recommendations;
        private String suggestion;
    }

    @Data
    public static class PatentInfo {
        private String publicNum;
        private String title;
        private String applicant;
        private String inventor;
        @JsonProperty("abstract")
        private String abstractText;
        private String ipcClass;
    }
}

