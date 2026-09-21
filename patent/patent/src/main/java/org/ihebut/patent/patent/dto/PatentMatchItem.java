package org.ihebut.patent.patent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 需求匹配到的专利摘要信息。
 */
@Data
@AllArgsConstructor
public class PatentMatchItem {
    private String category;
    private String publicNum;
    private String title;
    private String applicant;
    private String inventor;
}

