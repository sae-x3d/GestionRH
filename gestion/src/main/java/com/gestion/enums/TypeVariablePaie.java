package com.gestion.enums;

public enum TypeVariablePaie {

    HEURE_SUP("Heures supplementaires"),
    ABSENCE("Absence"),
    PRIME("Prime"),
    RECUPERATION("Recuperation");

    private final String libelle;

    TypeVariablePaie(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
