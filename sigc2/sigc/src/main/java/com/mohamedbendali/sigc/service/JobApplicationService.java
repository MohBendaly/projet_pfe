package com.mohamedbendali.sigc.service;

import com.mohamedbendali.sigc.dto.JobApplicationDTO;
import com.mohamedbendali.sigc.enums.ApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface JobApplicationService {
    JobApplicationDTO createApplication(JobApplicationDTO dto);
    JobApplicationDTO getApplicationById(Long id);
    List<JobApplicationDTO> getApplicationsByCandidateId(Long candidateId);
    Page<JobApplicationDTO> getApplicationsByOfferId(Long offerId, Pageable pageable);
    JobApplicationDTO updateApplicationStatus(Long id, ApplicationStatus status);
    void withdrawApplication(Long id); // Action du candidat
    // Autres méthodes (ex: filtrer par statut, etc.)
}