package com.mohamedbendali.sigc.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode; // Important pour ManyToMany
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "skills")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"candidates", "offers"}) // Éviter la récursion dans equals/hashCode
public class Skill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, unique = true) // Nom de compétence unique
    private String name;

    private String category; // Ex: Langage, Framework, Outil, Soft Skill
    private String proficiencyLevel; // Optionnel, pourrait être dans la table de jointure

    // Relations ManyToMany (mappedBy indique que l'autre côté gère la table de jointure)
    @ManyToMany(mappedBy = "skills", fetch = FetchType.LAZY)
    private Set<Candidate> candidates = new HashSet<>();

    @ManyToMany(mappedBy = "requiredSkills", fetch = FetchType.LAZY)
    private Set<JobOffer> offers = new HashSet<>();
}