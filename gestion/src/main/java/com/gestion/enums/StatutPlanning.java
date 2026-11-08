package com.gestion.enums;

public enum StatutPlanning {

    BROUILLON("Brouillon"),
    PUBLIE("Publie");

    private final String libelle;

    StatutPlanning(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
