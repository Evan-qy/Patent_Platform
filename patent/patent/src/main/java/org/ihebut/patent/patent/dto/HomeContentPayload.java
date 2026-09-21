package org.ihebut.patent.patent.dto;

import lombok.Data;
import java.util.List;

@Data
public class HomeContentPayload {
    private String heroTitle;
    private String brandSlogan;
    private String heroDescription;
    private String coreFeaturesTitle;
    private String coreFeaturesIntro;
    private List<MetricItem> heroMetrics;
    private List<FeatureItem> features;
    private List<AdvantageItem> advantages;
    private List<String> mobileOperationTips;
    private List<ServicePromiseItem> mobileServicePromises;
    private List<String> heroHighlights;
    private List<KpiItem> caseKpis;
    private String articlesSectionTitle;
    private String articlesSectionSubtitle;

    @Data
    public static class MetricItem {
        private String value;
        private String label;
        private String detail;
    }

    @Data
    public static class FeatureItem {
        private String icon;
        private String title;
        private String description;
        private String detail;
        private String moreLabel;
        private String to;
    }

    @Data
    public static class AdvantageItem {
        private String icon;
        private String title;
        private String description;
    }

    @Data
    public static class ServicePromiseItem {
        private String icon;
        private String title;
        private String text;
    }

    @Data
    public static class KpiItem {
        private String label;
        private String value;
    }
}
