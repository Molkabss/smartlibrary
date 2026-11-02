package com.smartlibrary.smartlibrary.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Centralise la gestion des erreurs pour TOUS les controleurs REST.
 * Sans cette classe, une exception non geree renverrait une page d'erreur
 * HTML par defaut de Spring - ici, on garantit que toute erreur revient
 * en JSON propre, avec le bon code HTTP.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Ressource introuvable (auteur, genre, editeur...) -> 404
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> gererRessourceIntrouvable(ResourceNotFoundException ex) {
        return construireErreur(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    // Parametre de mauvais type, ex: /membres/multi-emprunts?min=abc -> 400
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> gererParametreInvalide(MethodArgumentTypeMismatchException ex) {
        return construireErreur(HttpStatus.BAD_REQUEST,
                "Parametre invalide : '" + ex.getName() + "' attend un type different.");
    }

    // Filet de securite pour toute autre erreur inattendue -> 500
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> gererErreurGenerale(Exception ex) {
        return construireErreur(HttpStatus.INTERNAL_SERVER_ERROR,
                "Une erreur interne est survenue : " + ex.getMessage());
    }

    private ResponseEntity<Map<String, Object>> construireErreur(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}
