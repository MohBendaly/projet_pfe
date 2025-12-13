package com.mohamedbendali.sigc.entity;

import com.mohamedbendali.sigc.enums.ApplicationStatus; // Assurez-vous que cet enum existe si utilisé directement ici
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp; // Gardé de l'exemple même si pas dans le détail de Candidate
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "candidates")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Candidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 100)
    private String firstName;

    @NotBlank
    @Size(max = 100)
    private String lastName;

    @NotBlank // Email est généralement requis
    @Email
    @Size(max = 100) // Ajouté pour cohérence
    @Column(unique = true, nullable = false) // Rendu non null et unique
    private String email;

    @Pattern(regexp = "^\\+?[0-9.\\-\\s()]+$", message = "Format de téléphone invalide") // Regex améliorée
    @Size(max = 20) // Ajouté pour limiter la taille
    private String phone;

    // Relations basées sur Chapitre 2 et 5

    @OneToMany(mappedBy = "candidate", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<JobApplication> applications = new ArrayList<>();

    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Attachment> documents = new ArrayList<>(); // Chapitre 5

    @OneToOne(fetch = FetchType.LAZY) // Chapitre 5
    @JoinColumn(name = "user_id", referencedColumnName = "id") // Nom de colonne standard
    private User user;

    @ManyToMany(fetch = FetchType.LAZY) // Chapitre 5
    @JoinTable(
            name = "candidate_skills",
            joinColumns = @JoinColumn(name = "candidate_id"),
            inverseJoinColumns = @JoinColumn(name = "skill_id")
    )
    private Set<Skill> skills = new HashSet<>();

    // Champs additionnels mentionnés implicitement ou dans le texte
    private String resumePath; // Chemin vers le CV (mentionné dans le détail)

    @CreationTimestamp // Date de création du profil candidat
    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt; // Date de mise à jour

    @PreUpdate // Met à jour automatiquement avant la mise à jour en BDD
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}