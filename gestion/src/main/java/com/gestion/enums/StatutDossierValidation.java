package com.gestion.enums;

public enum StatutDossierValidation {

    EN_COURS("En cours"),
    APPROUVE("Approuve"),
    REFUSE("Refuse"),
    ANNULE("Annule");

    private final String libelle;

    StatutDossierValidation(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
