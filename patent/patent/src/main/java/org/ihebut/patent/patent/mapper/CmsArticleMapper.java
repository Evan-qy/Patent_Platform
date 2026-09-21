package org.ihebut.patent.patent.mapper;

import org.ihebut.patent.patent.entity.CmsArticle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CmsArticleMapper extends JpaRepository<CmsArticle, Long> {
    Optional<CmsArticle> findBySlug(String slug);
    List<CmsArticle> findAllByStatusOrderBySortOrderAscCreatedAtDesc(String status);
}
