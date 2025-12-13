package com.mohamedbendali.sigc.service;

import com.mohamedbendali.sigc.dto.AttachmentDTO;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AttachmentService {

    /**
     * Stocke un fichier lié à un candidat.
     * @param file Le fichier à uploader.
     * @param candidateId L'ID du candidat propriétaire.
     * @return Les métadonnées du fichier stocké.
     * @throws com.mohamedbendali.sigc.exception.FileStorageException Si une erreur de stockage survient.
     * @throws com.mohamedbendali.sigc.exception.ResourceNotFoundException Si le candidat n'est pas trouvé.
     * @throws org.springframework.security.access.AccessDeniedException Si l'utilisateur n'a pas la permission.
     */
    // ----> AJOUTER CETTE SIGNATURE <----
    AttachmentDTO storeFileForCandidate(MultipartFile file, Long candidateId);

    /**
     * Stocke un fichier lié à une candidature spécifique.
     * @param file Le fichier à uploader.
     * @param applicationId L'ID de la candidature associée.
     * @return Les métadonnées du fichier stocké.
     * @throws com.mohamedbendali.sigc.exception.FileStorageException Si une erreur de stockage survient.
     * @throws com.mohamedbendali.sigc.exception.ResourceNotFoundException Si la candidature n'est pas trouvée.
     * @throws org.springframework.security.access.AccessDeniedException Si l'utilisateur n'a pas la permission.
     */
    AttachmentDTO storeFileForApplication(MultipartFile file, Long applicationId); // Celle-ci était déjà là

    /**
     * Récupère les métadonnées d'une pièce jointe par son ID.
     * Vérifie les permissions d'accès.
     * @param attachmentId L'ID de la pièce jointe.
     * @return Le DTO de la pièce jointe.
     * @throws com.mohamedbendali.sigc.exception.ResourceNotFoundException si non trouvée.
     * @throws org.springframework.security.access.AccessDeniedException si accès refusé.
     */
    AttachmentDTO getAttachmentMetadata(Long attachmentId); // Déjà là

    /**
     * Charge le contenu d'une pièce jointe pour le téléchargement.
     * Vérifie les permissions d'accès.
     * @param attachmentId L'ID de la pièce jointe.
     * @return La ressource fichier.
     * @throws com.mohamedbendali.sigc.exception.ResourceNotFoundException si non trouvée ou illisible.
     * @throws org.springframework.security.access.AccessDeniedException si accès refusé.
     */
    Resource downloadFile(Long attachmentId); // Déjà là

    /**
     * Récupère la liste des métadonnées des pièces jointes pour un candidat.
     * Vérifie les permissions d'accès.
     * @param candidateId L'ID du candidat.
     * @return La liste des DTOs des pièces jointes.
     * @throws org.springframework.security.access.AccessDeniedException si accès refusé.
     */
    List<AttachmentDTO> getAttachmentsForCandidate(Long candidateId); // Déjà là

    /**
     * Récupère la liste des métadonnées des pièces jointes pour une candidature.
     * Vérifie les permissions d'accès.
     * @param applicationId L'ID de la candidature.
     * @return La liste des DTOs des pièces jointes.
     * @throws org.springframework.security.access.AccessDeniedException si accès refusé.
     */
    List<AttachmentDTO> getAttachmentsForApplication(Long applicationId); // Déjà là

    /**
     * Supprime une pièce jointe (fichier physique et métadonnées).
     * Vérifie les permissions d'accès.
     * @param attachmentId L'ID de la pièce jointe à supprimer.
     * @throws com.mohamedbendali.sigc.exception.ResourceNotFoundException si non trouvée.
     * @throws org.springframework.security.access.AccessDeniedException si accès refusé.
     * @throws java.lang.RuntimeException Si une erreur survient lors de la suppression.
     */
    void deleteAttachment(Long attachmentId); // Déjà là
}