package org.ihebut.patent.patent.mapper;

import org.ihebut.patent.patent.entity.UserPatent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserPatentMapper extends JpaRepository<UserPatent, Long> {
    List<UserPatent> findByVisibility(String visibility);
    List<UserPatent> findByOwnerUserId(Long ownerUserId);
    List<UserPatent> findByCategoryAndVisibility(String category, String visibility);
    List<UserPatent> findByCategoryAndOwnerUserId(String category, Long ownerUserId);
}

