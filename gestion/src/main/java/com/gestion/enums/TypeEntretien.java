package com.gestion.enums;

public enum TypeEntretien {

    ENTRETIEN("Entretien"),
    TEST("Test");

    private final String libelle;

    TypeEntretien(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
