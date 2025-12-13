package com.mohamedbendali.sigc.repository;

import com.mohamedbendali.sigc.entity.Interview;
import com.mohamedbendali.sigc.enums.InterviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InterviewRepository extends JpaRepository<Interview, Long> {

    // Trouver les entretiens pour une candidature spécifique
    List<Interview> findByApplicationId(Long applicationId);

    // Trouver les entretiens par statut pour une candidature
    List<Interview> findByApplicationIdAndStatus(Long applicationId, InterviewStatus status);

    // NOUVELLE METHODE AJOUTÉE
    @Query("SELECT i FROM Interview i " +
            "LEFT JOIN FETCH i.application a " +
            "LEFT JOIN FETCH a.jobOffer " +
            "LEFT JOIN FETCH a.candidate " +
            "WHERE i.id = :interviewId")
    Optional<Interview> findByIdWithDetails(@Param("interviewId") Long interviewId);
}