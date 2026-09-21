package org.ihebut.patent.patent.dto;

import lombok.Data;
import java.util.List;

@Data
public class RequirementExpertMatchPersistRequest {
    private List<Item> items;

    @Data
    public static class Item {
        private Long expertId;
        private Double matchScore;
        private String matchReason;
    }
}
