package com.mohamedbendali.sigc.controller;

import com.mohamedbendali.sigc.dto.AttachmentDTO;
import com.mohamedbendali.sigc.exception.ResourceNotFoundException;
import com.mohamedbendali.sigc.service.AttachmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException; // Pour les erreurs HTTP
import org.springframework.http.HttpStatus; // Pour les statuts HTTP

import java.io.IOException; // Pour les exceptions potentielles de lecture de Resource
import java.nio.file.Files;
import java.util.List;

@RestController
@RequestMapping("/api") // Préfixe général /api
@RequiredArgsConstructor // Injection via constructeur
@Slf4j
@CrossOrigin // Autoriser Cross-Origin (ou configurer globalement)
public class AttachmentController {

    private final AttachmentService attachmentService;

    // --- Upload Endpoints ---

    @PostMapping("/candidates/{candidateId}/attachments")
    // Sécurité : Le candidat lui-même ou un Admin/Recruteur peut uploader pour ce candidat
    @PreAuthorize("hasAnyRole('ADMIN', 'RECRUITER') or @securityService.isCandidateOwner(#candidateId, principal)")
    public ResponseEntity<AttachmentDTO> uploadForCandidate(
            @PathVariable Long candidateId,
            @RequestParam("file") MultipartFile file) { // Le nom "file" doit correspondre à la clé FormData
        log.info("Received file upload request for candidate ID: {}", candidateId);
        if (file.isEmpty()) {
            log.warn("Upload request for candidate {} rejected: file is empty.", candidateId);
            return ResponseEntity.badRequest().build(); // Retourner 400 si fichier vide
        }
        try {
            AttachmentDTO attachmentDto = attachmentService.storeFileForCandidate(file, candidateId);
            log.info("File successfully stored for candidate {}. Attachment ID: {}", candidateId, attachmentDto.getId());
            // Renvoyer 201 Created serait sémantiquement mieux, mais 200 OK est souvent utilisé
            return ResponseEntity.ok(attachmentDto);
        } catch (Exception e) {
            // Logguer l'erreur côté serveur est important
            log.error("Failed to store file for candidate {}: {}", candidateId, e.getMessage(), e);
            // Renvoyer une erreur 500 générique ou une erreur plus spécifique si possible
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors du stockage du fichier.", e);
        }
    }

