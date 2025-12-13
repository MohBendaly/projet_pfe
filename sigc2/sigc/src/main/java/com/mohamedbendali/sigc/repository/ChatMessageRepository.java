package com.mohamedbendali.sigc.repository;

import com.mohamedbendali.sigc.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // Trouver tous les messages pour un entretien, triés par timestamp
    List<ChatMessage> findByInterviewIdOrderByTimestampAsc(Long interviewId);
}