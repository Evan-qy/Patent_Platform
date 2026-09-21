package org.ihebut.patent.patent.mapper;

import org.ihebut.patent.patent.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditLogMapper extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {
    List<AuditLog> findByUserId(Long userId);
    List<AuditLog> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to);
}
