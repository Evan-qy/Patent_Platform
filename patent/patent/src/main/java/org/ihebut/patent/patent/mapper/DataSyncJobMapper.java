package org.ihebut.patent.patent.mapper;

import org.ihebut.patent.patent.entity.DataSyncJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DataSyncJobMapper extends JpaRepository<DataSyncJob, Long> {
    List<DataSyncJob> findByCreatedBy(Long createdBy);
}

