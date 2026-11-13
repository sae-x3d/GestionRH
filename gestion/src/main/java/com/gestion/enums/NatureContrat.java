package com.gestion.enums;

public enum NatureContrat {

    INITIAL("Initial"),
    RENOUVELLEMENT("Renouvellement"),
    AVENANT("Avenant"),
    CONVERSION("Conversion");

    private final String libelle;

    NatureContrat(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
