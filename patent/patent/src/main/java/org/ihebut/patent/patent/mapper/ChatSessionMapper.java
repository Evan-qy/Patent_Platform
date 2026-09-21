package org.ihebut.patent.patent.mapper;

import org.ihebut.patent.patent.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatSessionMapper extends JpaRepository<ChatSession, Long> {
    List<ChatSession> findByUserIdOrderByUpdatedAtDesc(Long userId);
    Optional<ChatSession> findByIdAndUserId(Long id, Long userId);
}

