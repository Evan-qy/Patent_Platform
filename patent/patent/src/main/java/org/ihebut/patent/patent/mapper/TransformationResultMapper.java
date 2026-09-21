package org.ihebut.patent.patent.mapper;

import org.ihebut.patent.patent.entity.TransformationResult;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 转化成果数据访问层（Mapper）。
 *
 * <p>当前采用 Spring Data JPA 的接口方式实现。</p>
 */
public interface TransformationResultMapper extends JpaRepository<TransformationResult, Long> {
}
