package com.mohamedbendali.sigc.repository;

import com.mohamedbendali.sigc.entity.JobOffer;
import com.mohamedbendali.sigc.enums.OfferStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobOfferRepository extends JpaRepository<JobOffer, Long> {

    // Trouve les offres par statut (utile pour voir les publiées, brouillons, etc.)
    Page<JobOffer> findByStatus(OfferStatus status, Pageable pageable);

    // Recherche d'offres par titre contenant une chaîne (ignore la casse)
    Page<JobOffer> findByTitleContainingIgnoreCaseAndStatus(String title, OfferStatus status, Pageable pageable);

    // Recherche d'offres par titre OU description contenant une chaîne
    Page<JobOffer> findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCaseAndStatus(String title, String description, OfferStatus status, Pageable pageable);
}