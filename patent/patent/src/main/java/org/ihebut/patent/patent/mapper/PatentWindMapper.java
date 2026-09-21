package org.ihebut.patent.patent.mapper;

import org.ihebut.patent.patent.entity.PatentWind;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 风能专利数据访问层（Mapper）。
 */
public interface PatentWindMapper extends JpaRepository<PatentWind, String> {
    List<PatentWind> findByTitleContainingOrAbstractTextContainingOrApplicantContainingOrInventorContaining(
            String title, String abstractText, String applicant, String inventor
    );

    Page<PatentWind> findByTitleContainingOrAbstractTextContainingOrApplicantContainingOrInventorContaining(
            String title, String abstractText, String applicant, String inventor, Pageable pageable
    );

    List<PatentWind> findByPublicNumContainingOrTitleContainingOrAbstractTextContainingOrApplicantContainingOrInventorContaining(
            String publicNum, String title, String abstractText, String applicant, String inventor
    );

    Page<PatentWind> findByPublicNumContainingOrTitleContainingOrAbstractTextContainingOrApplicantContainingOrInventorContaining(
            String publicNum, String title, String abstractText, String applicant, String inventor, Pageable pageable
    );
}

