package com.mohamedbendali.sigc.enums;

public enum OfferStatus {
    DRAFT,      // Brouillon, non visible
    PUBLISHED,  // Publiée, visible et accepte les candidatures
    CLOSED,     // Fermée, n'accepte plus de nouvelles candidatures mais reste visible
    ARCHIVED,   // Archivée, non visible
    FILLED      // Pourvue
}