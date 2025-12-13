package com.mohamedbendali.sigc.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception personnalisée pour indiquer une tentative de création d'une ressource
 * qui existe déjà (violation d'une contrainte d'unicité).
 *
 * Généralement mappée au statut HTTP 409 (Conflict).
 */
@ResponseStatus(HttpStatus.CONFLICT) // 409 Conflict est le statut le plus approprié
public class DuplicateResourceException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructeur pour créer l'exception avec des détails précis.
     * @param resourceName Nom de la ressource (ex: "User", "Candidate")
     * @param fieldName Nom du champ dupliqué (ex: "username", "email")
     * @param fieldValue Valeur du champ qui cause le conflit
     */
    public DuplicateResourceException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s avec %s '%s' existe déjà.", resourceName, fieldName, fieldValue));
    }

    // Constructeur simple avec un message personnalisé si nécessaire
    public DuplicateResourceException(String message) {
        super(message);
    }

    public DuplicateResourceException(String message, Throwable cause) {
        super(message, cause);
    }
}