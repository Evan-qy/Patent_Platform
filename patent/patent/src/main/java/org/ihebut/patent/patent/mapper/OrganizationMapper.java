package org.ihebut.patent.patent.mapper;

import org.ihebut.patent.patent.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrganizationMapper extends JpaRepository<Organization, Long> {
    List<Organization> findByNameContaining(String query);
    List<Organization> findByTypeAndNameContaining(String type, String query);
    List<Organization> findByType(String type);
}

