package org.ihebut.patent.patent.mapper;

import org.ihebut.patent.patent.entity.PatentValuationReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PatentValuationReportMapper extends JpaRepository<PatentValuationReport, Long> {
    List<PatentValuationReport> findByPatentSource(String patentSource);

    List<PatentValuationReport> findByPatentSourceAndDatasetIdAndRecordId(
            String patentSource, Long datasetId, String recordId
    );

    List<PatentValuationReport> findByPatentSourceAndPatentCategoryAndPatentPublicNum(
            String patentSource, String patentCategory, String patentPublicNum
    );

    List<PatentValuationReport> findByPatentSourceAndUserPatentId(String patentSource, Long userPatentId);
}

