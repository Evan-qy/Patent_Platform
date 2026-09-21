package org.ihebut.patent.patent.mapper;

import org.ihebut.patent.patent.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageMapper extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findBySessionIdOrderByCreatedAtAsc(Long sessionId);
    List<ChatMessage> findTop50BySessionIdOrderByCreatedAtDesc(Long sessionId);
    void deleteBySessionId(Long sessionId);
}

