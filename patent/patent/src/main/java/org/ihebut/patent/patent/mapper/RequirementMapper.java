package org.ihebut.patent.patent.mapper;

import org.ihebut.patent.patent.entity.Requirement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 需求数据访问层（Mapper）。
 *
 * <p>当前采用 Spring Data JPA 的接口方式实现。</p>
 */
public interface RequirementMapper extends JpaRepository<Requirement, Long> {
    /**
     * 按状态查询需求。
     *
     * @param status 状态
     * @return 需求列表
     */
    List<Requirement> findByStatus(String status);

    /**
     * 模糊查询需求（title 或 keywords 包含关键字）。
     *
     * @param title 标题关键字
     * @param keywords 关键词关键字
     * @param pageable 分页参数
     * @return 分页结果
     */
    Page<Requirement> findByTitleContainingOrKeywordsContaining(String title, String keywords, Pageable pageable);

    /**
     * 查询指定用户发布的需求（按创建时间倒序）。
     *
     * @param userId 用户ID
     * @param pageable 分页参数
     * @return 分页结果
     */
    Page<Requirement> findByRequesterUserIdOrderByCreatedDateDesc(Long userId, Pageable pageable);

    /**
     * 复合查询：支持可选的专利类别（技术方向）和关键词模糊查询。
     * <p>注：techDirection 字段用于存储技术方向，可对应专利类别。</p>
     */
    @Query("SELECT r FROM Requirement r WHERE " +
            "(:techDirection IS NULL OR r.techDirection = :techDirection) AND " +
            "(:query IS NULL OR r.title LIKE %:query% OR r.keywords LIKE %:query%) " +
            "ORDER BY r.createdDate DESC")
    Page<Requirement> search(@Param("techDirection") String techDirection, @Param("query") String query, Pageable pageable);

    /**
     * 查询所有需求并按ID升序排列。
     *
     * @return 需求列表
     */
    List<Requirement> findAllByOrderByIdAsc();
}
