package com.mohamedbendali.sigc.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentDTO {

    private Long id;
    private String fileName; // Nom original
    private String fileType; // ContentType (MIME type)
    private long fileSize; // Taille en octets
    private LocalDateTime uploadedAt;
    // IDs des entités parentes possibles
    private Long ownerId; // ID du Candidate si lié au candidat
    private Long jobApplicationId; // ID de JobApplication si lié à l'application

    // URL pour télécharger le fichier
    private String downloadUrl;

    // Constructeur sans downloadUrl (il sera ajouté par le service)
    public AttachmentDTO(Long id, String fileName, String fileType, long fileSize, LocalDateTime uploadedAt, Long ownerId, Long jobApplicationId) {
        this.id = id;
        this.fileName = fileName;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.uploadedAt = uploadedAt;
        this.ownerId = ownerId;
        this.jobApplicationId = jobApplicationId;
    }
}