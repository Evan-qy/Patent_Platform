package org.ihebut.patent.patent.mapper;

import org.ihebut.patent.patent.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PermissionMapper extends JpaRepository<Permission, Long> {
    Optional<Permission> findByCode(String code);
}

