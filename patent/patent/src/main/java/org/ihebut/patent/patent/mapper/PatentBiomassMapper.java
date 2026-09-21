package org.ihebut.patent.patent.mapper;

import org.ihebut.patent.patent.entity.PatentBiomass;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 生物质专利数据访问层（Mapper）。
 */
public interface PatentBiomassMapper extends JpaRepository<PatentBiomass, String> {
    List<PatentBiomass> findByTitleContainingOrAbstractTextContainingOrApplicantContainingOrInventorContaining(
            String title, String abstractText, String applicant, String inventor
    );

    Page<PatentBiomass> findByTitleContainingOrAbstractTextContainingOrApplicantContainingOrInventorContaining(
            String title, String abstractText, String applicant, String inventor, Pageable pageable
    );

    List<PatentBiomass> findByPublicNumContainingOrTitleContainingOrAbstractTextContainingOrApplicantContainingOrInventorContaining(
            String publicNum, String title, String abstractText, String applicant, String inventor
    );

    Page<PatentBiomass> findByPublicNumContainingOrTitleContainingOrAbstractTextContainingOrApplicantContainingOrInventorContaining(
            String publicNum, String title, String abstractText, String applicant, String inventor, Pageable pageable
    );
}

