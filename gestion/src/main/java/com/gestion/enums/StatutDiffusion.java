package com.gestion.enums;

public enum StatutDiffusion {

    PLANIFIEE("Planifiee"),
    PUBLIEE("Publiee"),
    CLOTUREE("Cloturee");

    private final String libelle;

    StatutDiffusion(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
