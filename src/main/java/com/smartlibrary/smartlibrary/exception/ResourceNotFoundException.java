package com.smartlibrary.smartlibrary.exception;

/**
 * Levee quand une ressource demandee (auteur, genre, editeur...) n'existe
 * pas dans le graphe RDF. Interceptee par GlobalExceptionHandler pour
 * renvoyer un code HTTP 404 avec un message JSON clair.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
