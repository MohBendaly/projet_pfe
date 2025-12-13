package com.mohamedbendali.sigc.controller;

import com.mohamedbendali.sigc.dto.SkillDTO;
import com.mohamedbendali.sigc.service.SkillService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/skills")
@RequiredArgsConstructor
public class SkillController {

    private final SkillService skillService;

    // Lister toutes les compétences (Admin/Recruteur)
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RECRUITER')")
    public ResponseEntity<Page<SkillDTO>> getAllSkills(
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return ResponseEntity.ok(skillService.findAll(pageable));
    }

    // Rechercher des compétences par nom (Admin/Recruteur)
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECRUITER')")
    public ResponseEntity<List<SkillDTO>> searchSkills(@RequestParam String name) {
        return ResponseEntity.ok(skillService.searchSkills(name));
    }


    // Obtenir une compétence par ID (Admin/Recruteur)
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECRUITER')")
    public ResponseEntity<SkillDTO> getSkillById(@PathVariable Long id) {
        return skillService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Créer une nouvelle compétence (Admin seulement)
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SkillDTO> createSkill(@Valid @RequestBody SkillDTO skillDto) {
        // Assurer que l'ID n'est pas fourni
        skillDto.setId(null);
        SkillDTO createdSkill = skillService.createSkill(skillDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdSkill);
    }

    // Mettre à jour une compétence (Admin seulement)
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SkillDTO> updateSkill(@PathVariable Long id, @Valid @RequestBody SkillDTO skillDto) {
        SkillDTO updatedSkill = skillService.updateSkill(id, skillDto);
        return ResponseEntity.ok(updatedSkill);
    }

    // Supprimer une compétence (Admin seulement)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteSkill(@PathVariable Long id) {
        skillService.deleteSkill(id);
        return ResponseEntity.noContent().build();
    }
    // ----> AJOUTER OU VÉRIFIER CETTE MÉTHODE <----
    /**
     * Récupère la liste complète de toutes les compétences.
     * Utilisé par le frontend pour peupler les listes déroulantes/multiselect.
     * Sécurisé pour Admin/Recruteur.
     */
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECRUITER')") // Ou juste ADMIN selon besoin
    public ResponseEntity<List<SkillDTO>> getAllSkillsList() {
        // Appeler une méthode de service qui retourne List<SkillDTO>
        // Vous devez créer skillService.findAllSkillsAsList()
        List<SkillDTO> skills = skillService.findAllSkillsAsList();
        return ResponseEntity.ok(skills);
    }
    // ----> FIN AJOUT/VÉRIFICATION <----
}