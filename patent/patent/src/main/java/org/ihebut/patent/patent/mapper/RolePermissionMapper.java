package org.ihebut.patent.patent.mapper;

import org.ihebut.patent.patent.entity.RolePermission;
import org.ihebut.patent.patent.entity.RolePermissionId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RolePermissionMapper extends JpaRepository<RolePermission, RolePermissionId> {
    List<RolePermission> findByRoleId(Long roleId);
}

