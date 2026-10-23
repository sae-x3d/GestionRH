package com.gestion.exception;

import jakarta.validation.ConstraintViolationException;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                 HttpHeaders headers,
                                                                 HttpStatusCode status,
                                                                 WebRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        ex.getBindingResult().getGlobalErrors()
                .forEach(error -> fieldErrors.putIfAbsent(error.getObjectName(), error.getDefaultMessage()));

        return new ResponseEntity<>(badRequest(fieldErrors), headers, status);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
                                                                            HttpHeaders headers,
                                                                            HttpStatusCode status,
                                                                            WebRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (ParameterValidationResult result : ex.getParameterValidationResults()) {
            String parameterName = result.getMethodParameter().getParameterName();
            for (MessageSourceResolvable error : result.getResolvableErrors()) {
                fieldErrors.putIfAbsent(parameterName, error.getDefaultMessage());
            }
        }

        return new ResponseEntity<>(badRequest(fieldErrors), headers, status);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getConstraintViolations()
                .forEach(violation -> fieldErrors.putIfAbsent(violation.getPropertyPath().toString(),
                        violation.getMessage()));

        return ResponseEntity.badRequest().body(badRequest(fieldErrors));
    }

    @ExceptionHandler(RessourceIntrouvableException.class)
    public ResponseEntity<ApiError> handleRessourceIntrouvable(RessourceIntrouvableException ex) {
        ApiError body = new ApiError(HttpStatus.NOT_FOUND.value(), ex.getMessage(), Map.of());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(AccesRefuseException.class)
    public ResponseEntity<ApiError> handleAccesRefuse(AccesRefuseException ex) {
        ApiError body = new ApiError(HttpStatus.FORBIDDEN.value(), ex.getMessage(), Map.of());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(DecisionRhImpossibleException.class)
    public ResponseEntity<ApiError> handleDecisionRhImpossible(DecisionRhImpossibleException ex) {
        ApiError body = new ApiError(HttpStatus.CONFLICT.value(), ex.getMessage(), Map.of());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    private ApiError badRequest(Map<String, String> fieldErrors) {
        return new ApiError(HttpStatus.BAD_REQUEST.value(), "Erreur de validation", fieldErrors);
    }
}
