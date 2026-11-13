package com.gestion.enums;

public enum TypeMouvementCarriere {

    EMBAUCHE("Embauche"),
    PROMOTION("Promotion"),
    MUTATION("Mutation"),
    AFFECTATION("Affectation"),
    SORTIE("Sortie");

    private final String libelle;

    TypeMouvementCarriere(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
