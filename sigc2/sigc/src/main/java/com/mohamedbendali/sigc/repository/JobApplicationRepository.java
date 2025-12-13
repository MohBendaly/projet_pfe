package com.mohamedbendali.sigc.repository;

import com.mohamedbendali.sigc.entity.JobApplication;
import com.mohamedbendali.sigc.enums.ApplicationStatus;
// Imports nécessaires pour Page et Pageable
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {

    // Trouve les candidatures pour un candidat spécifique
    List<JobApplication> findByCandidateId(Long candidateId);

    // Trouve les candidatures pour une offre spécifique (AVEC PAGINATION)
    // MODIFIEZ CETTE LIGNE : changez le type de retour en Page<> et ajoutez Pageable
    Page<JobApplication> findByJobOfferId(Long jobOfferId, Pageable pageable);

    // Trouve les candidatures par statut
    Page<JobApplication> findByStatus(ApplicationStatus status, Pageable pageable);

    // Exemple de requête custom comme dans le document, mais adaptée
    // Trouve les candidatures pour les offres actuellement publiées
    @Query("SELECT a FROM JobApplication a JOIN a.jobOffer o WHERE o.status = com.mohamedbendali.sigc.enums.OfferStatus.PUBLISHED")
    Page<JobApplication> findApplicationsForPublishedOffers(Pageable pageable);

    // Compter les candidatures par statut pour une offre donnée
    long countByJobOfferIdAndStatus(Long jobOfferId, ApplicationStatus status);

    // Optionnel mais utile : vérifier si un candidat a déjà postulé
    // boolean existsByCandidateIdAndJobOfferId(Long candidateId, Long jobOfferId);
}