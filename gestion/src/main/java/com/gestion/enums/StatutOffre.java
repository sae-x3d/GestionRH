package com.gestion.enums;

public enum StatutOffre {

    BROUILLON("Brouillon"),
    PUBLIEE("Publiee"),
    CLOTUREE("Cloturee");

    private final String libelle;

    StatutOffre(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