    @PostMapping("/applications/{applicationId}/attachments")
    // Sécurité : Le propriétaire de l'application ou un Admin/Recruteur
    @PreAuthorize("hasAnyRole('ADMIN', 'RECRUITER') or @securityService.isApplicationOwner(#applicationId, principal)")
    public ResponseEntity<AttachmentDTO> uploadForApplication(
            @PathVariable Long applicationId,
            @RequestParam("file") MultipartFile file) {
        log.info("Received file upload request for application ID: {}", applicationId);
        if (file.isEmpty()) {
            log.warn("Upload request for application {} rejected: file is empty.", applicationId);
            return ResponseEntity.badRequest().build();
        }
        try {
            AttachmentDTO attachmentDto = attachmentService.storeFileForApplication(file, applicationId);
            log.info("File successfully stored for application {}. Attachment ID: {}", applicationId, attachmentDto.getId());
            return ResponseEntity.ok(attachmentDto);
        } catch (Exception e) {
            log.error("Failed to store file for application {}: {}", applicationId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors du stockage du fichier.", e);
        }
    }

    // --- Download & Metadata Endpoints ---

    // Obtenir seulement les métadonnées
    @GetMapping("/attachments/{attachmentId}/metadata")
    // Sécurité : Vérifiée dans le service via findAttachmentByIdAndCheckAccess
    @PreAuthorize("@securityService.isAttachmentOwner(#attachmentId, principal) or hasAnyRole('ADMIN', 'RECRUITER')") // Double sécurité
    public ResponseEntity<AttachmentDTO> getAttachmentMetadata(@PathVariable Long attachmentId) {
        log.debug("Request for metadata for attachment ID: {}", attachmentId);
        // Le service lèvera une exception (NotFound ou AccessDenied) si problème
        AttachmentDTO dto = attachmentService.getAttachmentMetadata(attachmentId);
        return ResponseEntity.ok(dto);
    }

    // Télécharger le fichier
    @GetMapping("/attachments/{attachmentId}/download")
    // Sécurité : Vérifiée dans le service via findAttachmentByIdAndCheckAccess
    @PreAuthorize("@securityService.isAttachmentOwner(#attachmentId, principal) or hasAnyRole('ADMIN', 'RECRUITER')") // Double sécurité
    public ResponseEntity<Resource> downloadFile(@PathVariable Long attachmentId) {
        log.info("Request to download attachment ID: {}", attachmentId);
        try {
            // Le service récupère le fichier et vérifie l'accès
            Resource resource = attachmentService.downloadFile(attachmentId);
            // Récupérer les métadonnées pour définir Content-Type et Content-Disposition
            AttachmentDTO metadata = attachmentService.getAttachmentMetadata(attachmentId); // Le service re-vérifie l'accès

            // Essayer de déterminer le type MIME si possible
            String contentType = metadata.getFileType();
            if (contentType == null || contentType.isBlank() || contentType.equals("application/octet-stream")) {
                // Tenter une détection basique si type inconnu (optionnel)
                try {
                    contentType = Files.probeContentType(resource.getFile().toPath());
                } catch (IOException | UnsupportedOperationException e) {
                    log.warn("Could not determine content type for attachment {}, falling back to octet-stream", attachmentId);
                    contentType = "application/octet-stream"; // Type binaire générique par défaut
                }
            }
            log.debug("Serving attachment ID {} with Content-Type: {}", attachmentId, contentType);


            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    // Header pour forcer le téléchargement avec le nom original
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + metadata.getFileName() + "\"")
                    // Ajouter la taille si connue (optionnel mais recommandé)
                    .contentLength(metadata.getFileSize())
                    .body(resource);

        } catch (ResourceNotFoundException e) {
            log.warn("Download failed for attachment ID {}: {}", attachmentId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (AccessDeniedException e) {
            log.warn("Access denied for download attachment ID {}: {}", attachmentId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage(), e);
        } catch (Exception e) { // Autres erreurs potentielles (IO, etc.)
            log.error("Error during download for attachment ID {}: {}", attachmentId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors du téléchargement du fichier.", e);
        }
    }

    // --- Listing Endpoints ---

    @GetMapping("/candidates/{candidateId}/attachments")
    // Sécurité : Le candidat lui-même ou Admin/Recruteur
    @PreAuthorize("hasAnyRole('ADMIN', 'RECRUITER') or @securityService.isCandidateOwner(#candidateId, principal)")
    public ResponseEntity<List<AttachmentDTO>> getAttachmentsForCandidate(@PathVariable Long candidateId) {
        log.debug("Request for attachments for candidate ID: {}", candidateId);
        List<AttachmentDTO> attachments = attachmentService.getAttachmentsForCandidate(candidateId);
        return ResponseEntity.ok(attachments);
    }

    @GetMapping("/applications/{applicationId}/attachments")
    // Sécurité : Le propriétaire de l'application ou Admin/Recruteur
    @PreAuthorize("hasAnyRole('ADMIN', 'RECRUITER') or @securityService.isApplicationOwner(#applicationId, principal)")
    public ResponseEntity<List<AttachmentDTO>> getAttachmentsForApplication(@PathVariable Long applicationId) {
        log.debug("Request for attachments for application ID: {}", applicationId);
        List<AttachmentDTO> attachments = attachmentService.getAttachmentsForApplication(applicationId);
        return ResponseEntity.ok(attachments);
    }

    // --- Deletion Endpoint ---

    @DeleteMapping("/attachments/{attachmentId}")
    // Sécurité : Vérifiée dans le service
    @PreAuthorize("@securityService.isAttachmentOwner(#attachmentId, principal) or hasAnyRole('ADMIN', 'RECRUITER')") // Double sécurité
    public ResponseEntity<Void> deleteAttachment(@PathVariable Long attachmentId) {
        log.info("Request to delete attachment ID: {}", attachmentId);
        try {
            attachmentService.deleteAttachment(attachmentId);
            return ResponseEntity.noContent().build(); // 204 No Content on successful deletion
        } catch (ResourceNotFoundException e) {
            log.warn("Delete failed for attachment ID {}: {}", attachmentId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (AccessDeniedException e) {
            log.warn("Access denied for delete attachment ID {}: {}", attachmentId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error during deletion for attachment ID {}: {}", attachmentId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors de la suppression du fichier.", e);
        }
    }
}
