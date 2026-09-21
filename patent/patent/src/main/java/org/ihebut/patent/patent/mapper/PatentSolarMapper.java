package org.ihebut.patent.patent.mapper;

import org.ihebut.patent.patent.entity.PatentSolar;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 光伏专利数据访问层（Mapper）。
 */
public interface PatentSolarMapper extends JpaRepository<PatentSolar, String> {
    List<PatentSolar> findByTitleContainingOrAbstractTextContainingOrApplicantContainingOrInventorContaining(
            String title, String abstractText, String applicant, String inventor
    );

    Page<PatentSolar> findByTitleContainingOrAbstractTextContainingOrApplicantContainingOrInventorContaining(
            String title, String abstractText, String applicant, String inventor, Pageable pageable
    );

    List<PatentSolar> findByPublicNumContainingOrTitleContainingOrAbstractTextContainingOrApplicantContainingOrInventorContaining(
            String publicNum, String title, String abstractText, String applicant, String inventor
    );

    Page<PatentSolar> findByPublicNumContainingOrTitleContainingOrAbstractTextContainingOrApplicantContainingOrInventorContaining(
            String publicNum, String title, String abstractText, String applicant, String inventor, Pageable pageable
    );
}

