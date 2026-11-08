package com.gestion.enums;

public enum TypeDiffusion {

    SITE("Site carriere"),
    RESEAUX_SOCIAUX("Reseaux sociaux"),
    CABINET("Cabinet de recrutement"),
    AUTRE("Autre");

    private final String libelle;

    TypeDiffusion(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
