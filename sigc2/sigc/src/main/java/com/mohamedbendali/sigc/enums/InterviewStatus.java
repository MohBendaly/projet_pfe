package com.mohamedbendali.sigc.enums;

public enum InterviewStatus {
    SCHEDULED,      // Planifié
    IN_PROGRESS,    // En cours (chatbot ou live)
    COMPLETED,      // Terminé
    CANCELLED,      // Annulé (par le candidat ou le recruteur)
    PENDING_FEEDBACK // En attente de retour/évaluation
}