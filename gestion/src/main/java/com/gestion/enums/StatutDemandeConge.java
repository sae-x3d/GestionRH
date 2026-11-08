package com.gestion.enums;

public enum StatutDemandeConge {

    BROUILLON("Brouillon"),
    SOUMISE("Soumise"),
    VALIDEE_MANAGER("Validee manager"),
    ATTENTE_DIRECTION("En attente Direction"),
    VALIDEE_RH("Validee RH"),
    REFUSEE("Refusee"),
    ANNULEE("Annulee");

    private final String libelle;

    StatutDemandeConge(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }

    public boolean estActive() {
        return this == SOUMISE || this == VALIDEE_MANAGER || this == VALIDEE_RH;
    }
}
