package com.mohamedbendali.sigc.controller;

import com.mohamedbendali.sigc.dto.CandidateDTO;
import com.mohamedbendali.sigc.entity.User; // Pour AuthenticationPrincipal
import com.mohamedbendali.sigc.service.CandidateService;
import com.mohamedbendali.sigc.service.UserService; // Service pour récupérer User depuis UserDetails
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/candidates")
@RequiredArgsConstructor
public class CandidateController {

    private final CandidateService candidateService;
    // private final UserService userService; // Pour lier UserDetails à User/Candidate

    // Endpoint pour récupérer le profil du candidat connecté
    @GetMapping("/me")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<CandidateDTO> getCurrentCandidateProfile(@AuthenticationPrincipal UserDetails userDetails) {
        // Logique pour trouver le Candidate lié à userDetails (par email ou username)
        // Supposons une méthode dans candidateService ou un service dédié
        // User user = userService.findByUsername(userDetails.getUsername());
        // CandidateDTO candidate = candidateService.getCandidateByUserId(user.getId());
        // --- Placeholder ---
        // Simulé pour l'exemple : récupérer par email (si email est dans UserDetails)
        CandidateDTO candidate = candidateService.getCandidateByEmail(userDetails.getUsername()); // ou getEmail() si disponible
        return ResponseEntity.ok(candidate);
    }

    // Endpoint pour qu'un recruteur/admin récupère un candidat par ID
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('RECRUITER') or hasRole('ADMIN')")
    public ResponseEntity<CandidateDTO> getCandidateById(@PathVariable Long id) {
        CandidateDTO candidate = candidateService.getCandidateById(id);
        return ResponseEntity.ok(candidate);
    }

    // Endpoint pour lister tous les candidats (Recruteur/Admin)
    @GetMapping
    @PreAuthorize("hasRole('RECRUITER') or hasRole('ADMIN')")
    public ResponseEntity<Page<CandidateDTO>> getAllCandidates(
            @PageableDefault(size = 10, sort = "lastName") Pageable pageable) {
        Page<CandidateDTO> candidates = candidateService.getAllCandidates(pageable);
        return ResponseEntity.ok(candidates);
    }

    // Endpoint pour créer un candidat (Admin? Ou lié à l'inscription User?)
    // La création de profil est souvent liée à la création du User.
    // Cet endpoint pourrait être réservé à l'admin pour créer un profil manuellement.
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')") // Ouvert si l'inscription crée aussi le profil
    public ResponseEntity<CandidateDTO> createCandidate(@Valid @RequestBody CandidateDTO dto) {
        // Assurer que l'ID n'est pas fourni dans le DTO pour la création
        dto.setId(null);
        CandidateDTO createdCandidate = candidateService.createCandidate(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdCandidate);
    }

    // Endpoint pour mettre à jour un profil candidat (Admin ou le candidat lui-même)
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @securityService.isCandidateOwner(#id, principal)") // Nécessite SecurityService
    // Alternative simple :
    // @PreAuthorize("hasRole('ADMIN')") // Laisser le candidat utiliser /me/update ?
    public ResponseEntity<CandidateDTO> updateCandidate(@PathVariable Long id, @Valid @RequestBody CandidateDTO dto) {
        // TODO: Ajouter vérification de sécurité plus fine (le candidat ne peut modifier que son profil)
        CandidateDTO updatedCandidate = candidateService.updateCandidate(id, dto);
        return ResponseEntity.ok(updatedCandidate);
    }

    // Endpoint spécifique pour que le candidat mette à jour son propre profil
    @PutMapping("/me")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<CandidateDTO> updateMyProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CandidateDTO dto) {
        // Trouver l'ID du candidat associé à userDetails
        CandidateDTO currentCandidate = candidateService.getCandidateByEmail(userDetails.getUsername()); // Placeholder
        // Assurer que le DTO ne tente pas de changer l'email ou l'ID user lié ?
        dto.setEmail(currentCandidate.getEmail()); // Forcer l'email actuel
        dto.setUserId(currentCandidate.getUserId()); // Forcer l'userId actuel
        CandidateDTO updatedCandidate = candidateService.updateCandidate(currentCandidate.getId(), dto);
        return ResponseEntity.ok(updatedCandidate);
    }


    // Endpoint pour supprimer un candidat (Admin seulement)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCandidate(@PathVariable Long id) {
        candidateService.deleteCandidate(id);
        return ResponseEntity.noContent().build();
    }

    // TODO: Ajouter des endpoints pour gérer les compétences, le CV, etc.
    // Ex: POST /api/candidates/me/resume (upload CV)
    // Ex: POST /api/candidates/me/skills (ajouter compétence)
}