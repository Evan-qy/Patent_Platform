package org.ihebut.patent.patent.mapper;

import org.ihebut.patent.patent.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserAccountMapper extends JpaRepository<UserAccount, Long> {
    boolean existsByUsername(String username);
    Optional<UserAccount> findByUsername(String username);
    Optional<UserAccount> findByEmail(String email);
    Optional<UserAccount> findByPhone(String phone);
}
