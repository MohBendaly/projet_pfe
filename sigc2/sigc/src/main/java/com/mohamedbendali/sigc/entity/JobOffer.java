package com.mohamedbendali.sigc.entity;

import com.mohamedbendali.sigc.enums.OfferStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "job_offers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobOffer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 200)
    @Column(nullable = false)
    private String title;

    @Lob // Utiliser @Lob pour les textes longs (description)
    @Column(columnDefinition = "TEXT") // Préciser le type SQL si nécessaire
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OfferStatus status = OfferStatus.DRAFT; // Valeur par défaut

    @ElementCollection(fetch = FetchType.LAZY)
    // Peut être LAZY
    @CollectionTable(name = "offer_requirements", joinColumns = @JoinColumn(name = "offer_id"))
    @Column(name = "requirement")
    private List<String> requirements;
    // Simple liste de strings comme dans l'exemple

    @OneToMany(mappedBy = "jobOffer", cascade = CascadeType.PERSIST, fetch = FetchType.LAZY) // Attention au cascade ici, ne pas supprimer les candidatures si l'offre est supprimée ?
    private List<JobApplication> applications = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY) // Relation de Chapitre 5
    @JoinTable(
            name = "offer_skills",
            joinColumns = @JoinColumn(name = "offer_id"),
            inverseJoinColumns = @JoinColumn(name = "skill_id")
    )
    private Set<Skill> requiredSkills = new HashSet<>();
    // Champs additionnels mentionnés dans le détail chapitre 5
    private String salaryRange;
    private LocalDateTime publicationDate; // Date de publication réelle
    private LocalDateTime expirationDate;

    @CreationTimestamp
    @Column(updatable = false, nullable = false)
    private LocalDateTime createdAt; // Date de création de l'enregistrement
    @Column
    private LocalDateTime updatedAt;

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}