package org.ihebut.patent.patent.mapper;

import org.ihebut.patent.patent.entity.RequirementPatentMatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RequirementPatentMatchMapper extends JpaRepository<RequirementPatentMatch, Long> {
    List<RequirementPatentMatch> findByRequirementId(Long requirementId);

    Optional<RequirementPatentMatch> findByRequirementIdAndPatentSourceAndPatentCategoryAndPatentPublicNum(
            Long requirementId, String patentSource, String patentCategory, String patentPublicNum
    );

    void deleteByRequirementId(Long requirementId);
}

