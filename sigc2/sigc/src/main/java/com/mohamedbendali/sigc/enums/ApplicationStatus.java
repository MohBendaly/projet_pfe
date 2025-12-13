package com.mohamedbendali.sigc.enums;

public enum ApplicationStatus {
    RECEIVED,           // Reçue
    UNDER_REVIEW,       // En cours d'examen
    INTERVIEW_SCHEDULED,// Entretien planifié
    INTERVIEW_COMPLETED,// Entretien terminé
    ASSESSMENT_SENT,    // Test technique envoyé
    ASSESSMENT_PASSED,  // Test technique réussi
    OFFER_EXTENDED,     // Offre proposée
    ACCEPTED,           // Acceptée par le candidat
    REJECTED,           // Refusée (par l'entreprise ou le candidat)
    WITHDRAWN           // Retirée par le candidat
}