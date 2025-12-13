package com.mohamedbendali.sigc.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "attachments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Attachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String fileName; // Nom original du fichier

    @NotBlank
    @Column(nullable = false)
    private String filePath; // Chemin de stockage (relatif ou absolu) ou identifiant cloud

    @NotBlank
    @Column(nullable = false)
    private String fileType; // ContentType (e.g., application/pdf)

    private long fileSize; // Taille en octets

    // Un document peut appartenir soit à un candidat (profil général)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id") // Nommé 'owner' dans le texte, mais 'candidate_id' est plus clair
    private Candidate owner;

    // Soit à une candidature spécifique (lettre de motivation pour CETTE offre)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id")
    private JobApplication jobApplication;

    @CreationTimestamp
    @Column(updatable = false, nullable = false)
    private LocalDateTime uploadedAt;
}