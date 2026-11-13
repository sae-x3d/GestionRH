package com.gestion.enums;

public enum TypeJourPlanning {

    TRAVAIL("Travail"),
    REPOS("Repos"),
    CONGE("Conge"),
    FERIE("Ferie");

    private final String libelle;

    TypeJourPlanning(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
