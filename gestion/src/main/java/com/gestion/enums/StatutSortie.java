package com.gestion.enums;

public enum StatutSortie {

    EN_COURS("En cours"),
    VALIDEE("Validee"),
    ANNULEE("Annulee");

    private final String libelle;

    StatutSortie(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
