package com.mohamedbendali.sigc.controller;

import com.mohamedbendali.sigc.dto.JobOfferDTO;
import com.mohamedbendali.sigc.enums.OfferStatus;
import com.mohamedbendali.sigc.service.JobOfferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/offers")
@RequiredArgsConstructor
public class JobOfferController {

    private final JobOfferService offerService;

    // Endpoint public pour lister les offres publiées (avec pagination et recherche optionnelle)
    @GetMapping
    public ResponseEntity<Page<JobOfferDTO>> getPublishedOffers(
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        // Par défaut, on retourne les offres PUBLISHED
        Page<JobOfferDTO> offers = offerService.searchOffers(keyword, OfferStatus.PUBLISHED, pageable);
        return ResponseEntity.ok(offers);
    }

    // Endpoint pour récupérer une offre spécifique par ID (public)
    @GetMapping("/{id}")
    public ResponseEntity<JobOfferDTO> getOfferById(@PathVariable Long id) {
        JobOfferDTO offer = offerService.getOfferById(id);
        return ResponseEntity.ok(offer);
    }

    // Endpoint pour créer une offre (Recruteur uniquement)
    @PostMapping
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    public ResponseEntity<JobOfferDTO> createOffer(@Valid @RequestBody JobOfferDTO dto) {
        JobOfferDTO createdOffer = offerService.createOffer(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdOffer);
    }

    // Endpoint pour mettre à jour une offre (Recruteur uniquement)
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    public ResponseEntity<JobOfferDTO> updateOffer(@PathVariable Long id, @Valid @RequestBody JobOfferDTO dto) {
        JobOfferDTO updatedOffer = offerService.updateOffer(id, dto);
        return ResponseEntity.ok(updatedOffer);
    }

    // Endpoint pour changer le statut d'une offre (Recruteur uniquement)
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    public ResponseEntity<JobOfferDTO> updateOfferStatus(@PathVariable Long id, @RequestParam OfferStatus status) {
        JobOfferDTO updatedOffer = offerService.updateOfferStatus(id, status);
        return ResponseEntity.ok(updatedOffer);
    }


    // Endpoint pour supprimer une offre (Recruteur ou Admin - attention à la logique métier)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('RECRUITER') or hasRole('ADMIN')")
    public ResponseEntity<Void> deleteOffer(@PathVariable Long id) {
        offerService.deleteOffer(id);
        return ResponseEntity.noContent().build();
    }

    // Endpoint pour que les recruteurs voient TOUTES les offres (y compris brouillons etc)
    @GetMapping("/all")
    @PreAuthorize("hasRole('RECRUITER') or hasRole('ADMIN')")
    public ResponseEntity<Page<JobOfferDTO>> getAllOffersForRecruiter(
            @RequestParam(required = false) OfferStatus status, // Filtrer par statut optionnel
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        Page<JobOfferDTO> offers;
        if (status != null) {
            offers = offerService.getOffersByStatus(status, pageable);
        } else {
            offers = offerService.getAllOffers(pageable);
        }
        return ResponseEntity.ok(offers);
    }
}