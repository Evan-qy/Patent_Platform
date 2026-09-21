package org.ihebut.patent.patent.mapper;

import org.ihebut.patent.patent.entity.ExpertProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ExpertProfileMapper extends JpaRepository<ExpertProfile, Long> {
    
    List<ExpertProfile> findByCertStatus(String certStatus);

    // Existing method (keeping it for compatibility if used elsewhere, though searchExperts covers it partially)
    List<ExpertProfile> findByCertStatusAndFieldContainingOrCertStatusAndExpertiseContaining(
            String certStatus1, String field,
            String certStatus2, String expertise
    );

    @Query("SELECT e FROM ExpertProfile e JOIN UserProfile p ON e.userId = p.userId " +
           "WHERE e.certStatus = :status AND " +
           "(p.realName LIKE %:query% OR e.field LIKE %:query% OR e.expertise LIKE %:query%)")
    List<ExpertProfile> searchExperts(@Param("status") String status, @Param("query") String query);
}
