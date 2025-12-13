package com.mohamedbendali.sigc.controller;

import com.mohamedbendali.sigc.dto.ChatMessageDTO;
import com.mohamedbendali.sigc.dto.InterviewDTO;
import com.mohamedbendali.sigc.entity.Interview;
import com.mohamedbendali.sigc.service.ChatService;
import com.mohamedbendali.sigc.service.InterviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map; // Pour le corps de la requête simple

@RestController
@RequestMapping("/api/interviews")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewService interviewService;
    private final ChatService chatService;

    // Endpoint pour qu'un Recruteur planifie un entretien pour une candidature
    @PostMapping("/application/{applicationId}")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    public ResponseEntity<InterviewDTO> scheduleInterview(
            @PathVariable Long applicationId,
            @Valid @RequestBody(required = false) InterviewDTO dto) { // DTO peut être optionnel si seule la date/heure est nécessaire
        // Si DTO est null, utiliser valeurs par défaut dans le service
        InterviewDTO scheduledInterview = interviewService.scheduleInterview(applicationId, dto != null ? dto : new InterviewDTO());
        return ResponseEntity.status(HttpStatus.CREATED).body(scheduledInterview);
    }

    // Endpoint pour récupérer les détails d'un entretien (Recruteur/Admin ou Participant)
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('RECRUITER') or hasRole('ADMIN') or @securityService.isInterviewParticipant(#id, principal)")
    public ResponseEntity<InterviewDTO> getInterviewById(@PathVariable Long id) {
        // TODO: Implémenter la vérification de participation
        InterviewDTO interview = interviewService.getInterviewById(id);
        return ResponseEntity.ok(interview);
    }

    // Endpoint pour récupérer tous les entretiens d'une candidature (Recruteur/Admin ou Propriétaire de la candidature)
    @GetMapping("/application/{applicationId}")
    @PreAuthorize("hasRole('RECRUITER') or hasRole('ADMIN') or @securityService.isApplicationOwnerOfInterview(#applicationId, principal)")
    public ResponseEntity<List<InterviewDTO>> getInterviewsForApplication(@PathVariable Long applicationId) {
        // TODO: Implémenter la vérification de propriété de la candidature
        List<InterviewDTO> interviews = interviewService.getInterviewsByApplicationId(applicationId);
        return ResponseEntity.ok(interviews);
    }


    // Endpoint pour démarrer un entretien (peut être implicite via le premier message chat)
    // Ou un endpoint explicite si nécessaire (ex: le candidat clique sur "Démarrer")
    @PostMapping("/{id}/start")
    @PreAuthorize("@securityService.isInterviewParticipant(#id, principal)") // Le participant démarre
    public ResponseEntity<InterviewDTO> startInterview(@PathVariable Long id) {
        // TODO: Implémenter la vérification de participation
        InterviewDTO startedInterview = interviewService.startInterview(id);
        return ResponseEntity.ok(startedInterview);
    }

    // Endpoint pour qu'un Recruteur marque un entretien comme terminé (avec score/feedback)
    @PostMapping("/{id}/complete")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<InterviewDTO> completeInterview(
            @PathVariable Long id,
            @RequestBody Map<String, Object> payload) { // Utiliser un DTO spécifique serait mieux
        Double score = payload.containsKey("score") ? ((Number) payload.get("score")).doubleValue() : null;
        String feedback = (String) payload.get("feedback");
        InterviewDTO completedInterview = interviewService.completeInterview(id, score, feedback);
        return ResponseEntity.ok(completedInterview);
    }

    // Endpoint pour annuler un entretien (Recruteur ou Participant)
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('RECRUITER') or @securityService.isInterviewParticipant(#id, principal)")
    public ResponseEntity<InterviewDTO> cancelInterview(@PathVariable Long id) {
        // TODO: Implémenter la vérification de participation/rôle
        InterviewDTO cancelledInterview = interviewService.cancelInterview(id);
        return ResponseEntity.ok(cancelledInterview);
    }

    // --- Endpoints pour le Chat de l'entretien ---

    // Récupérer l'historique du chat (Recruteur/Admin ou Participant)
    @GetMapping("/{id}/chat")
    @PreAuthorize("hasRole('RECRUITER') or hasRole('ADMIN') or @securityService.isInterviewParticipant(#id, principal)")
    public ResponseEntity<List<ChatMessageDTO>> getChatHistory(@PathVariable Long id) {
        // TODO: Implémenter la vérification de participation
        List<ChatMessageDTO> history = chatService.getChatHistory(id);
        return ResponseEntity.ok(history);
    }

    // Envoyer un message (typiquement le candidat) et obtenir la réponse du bot
    @PostMapping("/{id}/chat")
    @PreAuthorize("@securityService.isInterviewParticipant(#id, principal)") // Seul le participant peut envoyer un message user
    public ResponseEntity<ChatMessageDTO> postChatMessage(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload) { // Attends {"content": "message"}
        // TODO: Implémenter la vérification de participation
        String content = payload.get("content");
        if (content == null || content.isBlank()) {
            return ResponseEntity.badRequest().build(); // Message vide non autorisé
        }
        // Le service traitera le message du candidat et retournera la réponse du bot
        ChatMessageDTO botResponse = chatService.processCandidateMessage(id, content);
        return ResponseEntity.ok(botResponse);
    }
    @PostMapping("/{id}/finish")
    @PreAuthorize("hasRole('RECRUITER') or hasRole('ADMIN') or @securityService.isInterviewParticipant(#id, principal)")
    public ResponseEntity<InterviewDTO> finishAndEvaluateInterview(@PathVariable Long id) {
        // Appeler directement via l'interface ChatService
        Interview evaluatedInterview = chatService.evaluateInterviewWithGemini(id);

        if (evaluatedInterview != null) {
            InterviewDTO responseDto = convertInterviewEntityToDto(evaluatedInterview); // Utiliser votre mapper
            return ResponseEntity.ok(responseDto);
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    private InterviewDTO convertInterviewEntityToDto(Interview interview) {
        if (interview == null) return null;
        InterviewDTO dto = new InterviewDTO();
        dto.setId(interview.getId());
        // Utiliser l'opérateur ternaire ou Optional pour éviter NullPointerException
        dto.setApplicationId(interview.getApplication() != null ? interview.getApplication().getId() : null);
        dto.setStartTime(interview.getStartTime());
        dto.setEndTime(interview.getEndTime());
        dto.setStatus(interview.getStatus());
        dto.setAiEvaluationScore(interview.getAiEvaluationScore());
        dto.setAiFeedback(interview.getAiFeedback());
        dto.setCreatedAt(interview.getCreatedAt());
        // Ne pas inclure les chatMessages par défaut pour la performance
        // dto.setChatMessages( ... );
        return dto;
    }
}