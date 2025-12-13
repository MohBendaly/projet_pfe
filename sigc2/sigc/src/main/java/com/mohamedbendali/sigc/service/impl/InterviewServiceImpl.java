package com.mohamedbendali.sigc.service.impl;

import com.mohamedbendali.sigc.dto.InterviewDTO;
import com.mohamedbendali.sigc.dto.ChatMessageDTO; // Nécessaire pour le mapping
import com.mohamedbendali.sigc.entity.ChatMessage;
import com.mohamedbendali.sigc.entity.Interview;
import com.mohamedbendali.sigc.entity.JobApplication;
import com.mohamedbendali.sigc.enums.ApplicationStatus;
import com.mohamedbendali.sigc.enums.InterviewStatus;
import com.mohamedbendali.sigc.exception.ResourceNotFoundException;
import com.mohamedbendali.sigc.exception.OperationNotAllowedException;
import com.mohamedbendali.sigc.repository.InterviewRepository;
import com.mohamedbendali.sigc.repository.JobApplicationRepository;
import com.mohamedbendali.sigc.service.InterviewService;
import com.mohamedbendali.sigc.service.ChatService; // Peut être injecté pour initier le chat
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class InterviewServiceImpl implements InterviewService {

    private final InterviewRepository interviewRepository;
    private final JobApplicationRepository applicationRepository;
    // private final ChatService chatService; // Optionnel: pour initier le chat lors du start

    @Override
    public InterviewDTO scheduleInterview(Long applicationId, InterviewDTO dto) {
        log.debug("Scheduling interview for application ID: {}", applicationId);
        JobApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("JobApplication", "id", applicationId));

        // Vérifier si l'application est dans un statut qui permet de planifier un entretien
        if (application.getStatus() != ApplicationStatus.UNDER_REVIEW && application.getStatus() != ApplicationStatus.RECEIVED /* autres? */) {
            log.warn("Attempted to schedule interview for application ID {} with status {}", applicationId, application.getStatus());
            throw new OperationNotAllowedException("Cannot schedule interview for application with status " + application.getStatus());
        }

        Interview interview = new Interview();
        interview.setApplication(application);
        interview.setStartTime(dto.getStartTime() != null ? dto.getStartTime() : LocalDateTime.now()); // Ou forcer une date?
        interview.setStatus(InterviewStatus.SCHEDULED);
        // interview.setEndTime(dto.getEndTime()); // L'heure de fin n'est pas connue à la planification

        Interview savedInterview = interviewRepository.save(interview);

        // Mettre à jour le statut de la candidature
        application.setStatus(ApplicationStatus.INTERVIEW_SCHEDULED);
        applicationRepository.save(application);

        log.info("Interview scheduled successfully with ID: {} for application ID: {}", savedInterview.getId(), applicationId);
        return convertToDto(savedInterview);
    }

    @Override
    public InterviewDTO startInterview(Long interviewId) {
        log.debug("Starting interview ID: {}", interviewId);
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview", "id", interviewId));

        if (interview.getStatus() != InterviewStatus.SCHEDULED) {
            log.warn("Attempted to start an interview (ID: {}) not in SCHEDULED status (current: {})", interviewId, interview.getStatus());
            throw new OperationNotAllowedException("Interview can only be started if it is scheduled.");
        }

        interview.setStatus(InterviewStatus.IN_PROGRESS);
        interview.setStartTime(LocalDateTime.now()); // Mettre à jour l'heure de début réelle
        Interview updatedInterview = interviewRepository.save(interview);

        // Optionnel: Initier le chat avec le premier message du bot
        // chatService.initiateChat(updatedInterview.getId());

        log.info("Interview started successfully for ID: {}", updatedInterview.getId());
        return convertToDto(updatedInterview);
    }

    @Override
    public InterviewDTO completeInterview(Long interviewId, Double score, String feedback) {
        log.debug("Completing interview ID: {} with score: {}", interviewId, score);
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview", "id", interviewId));

        if (interview.getStatus() != InterviewStatus.IN_PROGRESS && interview.getStatus() != InterviewStatus.PENDING_FEEDBACK) {
            log.warn("Attempted to complete an interview (ID: {}) not in IN_PROGRESS or PENDING_FEEDBACK status (current: {})", interviewId, interview.getStatus());
            throw new OperationNotAllowedException("Interview cannot be completed from status " + interview.getStatus());
        }

        interview.setStatus(InterviewStatus.COMPLETED);
        interview.setEndTime(LocalDateTime.now());
        interview.setAiEvaluationScore(score);
        interview.setAiFeedback(feedback);
        Interview updatedInterview = interviewRepository.save(interview);

        // Mettre à jour le statut de la candidature ? Ex: INTERVIEW_COMPLETED
        JobApplication application = interview.getApplication();
        if (application != null) {
            application.setStatus(ApplicationStatus.INTERVIEW_COMPLETED); // Ou autre statut post-entretien
            applicationRepository.save(application);
        }

        log.info("Interview completed successfully for ID: {}", updatedInterview.getId());
        return convertToDto(updatedInterview);
    }

    @Override
    public InterviewDTO cancelInterview(Long interviewId) {
        log.debug("Cancelling interview ID: {}", interviewId);
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview", "id", interviewId));

        // On peut annuler un entretien planifié ou en cours ?
        if (interview.getStatus() == InterviewStatus.COMPLETED) {
            log.warn("Attempted to cancel a completed interview (ID: {})", interviewId);
            throw new OperationNotAllowedException("Cannot cancel a completed interview.");
        }

        interview.setStatus(InterviewStatus.CANCELLED);
        interview.setEndTime(LocalDateTime.now()); // Marquer l'heure d'annulation
        Interview updatedInterview = interviewRepository.save(interview);

        // Revenir au statut précédent de la candidature ? Ex: UNDER_REVIEW
        JobApplication application = interview.getApplication();
        if (application != null && application.getStatus() == ApplicationStatus.INTERVIEW_SCHEDULED) {
            application.setStatus(ApplicationStatus.UNDER_REVIEW); // Revenir en arrière ? Logique à définir
            applicationRepository.save(application);
        }

        log.info("Interview cancelled successfully for ID: {}", updatedInterview.getId());
        return convertToDto(updatedInterview);
    }

    @Override
    @Transactional(readOnly = true)
    public InterviewDTO getInterviewById(Long id) {
        log.debug("Fetching interview by ID: {}", id);
        Interview interview = interviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Interview", "id", id));
        // Peut-être charger les messages ici ? Ou le laisser au DTO/Controller ?
        return convertToDto(interview);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InterviewDTO> getInterviewsByApplicationId(Long applicationId) {
        log.debug("Fetching interviews for application ID: {}", applicationId);
        List<Interview> interviews = interviewRepository.findByApplicationId(applicationId);
        return interviews.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    // --- Méthodes de Mapping ---

    private InterviewDTO convertToDto(Interview interview) {
        if (interview == null) return null;
        InterviewDTO dto = new InterviewDTO();
        dto.setId(interview.getId());
        dto.setStartTime(interview.getStartTime());
        dto.setEndTime(interview.getEndTime());
        dto.setStatus(interview.getStatus());
        dto.setAiEvaluationScore(interview.getAiEvaluationScore());
        dto.setAiFeedback(interview.getAiFeedback());
        dto.setCreatedAt(interview.getCreatedAt()); // Assurez-vous que ce champ existe et est mappé

        if (interview.getApplication() != null) {
            dto.setApplicationId(interview.getApplication().getId());
        }

        // Mapper les messages si nécessaire (peut être coûteux, optionnel)
        // if (interview.getChatMessages() != null && !interview.getChatMessages().isEmpty()) {
        //     dto.setChatMessages(interview.getChatMessages().stream()
        //         .map(this::convertChatMessageToDto) // Nécessite une méthode de conversion pour ChatMessage
        //         .collect(Collectors.toList()));
        // } else {
        //     dto.setChatMessages(Collections.emptyList());
        // }

        return dto;
    }

    // Méthode de conversion pour ChatMessage (si nécessaire ci-dessus)
    private ChatMessageDTO convertChatMessageToDto(ChatMessage message) {
        if (message == null) return null;
        ChatMessageDTO dto = new ChatMessageDTO();
        dto.setId(message.getId());
        dto.setContent(message.getContent());
        dto.setFromBot(message.isFromBot());
        dto.setTimestamp(message.getTimestamp());
        if (message.getInterview() != null) {
            dto.setInterviewId(message.getInterview().getId());
        }
        return dto;
    }

    // Pas besoin de convertToEntity ici généralement
}