package org.ihebut.patent.patent.dto;

import lombok.Data;
import java.util.List;

@Data
public class RequirementPatentMatchPersistRequest {
    private List<Item> items;

    @Data
    public static class Item {
        private String patentCategory;
        private String patentPublicNum;
        private Double matchScore;
        private String matchReason;
    }
}
