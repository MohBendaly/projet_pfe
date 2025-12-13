package com.mohamedbendali.sigc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SkillDTO {

    private Long id;

    @NotBlank(message = "Le nom de la compétence est obligatoire")
    @Size(max = 100, message = "Le nom ne peut dépasser 100 caractères")
    private String name;

    @Size(max = 50, message = "La catégorie ne peut dépasser 50 caractères")
    private String category; // Ex: Langage, Framework, Outil, Soft Skill

    // Pas de proficiencyLevel ici, car il dépend du contexte (offre ou candidat)
    // Pas de listes de candidats/offres ici pour garder le DTO simple
}