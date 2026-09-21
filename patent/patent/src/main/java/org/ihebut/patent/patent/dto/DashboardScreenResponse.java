package org.ihebut.patent.patent.dto;

import java.math.BigDecimal;
import java.util.List;

public record DashboardScreenResponse(
        String generatedAt,
        DashboardMetricSummary metrics,
        List<DashboardPatentCategoryItem> patentCategories,
        List<DashboardTrendItem> patentTrend,
        List<DashboardDatasetRankingItem> datasetRanking,
        List<DashboardExpertFieldItem> expertFields,
        List<DashboardTransformationItem> latestTransformations
) {
    public record DashboardMetricSummary(
            long patentTotal,
            BigDecimal transformationBenefitTotal,
            long expertTotal,
            long enterpriseTotal
    ) {
    }

    public record DashboardPatentCategoryItem(
            String category,
            String label,
            long count
    ) {
    }

    public record DashboardTrendItem(
            String month,
            long patentCount,
            long transformationCount
    ) {
    }

    public record DashboardDatasetRankingItem(
            long datasetId,
            String datasetCode,
            String datasetName,
            long count
    ) {
    }

    public record DashboardExpertFieldItem(
            String name,
            long count
    ) {
    }

    public record DashboardTransformationItem(
            long id,
            String title,
            BigDecimal benefitAmount,
            String transformationDate
    ) {
    }
}
