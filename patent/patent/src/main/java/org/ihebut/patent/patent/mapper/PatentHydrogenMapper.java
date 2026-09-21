package org.ihebut.patent.patent.mapper;

import org.ihebut.patent.patent.entity.PatentHydrogen;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 氢能专利数据访问层（Mapper）。
 */
public interface PatentHydrogenMapper extends JpaRepository<PatentHydrogen, String> {
    List<PatentHydrogen> findByTitleContainingOrAbstractTextContainingOrApplicantContainingOrInventorContaining(
            String title, String abstractText, String applicant, String inventor
    );

    Page<PatentHydrogen> findByTitleContainingOrAbstractTextContainingOrApplicantContainingOrInventorContaining(
            String title, String abstractText, String applicant, String inventor, Pageable pageable
    );

    List<PatentHydrogen> findByPublicNumContainingOrTitleContainingOrAbstractTextContainingOrApplicantContainingOrInventorContaining(
            String publicNum, String title, String abstractText, String applicant, String inventor
    );

    Page<PatentHydrogen> findByPublicNumContainingOrTitleContainingOrAbstractTextContainingOrApplicantContainingOrInventorContaining(
            String publicNum, String title, String abstractText, String applicant, String inventor, Pageable pageable
    );
}

