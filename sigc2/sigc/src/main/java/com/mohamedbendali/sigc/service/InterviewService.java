package com.mohamedbendali.sigc.service;

import com.mohamedbendali.sigc.dto.InterviewDTO;
import com.mohamedbendali.sigc.enums.InterviewStatus;
import java.util.List;

public interface InterviewService {
    InterviewDTO scheduleInterview(Long applicationId, InterviewDTO dto); // Peut-être juste l'ID application ?
    InterviewDTO startInterview(Long interviewId); // Démarrer la session chatbot
    InterviewDTO completeInterview(Long interviewId, Double score, String feedback); // Mettre à jour après l'entretien
    InterviewDTO cancelInterview(Long interviewId);
    InterviewDTO getInterviewById(Long id);
    List<InterviewDTO> getInterviewsByApplicationId(Long applicationId);
    // ... autres méthodes
}