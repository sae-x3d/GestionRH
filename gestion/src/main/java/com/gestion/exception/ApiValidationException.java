package com.gestion.exception;

import java.util.Map;

/**
 * Données invalides détectées par la couche service (référence inconnue,
 * règle métier...). Traduite en HTTP 400 avec un message par champ.
 */
public class ApiValidationException extends RuntimeException {

    private final Map<String, String> erreurs;

    public ApiValidationException(Map<String, String> erreurs) {
        super("Données invalides : " + erreurs);
        this.erreurs = erreurs;
    }

    public Map<String, String> getErreurs() {
        return erreurs;
    }
}
