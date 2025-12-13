package com.mohamedbendali.sigc.service;

import com.mohamedbendali.sigc.entity.*;
import com.mohamedbendali.sigc.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Important pour charger les relations LAZY

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service("securityService") // Nom du bean utilisé dans @PreAuthorize
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true) // La plupart des méthodes sont en lecture seule
public class SecurityService {
    private final AttachmentRepository attachmentRepository; // Ajouter le repo Attachment

    private final JobApplicationRepository applicationRepository;
    private final CandidateRepository candidateRepository;
    private final InterviewRepository interviewRepository;
    private final UserRepository userRepository; // Pour obtenir l'entité User complète

    /**
     * Obtient l'entité User de l'utilisateur actuellement authentifié.
     *
     * @param principal L'objet principal (peut être UserDetails ou Authentication).
     * @return Optional<User> L'utilisateur trouvé, ou Optional.empty() s'il n'est pas connecté ou introuvable.
     */
    private Optional<User> getCurrentUserEntity(Object principal) {
        String username = null;
        if (principal instanceof UserDetails) {
            username = ((UserDetails) principal).getUsername();
            // Note: getUsername() peut retourner l'email si configuré ainsi dans UserDetailsService
        } else if (principal instanceof Authentication) {
            Object authPrincipal = ((Authentication) principal).getPrincipal();
            if (authPrincipal instanceof UserDetails) {
                username = ((UserDetails) authPrincipal).getUsername();
            } else if (authPrincipal instanceof String) {
                username = (String) authPrincipal; // Cas moins courant
            }
        } else if (principal instanceof String) {
            username = (String) principal; // Si seul le username est passé
        }

        if (username == null) {
            // Essayer via SecurityContextHolder si principal n'est pas fourni explicitement
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof UserDetails) {
                username = ((UserDetails) authentication.getPrincipal()).getUsername();
            }
        }


        if (username != null) {
            // Chercher par username ou email, car UserDetails.getUsername() peut contenir l'un ou l'autre
            return userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase(username, username);
        }

