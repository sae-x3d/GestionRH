package com.gestion.enums;

public enum StatutPeriodePaie {

    OUVERTE("Ouverte"),
    CLOTUREE("Cloturee");

    private final String libelle;

    StatutPeriodePaie(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
