package org.ihebut.patent.patent.mapper;

import org.ihebut.patent.patent.entity.UserOrganization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserOrganizationMapper extends JpaRepository<UserOrganization, Long> {
    Optional<UserOrganization> findByUserIdAndOrgId(Long userId, Long orgId);
    List<UserOrganization> findByUserId(Long userId);
    Optional<UserOrganization> findFirstByUserIdAndIsPrimaryTrue(Long userId);
}

