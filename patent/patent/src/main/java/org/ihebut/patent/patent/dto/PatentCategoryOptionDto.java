package org.ihebut.patent.patent.dto;

import java.util.List;

/**
 * 与前台 {@code PatentCategoryOption} 对齐：分类编码、展示名、关联数据集 ID。
 */
public record PatentCategoryOptionDto(String value, String label, List<Long> datasetIds) {
}
