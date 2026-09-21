package org.ihebut.patent.patent.mapper;

import org.ihebut.patent.patent.entity.PatentLilon;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 锂电专利数据访问层（Mapper）。
 */
public interface PatentLilonMapper extends JpaRepository<PatentLilon, String> {
    List<PatentLilon> findByTitleContainingOrAbstractTextContainingOrApplicantContainingOrInventorContaining(
            String title, String abstractText, String applicant, String inventor
    );

    Page<PatentLilon> findByTitleContainingOrAbstractTextContainingOrApplicantContainingOrInventorContaining(
            String title, String abstractText, String applicant, String inventor, Pageable pageable
    );

    List<PatentLilon> findByPublicNumContainingOrTitleContainingOrAbstractTextContainingOrApplicantContainingOrInventorContaining(
            String publicNum, String title, String abstractText, String applicant, String inventor
    );

    Page<PatentLilon> findByPublicNumContainingOrTitleContainingOrAbstractTextContainingOrApplicantContainingOrInventorContaining(
            String publicNum, String title, String abstractText, String applicant, String inventor, Pageable pageable
    );
}

