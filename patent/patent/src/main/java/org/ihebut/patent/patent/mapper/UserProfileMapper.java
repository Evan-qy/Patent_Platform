package org.ihebut.patent.patent.mapper;

import org.ihebut.patent.patent.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProfileMapper extends JpaRepository<UserProfile, Long> {
}

