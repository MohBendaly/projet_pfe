package com.mohamedbendali.sigc.entity;

import com.mohamedbendali.sigc.enums.ApplicationStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "job_applications")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false) // Le statut est généralement requis
    private ApplicationStatus status = ApplicationStatus.RECEIVED; // Valeur par défaut

    @ManyToOne(fetch = FetchType.LAZY, optional = false) // optional=false rend la relation obligatoire
    @JoinColumn(name = "candidate_id", nullable = false)
    private Candidate candidate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "offer_id", nullable = false)
    private JobOffer jobOffer;

    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Interview> interviews = new ArrayList<>();

    // Relation ajoutée basée sur le chapitre 5
    @OneToMany(mappedBy = "jobApplication", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Attachment> attachments = new ArrayList<>();

    @Lob // Pour texte potentiellement long
    private String coverLetter; // Ajouté du détail chapitre 5

    @Column(updatable = false, nullable = false)
    @CreationTimestamp
    private LocalDateTime applicationDate;

    private LocalDateTime updatedAt;

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}