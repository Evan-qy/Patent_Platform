package org.ihebut.patent.patent.mapper;

import org.ihebut.patent.patent.entity.UserNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserNotificationMapper extends JpaRepository<UserNotification, Long> {
    List<UserNotification> findByUserIdAndReadFlag(Long userId, Boolean readFlag);
    List<UserNotification> findByUserId(Long userId);
}

