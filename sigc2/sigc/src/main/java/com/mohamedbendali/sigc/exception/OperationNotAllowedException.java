package com.mohamedbendali.sigc.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception personnalisée pour indiquer qu'une opération demandée n'est pas permise
 * en raison de l'état actuel de l'application ou d'une violation de règle métier.
 *
 * Peut être mappée à un statut HTTP 400 (Bad Request) ou 403 (Forbidden)
 * ou 409 (Conflict) dans le GlobalExceptionHandler, selon le contexte.
 * Par défaut, ResponseStatus(HttpStatus.BAD_REQUEST) est un bon point de départ.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST) // Ou HttpStatus.FORBIDDEN si c'est lié aux permissions métier
public class OperationNotAllowedException extends RuntimeException {

    private static final long serialVersionUID = 1L; // Bonne pratique pour les exceptions sérialisables

    public OperationNotAllowedException(String message) {
        super(message);
    }

    public OperationNotAllowedException(String message, Throwable cause) {
        super(message, cause);
    }
}