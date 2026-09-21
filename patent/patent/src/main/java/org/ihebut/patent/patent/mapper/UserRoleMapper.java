package org.ihebut.patent.patent.mapper;

import org.ihebut.patent.patent.entity.UserRole;
import org.ihebut.patent.patent.entity.UserRoleId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRoleMapper extends JpaRepository<UserRole, UserRoleId> {
    List<UserRole> findByUserId(Long userId);
}

