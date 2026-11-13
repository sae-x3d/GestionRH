package com.gestion.enums;

public enum StatutHeureSup {

    EN_ATTENTE("En attente"),
    VALIDEE("Validee"),
    REFUSEE("Refusee"),
    PAYEE("Payee"),
    REPOS("Convertie en repos");

    private final String libelle;

    StatutHeureSup(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
