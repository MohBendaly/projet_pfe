package com.mohamedbendali.sigc.service.impl;

import com.mohamedbendali.sigc.dto.AttachmentDTO;
import com.mohamedbendali.sigc.entity.Attachment;
import com.mohamedbendali.sigc.entity.Candidate;
import com.mohamedbendali.sigc.entity.JobApplication;
import com.mohamedbendali.sigc.exception.FileStorageException;
import com.mohamedbendali.sigc.exception.ResourceNotFoundException;
import com.mohamedbendali.sigc.repository.AttachmentRepository;
import com.mohamedbendali.sigc.repository.CandidateRepository;
import com.mohamedbendali.sigc.repository.JobApplicationRepository;
import com.mohamedbendali.sigc.service.AttachmentService;
import com.mohamedbendali.sigc.service.FileStorageService;
// import com.mohamedbendali.sigc.service.SecurityService; // Pour les vérifications de permission
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.security.access.AccessDeniedException; // Pour les erreurs de permission
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder; // Pour construire l'URL de download

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AttachmentServiceImpl implements AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final FileStorageService fileStorageService;
    private final CandidateRepository candidateRepository;
    private final JobApplicationRepository applicationRepository;
    // private final SecurityService securityService; // Injecter pour vérifs de permission

    // Injecter le path de base des uploads si nécessaire pour construire l'URL
    // Non utilisé ici, on construit depuis le chemin retourné par fileStorageService

    @Override
    public AttachmentDTO storeFileForCandidate(MultipartFile file, Long candidateId) {
        log.debug("Storing file for candidate ID: {}", candidateId);
        Candidate candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate", "id", candidateId));

        // Logique de sécurité : vérifier si l'utilisateur courant peut uploader pour ce candidat
        // if (!securityService.canManageCandidate(candidateId, SecurityContextHolder.getContext().getAuthentication().getPrincipal()) {
        //     throw new AccessDeniedException("User cannot upload files for this candidate.");
        // }

        // Définir un sous-répertoire (ex: "candidates/{candidateId}")
        String subDirectory = "candidates/" + candidateId;
        String storedPath = fileStorageService.store(file, subDirectory);

        Attachment attachment = createAttachmentEntity(file, storedPath);
        attachment.setOwner(candidate); // Lier au candidat

        Attachment savedAttachment = attachmentRepository.save(attachment);
        log.info("File stored successfully for candidate ID {}. Attachment ID: {}, Path: {}", candidateId, savedAttachment.getId(), storedPath);
        return convertToDto(savedAttachment);
    }

    @Override
    public AttachmentDTO storeFileForApplication(MultipartFile file, Long applicationId) {
        log.debug("Storing file for application ID: {}", applicationId);
        JobApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("JobApplication", "id", applicationId));

        // Logique de sécurité : vérifier si l'utilisateur courant peut uploader pour cette application
        // if (!securityService.canManageApplication(applicationId, SecurityContextHolder.getContext().getAuthentication().getPrincipal()) {
        //     throw new AccessDeniedException("User cannot upload files for this application.");
        // }

        // Définir un sous-répertoire (ex: "applications/{applicationId}")
        String subDirectory = "applications/" + applicationId;
        String storedPath = fileStorageService.store(file, subDirectory);

        Attachment attachment = createAttachmentEntity(file, storedPath);
        attachment.setJobApplication(application); // Lier à l'application

        Attachment savedAttachment = attachmentRepository.save(attachment);
        log.info("File stored successfully for application ID {}. Attachment ID: {}, Path: {}", applicationId, savedAttachment.getId(), storedPath);
        return convertToDto(savedAttachment);
    }

    // Méthode helper pour créer l'entité Attachment de base
    private Attachment createAttachmentEntity(MultipartFile file, String storedPath) {
        Attachment attachment = new Attachment();
        attachment.setFileName(org.springframework.util.StringUtils.cleanPath(file.getOriginalFilename()));
        attachment.setFileType(file.getContentType());
        attachment.setFileSize(file.getSize());
        attachment.setFilePath(storedPath); // Chemin relatif retourné par FileStorageService
        attachment.setUploadedAt(LocalDateTime.now());
        return attachment;
    }


    @Override
    @Transactional(readOnly = true)
    public AttachmentDTO getAttachmentMetadata(Long attachmentId) {
        log.debug("Fetching metadata for attachment ID: {}", attachmentId);
        Attachment attachment = findAttachmentByIdAndCheckAccess(attachmentId);
        return convertToDto(attachment);
    }

    @Override
    @Transactional(readOnly = true)
    public Resource downloadFile(Long attachmentId) {
        log.debug("Requesting download for attachment ID: {}", attachmentId);
        Attachment attachment = findAttachmentByIdAndCheckAccess(attachmentId);
        try {
            // Extraire le sous-répertoire et le nom de fichier du storedPath
            java.nio.file.Path filePath = java.nio.file.Paths.get(attachment.getFilePath());
            String subDirectory = filePath.getParent() != null ? filePath.getParent().toString() : "";
            String filename = filePath.getFileName().toString();
            return fileStorageService.loadAsResource(filename, subDirectory);
        } catch (FileStorageException e) {
            log.error("Could not load file for attachment ID {}: {}", attachmentId, e.getMessage());
            // Renvoyer une exception spécifique ou ResourceNotFound ?
            throw new ResourceNotFoundException("File not found or cannot be read for attachment: " + attachmentId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttachmentDTO> getAttachmentsForCandidate(Long candidateId) {
        log.debug("Fetching attachments for candidate ID: {}", candidateId);
        // Vérifier si le candidat existe
        candidateRepository.findById(candidateId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate", "id", candidateId));

        // Logique de sécurité : vérifier si l'utilisateur courant peut voir les pièces jointes de ce candidat
        // if (!securityService.canViewCandidateAttachments(candidateId, SecurityContextHolder.getContext().getAuthentication().getPrincipal()) {
        //     throw new AccessDeniedException("User cannot view attachments for this candidate.");
        // }

        return attachmentRepository.findByOwnerId(candidateId)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttachmentDTO> getAttachmentsForApplication(Long applicationId) {
        log.debug("Fetching attachments for application ID: {}", applicationId);
        // Vérifier si l'application existe
        applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("JobApplication", "id", applicationId));

        // Logique de sécurité : vérifier si l'utilisateur courant peut voir les pièces jointes de cette application
        // if (!securityService.canViewApplicationAttachments(applicationId, SecurityContextHolder.getContext().getAuthentication().getPrincipal()) {
        //     throw new AccessDeniedException("User cannot view attachments for this application.");
        // }

        return attachmentRepository.findByJobApplicationId(applicationId)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteAttachment(Long attachmentId) {
        log.debug("Attempting to delete attachment ID: {}", attachmentId);
        Attachment attachment = findAttachmentByIdAndCheckAccess(attachmentId); // Vérifie aussi l'accès

        try {
            // Extraire le sous-répertoire et le nom de fichier
            java.nio.file.Path filePath = java.nio.file.Paths.get(attachment.getFilePath());
            String subDirectory = filePath.getParent() != null ? filePath.getParent().toString() : "";
            String filename = filePath.getFileName().toString();

            // 1. Supprimer le fichier physique
            fileStorageService.delete(filename, subDirectory);
            log.info("Physical file deleted: {}", attachment.getFilePath());

            // 2. Supprimer l'enregistrement en base de données
            attachmentRepository.delete(attachment);
            log.info("Attachment metadata deleted for ID: {}", attachmentId);

        } catch (Exception e) {
            log.error("Could not delete attachment ID {}: {}", attachmentId, e.getMessage(), e);
            // Que faire si le fichier physique est supprimé mais pas la BDD (ou vice-versa) ? Transaction ?
            throw new RuntimeException("Failed to delete attachment " + attachmentId, e);
        }
    }

    // Méthode privée pour trouver l'attachement et vérifier l'accès (logique de sécurité simplifiée ici)
    private Attachment findAttachmentByIdAndCheckAccess(Long attachmentId) {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment", "id", attachmentId));

        // --- LOGIQUE DE SÉCURITÉ PRIMITIVE ---
        // À remplacer par des appels à SecurityService utilisant l'utilisateur authentifié
        boolean canAccess = false;
        // Exemple : Si l'utilisateur est admin/recruteur OU s'il est le propriétaire
        // Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        // if (securityService.isAdminOrRecruiter(principal)) {
        //      canAccess = true;
        // } else if (attachment.getOwner() != null && securityService.isCandidateOwner(attachment.getOwner().getId(), principal)) {
        //      canAccess = true;
        // } else if (attachment.getJobApplication() != null && securityService.isApplicationOwner(attachment.getJobApplication().getId(), principal)) {
        //      canAccess = true;
        // }

        // --- Simulation temporaire SANS sécurité ---
        canAccess = true; // !! ATTENTION: À REMPLACER PAR UNE VRAIE VÉRIFICATION !!

        if (!canAccess) {
            log.warn("Access denied for attachment ID: {}", attachmentId);
            throw new AccessDeniedException("Access denied to attachment " + attachmentId);
        }
        return attachment;
    }

    // --- Mapping Helper ---
    private AttachmentDTO convertToDto(Attachment attachment) {
        if (attachment == null) return null;

        AttachmentDTO dto = new AttachmentDTO(
                attachment.getId(),
                attachment.getFileName(),
                attachment.getFileType(),
                attachment.getFileSize(),
                attachment.getUploadedAt(),
                attachment.getOwner() != null ? attachment.getOwner().getId() : null,
                attachment.getJobApplication() != null ? attachment.getJobApplication().getId() : null
        );

        // Construire l'URL de téléchargement
        // IMPORTANT: Ceci suppose que le ResourceHandler est configuré pour servir "/uploads/**"
        // et que le `filePath` stocké est relatif à la base d'upload (ex: "candidates/1/cv.pdf")
        String downloadUri = ServletUriComponentsBuilder.fromCurrentContextPath() // Obtient http://localhost:8080
                .path("/uploads/") // Le préfixe configuré dans ResourceHandler
                .path(attachment.getFilePath().replace("\\", "/")) // Ajoute le chemin stocké (assure les slashs)
                .toUriString();
        dto.setDownloadUrl(downloadUri);

        return dto;
    }

    // Pas besoin de convertToEntity ici car l'entité est créée dans les méthodes store...
}