package com.mohamedbendali.sigc.service.impl;

import com.mohamedbendali.sigc.dto.JobApplicationDTO;
import com.mohamedbendali.sigc.entity.*;
import com.mohamedbendali.sigc.enums.ApplicationStatus;
import com.mohamedbendali.sigc.enums.OfferStatus;
import com.mohamedbendali.sigc.exception.ResourceNotFoundException;
import com.mohamedbendali.sigc.exception.OperationNotAllowedException; // Nouvelle exception possible
import com.mohamedbendali.sigc.repository.CandidateRepository;
import com.mohamedbendali.sigc.repository.JobApplicationRepository;
import com.mohamedbendali.sigc.repository.JobOfferRepository;
import com.mohamedbendali.sigc.service.JobApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class JobApplicationServiceImpl implements JobApplicationService {

    private final JobApplicationRepository applicationRepository;
    private final CandidateRepository candidateRepository;
    private final JobOfferRepository offerRepository;

    @Override
    public JobApplicationDTO createApplication(JobApplicationDTO dto) {
        log.debug("Attempting to create application for candidate ID: {} and offer ID: {}", dto.getCandidateId(), dto.getJobOfferId());

        Candidate candidate = candidateRepository.findById(dto.getCandidateId())
                .orElseThrow(() -> new ResourceNotFoundException("Candidate", "id", dto.getCandidateId()));

        JobOffer offer = offerRepository.findById(dto.getJobOfferId())
                .orElseThrow(() -> new ResourceNotFoundException("JobOffer", "id", dto.getJobOfferId()));

        // Vérifier si l'offre est bien publiée et accepte les candidatures
        if (offer.getStatus() != OfferStatus.PUBLISHED) {
            log.warn("Attempted to apply for a non-published offer ID: {}", offer.getId());
            throw new OperationNotAllowedException("Cannot apply to an offer that is not published.");
        }

        // Vérifier si le candidat a déjà postulé à cette offre ?
        // boolean alreadyApplied = applicationRepository.existsByCandidateIdAndJobOfferId(candidate.getId(), offer.getId());
        // if (alreadyApplied) { ... }

        JobApplication application = new JobApplication();
        application.setCandidate(candidate);
        application.setJobOffer(offer);
        application.setCoverLetter(dto.getCoverLetter());
        application.setStatus(ApplicationStatus.RECEIVED); // Statut initial
        application.setApplicationDate(LocalDateTime.now());

        JobApplication savedApplication = applicationRepository.save(application);
        log.info("JobApplication created successfully with ID: {}", savedApplication.getId());
        return convertToDto(savedApplication);
    }

    @Override
    @Transactional(readOnly = true)
    public JobApplicationDTO getApplicationById(Long id) {
        log.debug("Fetching application by ID: {}", id);
        JobApplication application = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("JobApplication", "id", id));
        return convertToDto(application);
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobApplicationDTO> getApplicationsByCandidateId(Long candidateId) {
        log.debug("Fetching applications for candidate ID: {}", candidateId);
        // Vérifier si le candidat existe? Pas forcément nécessaire si on retourne juste une liste vide.
        List<JobApplication> applications = applicationRepository.findByCandidateId(candidateId);
        return applications.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<JobApplicationDTO> getApplicationsByOfferId(Long offerId, Pageable pageable) {
        log.debug("Fetching applications for offer ID: {}, page: {}, size: {}", offerId, pageable.getPageNumber(), pageable.getPageSize());
        // Vérifier si l'offre existe?
        return applicationRepository.findByJobOfferId(offerId, pageable).map(this::convertToDto);
    }

    @Override
    public JobApplicationDTO updateApplicationStatus(Long id, ApplicationStatus status) {
        log.debug("Updating status for application ID: {} to {}", id, status);
        JobApplication application = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("JobApplication", "id", id));

        // Ajouter une logique de validation de transition de statut si nécessaire
        // Ex: On ne peut pas passer de REJECTED à INTERVIEW_SCHEDULED directement
        application.setStatus(status);
        application.setUpdatedAt(LocalDateTime.now());
        JobApplication updatedApplication = applicationRepository.save(application);
        log.info("Application status updated successfully for ID: {}", updatedApplication.getId());
        // Envoyer une notification au candidat ?
        return convertToDto(updatedApplication);
    }

    @Override
    public void withdrawApplication(Long id) {
        log.debug("Withdrawing application ID: {}", id);
        JobApplication application = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("JobApplication", "id", id));

        // Vérifier si le statut actuel permet le retrait
        // (On peut retirer une candidature tant qu'elle n'est pas ACCEPTED ou REJECTED final?)
        if (application.getStatus() == ApplicationStatus.ACCEPTED || application.getStatus() == ApplicationStatus.REJECTED) {
            log.warn("Attempted to withdraw an application with final status for ID: {}", id);
            throw new OperationNotAllowedException("Cannot withdraw an application with status " + application.getStatus());
        }

        application.setStatus(ApplicationStatus.WITHDRAWN);
        application.setUpdatedAt(LocalDateTime.now());
        applicationRepository.save(application);
        log.info("Application withdrawn successfully for ID: {}", id);
    }

    // --- Méthodes de Mapping ---

    private JobApplicationDTO convertToDto(JobApplication app) {
        if (app == null) return null;
        JobApplicationDTO dto = new JobApplicationDTO();
        dto.setId(app.getId());
        dto.setStatus(app.getStatus());
        dto.setApplicationDate(app.getApplicationDate());
        dto.setUpdatedAt(app.getUpdatedAt());
        dto.setCoverLetter(app.getCoverLetter());

        if (app.getCandidate() != null) {
            dto.setCandidateId(app.getCandidate().getId());
            dto.setCandidateFullName(app.getCandidate().getFirstName() + " " + app.getCandidate().getLastName());
        }
        if (app.getJobOffer() != null) {
            dto.setJobOfferId(app.getJobOffer().getId());
            dto.setJobOfferTitle(app.getJobOffer().getTitle());
        }
        // Mapper les IDs des entretiens et pièces jointes
        if (app.getInterviews() != null) {
            dto.setInterviewIds(app.getInterviews().stream().map(Interview::getId).collect(Collectors.toList()));
        }
        if (app.getAttachments() != null) {
            dto.setAttachmentIds(app.getAttachments().stream().map(Attachment::getId).collect(Collectors.toList()));
        }
        return dto;
    }

    // Pas besoin de convertToEntity ici car la création utilise directement les IDs du DTO
}

// Ajouter l'exception personnalisée si nécessaire
// package com.mohamedbendali.sigc.exception;
// public class OperationNotAllowedException extends RuntimeException {
//     public OperationNotAllowedException(String message) { super(message); }
// }