package com.gestion.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Transforme les erreurs de l'API en réponses JSON homogènes.
 * Limité aux contrôleurs REST : les pages web gardent leur gestion d'erreurs.
 */
@RestControllerAdvice(basePackages = "com.gestion.controller.api")
public class ApiExceptionHandler {

    /** Échec de @Valid sur le corps : un message par champ. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleBeanValidation(MethodArgumentNotValidException ex) {
        Map<String, String> messages = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> messages.putIfAbsent(e.getField(), e.getDefaultMessage()));
        return badRequest(messages);
    }

    /** JSON malformé, type incorrect (ex. budget = "abc"), date illisible. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex) {
        Map<String, String> messages = new LinkedHashMap<>();
        messages.put("corps", "Corps JSON illisible : vérifiez la syntaxe, les types "
                + "(entiers pour les identifiants et le budget) et le format des dates (yyyy-MM-ddTHH:mm:ss).");
        return badRequest(messages);
    }

    /** Règles vérifiées par le service (référence inconnue, date passée...). */
    @ExceptionHandler(ApiValidationException.class)
    public ResponseEntity<ApiError> handleBusinessValidation(ApiValidationException ex) {
        return badRequest(ex.getErreurs());
    }

    private ResponseEntity<ApiError> badRequest(Map<String, String> messages) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status)
                .body(new ApiError(status.value(), status.getReasonPhrase(), messages));
    }
}