        log.warn("Could not determine username from principal: {}", principal);
        return Optional.empty();
    }

    /**
     * Vérifie si l'utilisateur connecté est le propriétaire (candidat) de la candidature donnée.
     *
     * @param applicationId L'ID de la JobApplication.
     * @param principal     L'objet Authentication principal.
     * @return true si l'utilisateur est le propriétaire, false sinon.
     */
    public boolean isApplicationOwner(Long applicationId, Object principal) {
        Optional<User> currentUserOpt = getCurrentUserEntity(principal);
        if (currentUserOpt.isEmpty()) {
            log.warn("isApplicationOwner check failed: User not authenticated or found.");
            return false; // Non authentifié
        }
        User currentUser = currentUserOpt.get();

        Optional<JobApplication> applicationOpt = applicationRepository.findById(applicationId);
        if (applicationOpt.isEmpty()) {
            log.warn("isApplicationOwner check failed: Application not found with ID {}", applicationId);
            return false; // Application non trouvée
        }

        JobApplication application = applicationOpt.get();
        // Vérifier si le candidat de l'application est lié à l'utilisateur courant
        if (application.getCandidate() != null && application.getCandidate().getUser() != null) {
            boolean isOwner = Objects.equals(application.getCandidate().getUser().getId(), currentUser.getId());
            log.debug("isApplicationOwner check for app ID {}: User ID {} vs Owner User ID {}. Result: {}",
                    applicationId, currentUser.getId(), application.getCandidate().getUser().getId(), isOwner);
            return isOwner;
        } else {
            log.warn("isApplicationOwner check failed: Application ID {} has no linked candidate or user.", applicationId);
            return false; // Pas de candidat ou d'utilisateur lié à l'application
        }
    }

    /**
     * Vérifie si l'utilisateur connecté correspond au profil Candidat demandé.
     *
     * @param candidateId L'ID du Candidate.
     * @param principal   L'objet Authentication principal.
     * @return true si l'utilisateur correspond au candidat, false sinon.
     */
    public boolean isCandidateOwner(Long candidateId, Object principal) {
        Optional<User> currentUserOpt = getCurrentUserEntity(principal);
        if (currentUserOpt.isEmpty()) {
            log.warn("isCandidateOwner check failed: User not authenticated or found.");
            return false;
        }
        User currentUser = currentUserOpt.get();

        Optional<Candidate> candidateOpt = candidateRepository.findById(candidateId);
        if (candidateOpt.isEmpty()) {
            log.warn("isCandidateOwner check failed: Candidate not found with ID {}", candidateId);
            return false;
        }

        Candidate candidate = candidateOpt.get();
        if (candidate.getUser() != null) {
            boolean isOwner = Objects.equals(candidate.getUser().getId(), currentUser.getId());
            log.debug("isCandidateOwner check for candidate ID {}: User ID {} vs Owner User ID {}. Result: {}",
                    candidateId, currentUser.getId(), candidate.getUser().getId(), isOwner);
            return isOwner;
        } else {
            log.warn("isCandidateOwner check failed: Candidate ID {} has no linked user.", candidateId);
            return false; // Pas d'utilisateur lié au candidat
        }
    }

    /**
     * Vérifie si l'utilisateur connecté participe à l'entretien donné (en tant que candidat).
     *
     * @param interviewId L'ID de l'Interview.
     * @param principal   L'objet Authentication principal.
     * @return true si l'utilisateur est le candidat de l'entretien, false sinon.
     */
    public boolean isInterviewParticipant(Long interviewId, Object principal) {
        Optional<User> currentUserOpt = getCurrentUserEntity(principal);
        if (currentUserOpt.isEmpty()) {
            log.warn("isInterviewParticipant check failed: User not authenticated or found.");
            return false;
        }
        User currentUser = currentUserOpt.get();

        Optional<Interview> interviewOpt = interviewRepository.findById(interviewId);
        if (interviewOpt.isEmpty()) {
            log.warn("isInterviewParticipant check failed: Interview not found with ID {}", interviewId);
            return false;
        }

        Interview interview = interviewOpt.get();
        // Remonter de Interview -> JobApplication -> Candidate -> User
        if (interview.getApplication() != null &&
                interview.getApplication().getCandidate() != null &&
                interview.getApplication().getCandidate().getUser() != null) {

            boolean isParticipant = Objects.equals(interview.getApplication().getCandidate().getUser().getId(), currentUser.getId());
            log.debug("isInterviewParticipant check for interview ID {}: User ID {} vs Participant User ID {}. Result: {}",
                    interviewId, currentUser.getId(), interview.getApplication().getCandidate().getUser().getId(), isParticipant);
            return isParticipant;
        } else {
            log.warn("isInterviewParticipant check failed: Interview ID {} has incomplete links (App/Candidate/User).", interviewId);
            return false; // Chaîne de liaison incomplète
        }
    }

    /**
     * Vérifie si l'utilisateur connecté est le propriétaire de la candidature associée à la ressource demandée (ex: entretien).
     * Utilisé quand on accède à une ressource via l'ID de sa candidature parente.
     *
     * @param applicationId L'ID de la JobApplication parente.
     * @param principal     L'objet Authentication principal.
     * @return true si l'utilisateur est le propriétaire de la candidature, false sinon.
     */
    public boolean isApplicationOwnerOfResource(Long applicationId, Object principal) {
        // C'est la même logique que isApplicationOwner, mais le nom peut être plus clair dans certains contextes PreAuthorize.
        return isApplicationOwner(applicationId, principal);
    }


    // --- Fonctions utilitaires pour les rôles (peuvent aussi être utilisées dans PreAuthorize) ---

    /**
     * Vérifie si l'utilisateur courant a le rôle spécifié.
     * @param roleName Nom du rôle (ex: "ROLE_ADMIN", "ROLE_RECRUITER").
     * @return true si l'utilisateur a le rôle, false sinon.
     */
    public boolean hasRole(String roleName) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals(roleName));
    }

    /**
     * Vérifie si l'utilisateur courant a l'un des rôles spécifiés.
     * @param roles Noms des rôles.
     * @return true si l'utilisateur a au moins un des rôles, false sinon.
     */
    public boolean hasAnyRole(String... roles) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        Set<String> userRoles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        for (String role : roles) {
            if (userRoles.contains(role)) {
                return true;
            }
        }
        return false;
    }
    /**
     * Vérifie si l'utilisateur courant est le propriétaire de la pièce jointe
     * (soit via le Candidate parent, soit via la JobApplication parente)
     * ou s'il est Admin/Recruteur.
     *
     * @param attachmentId L'ID de la pièce jointe.
     * @param principal L'objet Authentication principal.
     * @return true si l'accès est autorisé, false sinon.
     */
    public boolean isAttachmentOwner(Long attachmentId, Object principal) {
        Optional<User> currentUserOpt = getCurrentUserEntity(principal);
        if (currentUserOpt.isEmpty()) {
            return false; // Non authentifié
        }
        User currentUser = currentUserOpt.get();

        // Vérifier si Admin ou Recruteur (ils ont accès à tout)
        if (currentUser.getRoles().contains(User.Role.ROLE_ADMIN) || currentUser.getRoles().contains(User.Role.ROLE_RECRUITER)) {
            log.debug("isAttachmentOwner check: Access granted for ADMIN/RECRUITER for attachment ID {}", attachmentId);
            return true;
        }

        // Trouver l'attachement
        Optional<Attachment> attachmentOpt = attachmentRepository.findById(attachmentId);
        if (attachmentOpt.isEmpty()) {
            log.warn("isAttachmentOwner check failed: Attachment not found with ID {}", attachmentId);
            return false; // Attachement non trouvé
        }
        Attachment attachment = attachmentOpt.get();

        // Vérifier si lié à un Candidat et si c'est le bon candidat
        if (attachment.getOwner() != null && attachment.getOwner().getUser() != null) {
            boolean isOwner = Objects.equals(attachment.getOwner().getUser().getId(), currentUser.getId());
            log.debug("isAttachmentOwner check via Candidate for attachment ID {}: User ID {} vs Owner User ID {}. Result: {}",
                    attachmentId, currentUser.getId(), attachment.getOwner().getUser().getId(), isOwner);
            return isOwner;
        }

        // Vérifier si lié à une Application et si c'est la bonne application (via son candidat)
        if (attachment.getJobApplication() != null &&
                attachment.getJobApplication().getCandidate() != null &&
                attachment.getJobApplication().getCandidate().getUser() != null) {
            boolean isAppOwner = Objects.equals(attachment.getJobApplication().getCandidate().getUser().getId(), currentUser.getId());
            log.debug("isAttachmentOwner check via Application for attachment ID {}: User ID {} vs App Owner User ID {}. Result: {}",
                    attachmentId, currentUser.getId(), attachment.getJobApplication().getCandidate().getUser().getId(), isAppOwner);
            return isAppOwner;
        }

        log.warn("isAttachmentOwner check failed: Attachment ID {} has no verifiable owner link.", attachmentId);
        return false; // Pas de lien propriétaire vérifiable
    }
}