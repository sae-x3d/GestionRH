package com.gestion.enums;

public enum StatutAnomalie {

    OUVERTE("Ouverte"),
    JUSTIFIEE("Justifiee"),
    VALIDEE("Validee");

    private final String libelle;

    StatutAnomalie(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
