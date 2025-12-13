package com.mohamedbendali.sigc.dto;

import com.mohamedbendali.sigc.enums.InterviewStatus;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class InterviewDTO {
    private Long id;
    private Long applicationId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private InterviewStatus status;
    private Double aiEvaluationScore;
    private String aiFeedback;
    private LocalDateTime createdAt;
    private List<ChatMessageDTO> chatMessages; // Inclure les messages si nécessaire
}