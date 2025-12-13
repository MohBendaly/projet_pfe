package com.mohamedbendali.sigc.service.impl;

import com.mohamedbendali.sigc.dto.ChatMessageDTO;
import com.mohamedbendali.sigc.entity.ChatMessage;
import com.mohamedbendali.sigc.entity.Interview;
import com.mohamedbendali.sigc.entity.JobApplication;
import com.mohamedbendali.sigc.entity.Candidate; // Import ajouté
import com.mohamedbendali.sigc.enums.ApplicationStatus;
import com.mohamedbendali.sigc.enums.InterviewStatus;
import com.mohamedbendali.sigc.exception.ResourceNotFoundException;
import com.mohamedbendali.sigc.exception.OperationNotAllowedException;
import com.mohamedbendali.sigc.repository.ChatMessageRepository;
import com.mohamedbendali.sigc.repository.InterviewRepository;
import com.mohamedbendali.sigc.repository.JobApplicationRepository;
import com.mohamedbendali.sigc.service.ChatService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ChatServiceImpl implements ChatService {
    @Value("${google.gemini.api.key}")
    private String geminiApiKey;

    @Value("${google.gemini.model.chat}")
    private String geminiModel;

    @Value("${google.gemini.api.baseurl}")
    private String geminiApiBaseUrl;
    private final ChatMessageRepository chatMessageRepository;
    private final InterviewRepository interviewRepository;
    private final WebClient.Builder webClientBuilder;
    private final JobApplicationRepository applicationRepository;

    @Override
    public ChatMessageDTO processCandidateMessage(Long interviewId, String messageContent) {
        log.debug("Processing candidate message for interview ID: {}", interviewId);
        ChatMessageDTO candidateMessageDto = saveMessageInternal(interviewId, messageContent, false);

        // Récupérer l'interview avec ses relations
        Interview interview = interviewRepository.findByIdWithDetails(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview", "id", interviewId));

        // Récupérer les détails de l'offre et du candidat
        String jobTitle = "[Poste non spécifié]";
        String candidateName = "[Candidat]";

        if (interview.getApplication() != null) {
            JobApplication application = interview.getApplication();

            // Récupérer le titre du poste
            if (application.getJobOffer() != null) {
                jobTitle = application.getJobOffer().getTitle();
            }

            // Récupérer le nom du candidat
            if (application.getCandidate() != null) {
                Candidate candidate = application.getCandidate();
                candidateName = candidate.getFirstName() + " " + candidate.getLastName();
            }
        }

        List<ChatMessage> history = chatMessageRepository.findByInterviewIdOrderByTimestampAsc(interviewId);
        log.info("Calling Gemini API for interview {} (Job: {}, Candidate: {})",
                interviewId, jobTitle, candidateName);

        String botResponseContent = callGeminiApiWebClient(history, jobTitle, candidateName);
        ChatMessageDTO botMessageDto = saveMessageInternal(interviewId, botResponseContent, true);
        log.info("Bot response saved for interview {}", interviewId);
        return botMessageDto;
    }

    private String callGeminiApiWebClient(List<ChatMessage> history, String jobTitle, String candidateName) {
        String apiUrl = geminiApiBaseUrl + geminiModel + ":generateContent?key=" + geminiApiKey;
        WebClient client = webClientBuilder.baseUrl(geminiApiBaseUrl).build();

        // Construire le contexte personnalisé
        String contextMessage = String.format(
                "CONTEXTE: Tu es un recruteur IA menant un entretien d'embauche pour le poste '%s'. " +
                        "Tu t'adresses au candidat %s. Continue la conversation de manière professionnelle " +
                        "et pose des questions pertinentes pour ce poste.",
                jobTitle, candidateName
        );

        List<Map<String, Object>> contents = new ArrayList<>();

        // Ajouter le contexte personnalisé
        contents.add(Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", contextMessage))
        ));

        // Convertir l'historique
        for (ChatMessage msg : history) {
            contents.add(Map.of(
                    "role", msg.isFromBot() ? "model" : "user",
                    "parts", List.of(Map.of("text", msg.getContent()))
            ));
        }

        Map<String, Object> requestBody = Map.of(
                "contents", contents,
                "generationConfig", Map.of(
                        "temperature", 0.7,
                        "maxOutputTokens", 250
                )
        );

        log.debug("Gemini API Request Body: {}", requestBody);

        try {
            GeminiApiResponse response = client.post()
                    .uri(apiUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    .flatMap(errorBody -> {
                                        log.error("Gemini API Error: Status={}, Body={}", clientResponse.statusCode(), errorBody);
                                        return Mono.error(new RuntimeException("Gemini API error: " + clientResponse.statusCode() + " Body: " + errorBody));
                                    }))
                    .bodyToMono(GeminiApiResponse.class)
                    .block();

            if (response != null && response.getCandidates() != null && !response.getCandidates().isEmpty()) {
                GeminiApiResponse.Candidate candidate = response.getCandidates().get(0);
                if (candidate.getContent() != null && candidate.getContent().getParts() != null && !candidate.getContent().getParts().isEmpty()) {
                    String content = candidate.getContent().getParts().get(0).getText();
                    log.debug("Received response content from Gemini API.");
                    return content != null ? content.trim() : "[Erreur: Réponse vide de l'IA Gemini]";
                }
            }
            log.error("Invalid or empty response structure received from Gemini API. Response: {}", response);
            return "[Erreur: Réponse invalide de l'IA Gemini]";

        } catch (Exception e) {
            log.error("Error calling Gemini API: {}", e.getMessage(), e);
            return "[Erreur: Impossible de contacter l'IA Gemini pour le moment]";
        }
    }

    @Override
    public ChatMessageDTO initiateChat(Long interviewId) {
        // Récupérer l'interview avec ses relations
        Interview interview = interviewRepository.findByIdWithDetails(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview", "id", interviewId));

        String jobTitle = "[Poste non spécifié]";
        String candidateName = "[Candidat]";

        if (interview.getApplication() != null) {
            JobApplication application = interview.getApplication();
            if (application.getJobOffer() != null) {
                jobTitle = application.getJobOffer().getTitle();
            }
            if (application.getCandidate() != null) {
                Candidate candidate = application.getCandidate();
                candidateName = candidate.getFirstName() + " " + candidate.getLastName();
            }
        }

        // Personnaliser le premier message
        String firstMessage = String.format(
                "Bonjour %s ! Je suis l'assistant IA pour votre entretien concernant le poste '%s'. " +
                        "Prêt(e) à commencer ?",
                candidateName, jobTitle
        );

        return saveMessageInternal(interviewId, firstMessage, true);
    }

    // ... [Les autres méthodes restent inchangées à partir d'ici] ...

    private ChatMessageDTO saveMessageInternal(Long interviewId, String content, boolean isFromBot) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview", "id", interviewId));

        if (interview.getStatus() != InterviewStatus.SCHEDULED && interview.getStatus() != InterviewStatus.IN_PROGRESS) {
            log.warn("Attempted to send message to interview ID {} with invalid status: {}", interviewId, interview.getStatus());
            throw new OperationNotAllowedException("Cannot send messages to an interview that is not scheduled or in progress.");
        }

        boolean wasScheduled = false;
        if (interview.getStatus() == InterviewStatus.SCHEDULED && !isFromBot) {
            log.info("Interview ID {} starting now (triggered by first candidate message).", interviewId);
            interview.setStatus(InterviewStatus.IN_PROGRESS);
            interview.setStartTime(LocalDateTime.now());
            wasScheduled = true;
        }

        if(interview.getStatus() != InterviewStatus.IN_PROGRESS) {
            log.warn("Attempted to send message to non-active interview ID: {}", interviewId);
            throw new OperationNotAllowedException("Cannot send messages to an interview that is not in progress.");
        }

        ChatMessage message = new ChatMessage();
        message.setInterview(interview);
        message.setContent(content);
        message.setFromBot(isFromBot);
        message.setTimestamp(LocalDateTime.now());
        ChatMessage savedMessage = chatMessageRepository.save(message);
        return convertToDto(savedMessage);
    }

    @Override
    public ChatMessageDTO saveMessage(ChatMessageDTO dto) {
        return saveMessageInternal(dto.getInterviewId(), dto.getContent(), dto.isFromBot());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatMessageDTO> getChatHistory(Long interviewId) {
        log.debug("Fetching chat history for interview ID: {}", interviewId);
        List<ChatMessage> messages = chatMessageRepository.findByInterviewIdOrderByTimestampAsc(interviewId);
        return messages.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Interview evaluateInterviewWithGemini(Long interviewId) {
        log.info("Starting evaluation process for interview ID: {}", interviewId);

        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview", "id", interviewId));

        if (interview.getStatus() != InterviewStatus.IN_PROGRESS && interview.getStatus() != InterviewStatus.PENDING_FEEDBACK) {
            log.warn("Cannot evaluate interview ID {} in status {}", interviewId, interview.getStatus());
            return interview;
        }

        List<ChatMessage> history = chatMessageRepository.findByInterviewIdOrderByTimestampAsc(interviewId);
        if (history.isEmpty()) {
            log.warn("Cannot evaluate interview ID {}: No chat history found.", interviewId);
            interview.setAiEvaluationScore(0.0);
            interview.setAiFeedback("Évaluation impossible : aucun historique de chat.");
            interview.setStatus(InterviewStatus.COMPLETED);
            interview.setEndTime(LocalDateTime.now());
            return interviewRepository.save(interview);
        }

        String evaluationPrompt = buildEvaluationPrompt(interview, history);
        log.info("Calling Gemini API for final evaluation of interview ID: {}", interviewId);
        String evaluationResult = callGeminiApiForEvaluation(evaluationPrompt);
        EvaluationScore scoreAndFeedback = parseEvaluationResponse(evaluationResult);

        interview.setAiEvaluationScore(scoreAndFeedback.getScore());
        interview.setAiFeedback(scoreAndFeedback.getFeedback());
        interview.setStatus(InterviewStatus.COMPLETED);
        interview.setEndTime(LocalDateTime.now());

        Interview savedInterview = interviewRepository.save(interview);
        log.info("Interview ID {} evaluated and updated with score: {}, feedback snippet: {}",
                savedInterview.getId(),
                savedInterview.getAiEvaluationScore(),
                savedInterview.getAiFeedback() != null ? savedInterview.getAiFeedback().substring(0, Math.min(50, savedInterview.getAiFeedback().length())) + "..." : "N/A");

        JobApplication application = interview.getApplication();
        if (application != null) {
            application.setStatus(ApplicationStatus.INTERVIEW_COMPLETED);
            application.setUpdatedAt(LocalDateTime.now());
            applicationRepository.save(application);
            log.info("JobApplication ID {} status updated to INTERVIEW_COMPLETED.", application.getId());
        } else {
            log.warn("Interview ID {} has no associated JobApplication to update status.", interviewId);
        }

        return savedInterview;
    }

    private String buildEvaluationPrompt(Interview interview, List<ChatMessage> history) {
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("ANALYSE D'ENTRETIEN D'EMBAUCHE\n\n");
        promptBuilder.append("POSTE : ").append(interview.getApplication().getJobOffer().getTitle()).append("\n");
        promptBuilder.append("DESCRIPTION DU POSTE (Extrait) : ").append(interview.getApplication().getJobOffer().getDescription().substring(0, Math.min(200, interview.getApplication().getJobOffer().getDescription().length()))).append("...\n\n");

        promptBuilder.append("TRANSCRIPTION DE LA CONVERSATION :\n");
        for (ChatMessage msg : history) {
            promptBuilder.append(msg.isFromBot() ? "Recruteur IA: " : "Candidat: ");
            promptBuilder.append(msg.getContent().replace("\n", "\n  ")).append("\n");
        }
        promptBuilder.append("\n------------------------------------\n");
        promptBuilder.append("INSTRUCTIONS POUR L'IA :\n");
        promptBuilder.append("1. Analyse la transcription ci-dessus.\n");
        promptBuilder.append("2. Évalue la pertinence des réponses du candidat par rapport au poste.\n");
        promptBuilder.append("3. Évalue les compétences clés mentionnées (si fournies) ou déduites.\n");
        promptBuilder.append("4. Fournis un feedback constructif et concis sur les points forts et les points faibles du candidat.\n");
        promptBuilder.append("5. Attribue un score global sur 100 basé sur ton évaluation.\n");
        promptBuilder.append("6. IMPORTANT : Structure ta réponse EXACTEMENT comme suit :\n");
        promptBuilder.append("SCORE: [Score numérique entre 0 et 100]\n");
        promptBuilder.append("FEEDBACK: [Ton analyse et feedback détaillé ici]\n");

        return promptBuilder.toString();
    }

    private String callGeminiApiForEvaluation(String prompt) {
        log.debug("Sending evaluation prompt to Gemini: {}...", prompt.substring(0, Math.min(100, prompt.length())));

        String apiUrl = geminiApiBaseUrl + geminiModel + ":generateContent?key=" + geminiApiKey;
        WebClient client = webClientBuilder.baseUrl(geminiApiBaseUrl).build();
        List<Map<String, Object>> contents = List.of(
                Map.of("role", "user", "parts", List.of(Map.of("text", prompt)))
        );
        Map<String, Object> requestBody = Map.of(
                "contents", contents,
                "generationConfig", Map.of(
                        "temperature", 0.5,
                        "maxOutputTokens", 500
                )
        );

        try {
            GeminiApiResponse response = client.post()
                    .uri(apiUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    .flatMap(errorBody -> {
                                        log.error("Gemini API Error (Evaluation): Status={}, Body={}", clientResponse.statusCode(), errorBody);
                                        return Mono.error(new RuntimeException("Gemini API error (Evaluation): " + clientResponse.statusCode()));
                                    }))
                    .bodyToMono(GeminiApiResponse.class)
                    .block();

            if (response != null && response.getCandidates() != null && !response.getCandidates().isEmpty() &&
                    response.getCandidates().get(0).getContent() != null && response.getCandidates().get(0).getContent().getParts() != null &&
                    !response.getCandidates().get(0).getContent().getParts().isEmpty())
            {
                return response.getCandidates().get(0).getContent().getParts().get(0).getText();
            } else {
                log.error("Invalid or empty evaluation response from Gemini.");
                return "SCORE: 0\nFEEDBACK: Erreur lors de la récupération de l'évaluation de l'IA.";
            }
        } catch (Exception e) {
            log.error("Error calling Gemini API for evaluation: {}", e.getMessage(), e);
            return "SCORE: 0\nFEEDBACK: Erreur technique lors de l'évaluation par l'IA.";
        }
    }

    private EvaluationScore parseEvaluationResponse(String rawResponse) {
        if (rawResponse == null || rawResponse.isBlank()) {
            return new EvaluationScore(0.0, "Réponse d'évaluation vide ou nulle.");
        }

        double score = 0.0;
        String feedback = "Impossible d'extraire le feedback détaillé.";

        Pattern scorePattern = Pattern.compile("^SCORE:\\s*(\\d{1,3}(?:\\.\\d+)?)\\s*$", Pattern.MULTILINE);
        Matcher scoreMatcher = scorePattern.matcher(rawResponse);

        if (scoreMatcher.find()) {
            try {
                score = Double.parseDouble(scoreMatcher.group(1));
                score = Math.max(0.0, Math.min(100.0, score));
                log.debug("Parsed score: {}", score);
            } catch (NumberFormatException e) {
                log.warn("Could not parse score number from response: {}", scoreMatcher.group(1));
            }
        } else {
            log.warn("Could not find 'SCORE: [number]' pattern in response:\n{}", rawResponse);
        }

        int feedbackIndex = rawResponse.indexOf("FEEDBACK:");
        if (feedbackIndex != -1) {
            feedback = rawResponse.substring(feedbackIndex + "FEEDBACK:".length()).trim();
            log.debug("Parsed feedback snippet: {}...", feedback.substring(0, Math.min(50, feedback.length())));
        } else {
            log.warn("Could not find 'FEEDBACK:' pattern in response. Using full response as fallback feedback.");
            feedback = rawResponse;
        }

        return new EvaluationScore(score, feedback);
    }

    private ChatMessageDTO convertToDto(ChatMessage message) {
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

    @Getter
    private static class EvaluationScore {
        private final double score;
        private final String feedback;

        public EvaluationScore(double score, String feedback) {
            this.score = score;
            this.feedback = feedback;
        }
    }

    private static class GeminiApiResponse {
        private List<Candidate> candidates;
        public List<Candidate> getCandidates() { return candidates; }
        public void setCandidates(List<Candidate> candidates) { this.candidates = candidates; }

        private static class Candidate {
            private Content content;
            public Content getContent() { return content; }
            public void setContent(Content content) { this.content = content; }
        }
        private static class Content {
            private List<Part> parts;
            private String role;
            public List<Part> getParts() { return parts; }
            public void setParts(List<Part> parts) { this.parts = parts; }
            public String getRole() { return role; }
            public void setRole(String role) { this.role = role; }
        }
        private static class Part {
            private String text;
            public String getText() { return text; }
            public void setText(String text) { this.text = text; }
        }
    }
}