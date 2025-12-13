package com.mohamedbendali.sigc.controller;

import com.mohamedbendali.sigc.dto.CandidateDTO;
import com.mohamedbendali.sigc.dto.JobApplicationDTO;
import com.mohamedbendali.sigc.entity.Candidate; // Pour récupérer l'ID du principal
import com.mohamedbendali.sigc.enums.ApplicationStatus;
import com.mohamedbendali.sigc.service.JobApplicationService;
import com.mohamedbendali.sigc.service.CandidateService; // Pour obtenir le Candidate ID
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class JobApplicationController {

    private final JobApplicationService applicationService;
    private final CandidateService candidateService; // Pour trouver l'ID du candidat connecté

    // Endpoint pour qu'un candidat crée une nouvelle candidature
    @PostMapping
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<JobApplicationDTO> createApplication(
            @Valid @RequestBody JobApplicationDTO dto,
            @AuthenticationPrincipal UserDetails userDetails) {

        // Récupérer l'ID du candidat à partir de l'utilisateur connecté
        CandidateDTO currentCandidate = candidateService.getCandidateByEmail(userDetails.getUsername()); // Placeholder
        dto.setCandidateId(currentCandidate.getId());
        // Assurer que l'ID et le statut ne sont pas forcés par le DTO entrant
        dto.setId(null);
        dto.setStatus(null);

        JobApplicationDTO createdApplication = applicationService.createApplication(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdApplication);
    }

    // Endpoint pour récupérer une candidature spécifique (Recruteur/Admin ou le candidat propriétaire)
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('RECRUITER') or hasRole('ADMIN') or @securityService.isApplicationOwner(#id, principal)")
    public ResponseEntity<JobApplicationDTO> getApplicationById(@PathVariable Long id) {
        // TODO: Implémenter la vérification de propriété dans SecurityService
        JobApplicationDTO application = applicationService.getApplicationById(id);
        return ResponseEntity.ok(application);
    }

    // Endpoint pour que le candidat récupère ses propres candidatures
    @GetMapping("/my")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<List<JobApplicationDTO>> getMyApplications(@AuthenticationPrincipal UserDetails userDetails) {
        CandidateDTO currentCandidate = candidateService.getCandidateByEmail(userDetails.getUsername()); // Placeholder
        List<JobApplicationDTO> applications = applicationService.getApplicationsByCandidateId(currentCandidate.getId());
        return ResponseEntity.ok(applications);
    }

    // Endpoint pour qu'un Recruteur/Admin voie les candidatures d'une offre spécifique
    @GetMapping("/offer/{offerId}")
    @PreAuthorize("hasRole('RECRUITER') or hasRole('ADMIN')")
    public ResponseEntity<Page<JobApplicationDTO>> getApplicationsForOffer(
            @PathVariable Long offerId,
            @PageableDefault(size = 10, sort = "applicationDate") Pageable pageable) {
        Page<JobApplicationDTO> applications = applicationService.getApplicationsByOfferId(offerId, pageable);
        return ResponseEntity.ok(applications);
    }

    // Endpoint pour qu'un Recruteur/Admin change le statut d'une candidature
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('RECRUITER') or hasRole('ADMIN')")
    public ResponseEntity<JobApplicationDTO> updateApplicationStatus(
            @PathVariable Long id,
            @RequestParam ApplicationStatus status) {
        JobApplicationDTO updatedApplication = applicationService.updateApplicationStatus(id, status);
        return ResponseEntity.ok(updatedApplication);
    }

    // Endpoint pour qu'un candidat retire sa candidature
    @PatchMapping("/{id}/withdraw")
    @PreAuthorize("@securityService.isApplicationOwner(#id, principal)") // Sécurisé pour le propriétaire
    public ResponseEntity<Void> withdrawApplication(@PathVariable Long id) {
        // TODO: Implémenter la vérification de propriété
        applicationService.withdrawApplication(id);
        return ResponseEntity.noContent().build();
    }

    // TODO: Ajouter endpoints pour gérer les pièces jointes liées à une candidature
    // Ex: POST /api/applications/{id}/attachments
}