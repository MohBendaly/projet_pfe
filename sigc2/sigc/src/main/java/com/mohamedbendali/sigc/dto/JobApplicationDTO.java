package com.mohamedbendali.sigc.dto;

import com.mohamedbendali.sigc.enums.ApplicationStatus;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class JobApplicationDTO {
    private Long id;
    private ApplicationStatus status;
    private Long candidateId;
    private String candidateFullName; // Pour affichage facile
    private Long jobOfferId;
    private String jobOfferTitle; // Pour affichage facile
    private String coverLetter;
    private LocalDateTime applicationDate;
    private LocalDateTime updatedAt;
    private List<Long> interviewIds; // Juste les IDs
    private List<Long> attachmentIds; // Juste les IDs
}