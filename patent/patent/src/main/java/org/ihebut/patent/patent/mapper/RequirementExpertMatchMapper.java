package org.ihebut.patent.patent.mapper;

import org.ihebut.patent.patent.entity.RequirementExpertMatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RequirementExpertMatchMapper extends JpaRepository<RequirementExpertMatch, Long> {
    List<RequirementExpertMatch> findByRequirementId(Long requirementId);

    void deleteByRequirementId(Long requirementId);
}

