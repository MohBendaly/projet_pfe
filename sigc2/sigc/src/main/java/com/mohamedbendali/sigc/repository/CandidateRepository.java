package com.mohamedbendali.sigc.repository;

import com.mohamedbendali.sigc.entity.Candidate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CandidateRepository extends JpaRepository<Candidate, Long> {

    // Trouver un candidat par email (utile pour l'unicité et la connexion)
    Optional<Candidate> findByEmailIgnoreCase(String email);

    // Trouver un candidat par son ID utilisateur lié
    Optional<Candidate> findByUserId(Long userId);
}