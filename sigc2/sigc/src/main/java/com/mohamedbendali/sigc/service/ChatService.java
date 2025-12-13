package com.mohamedbendali.sigc.service;

import com.mohamedbendali.sigc.dto.ChatMessageDTO;
import com.mohamedbendali.sigc.entity.Interview;

import java.util.List;

public interface ChatService {
    // Le candidat envoie un message, reçoit une réponse du bot
    ChatMessageDTO processCandidateMessage(Long interviewId, String messageContent);

    // Sauvegarder un message (peut être interne)
    ChatMessageDTO saveMessage(ChatMessageDTO dto);

    // Obtenir l'historique d'un entretien
    List<ChatMessageDTO> getChatHistory(Long interviewId);

    // Initier la conversation (premier message du bot)
    ChatMessageDTO initiateChat(Long interviewId);
    Interview evaluateInterviewWithGemini(Long interviewId);

}