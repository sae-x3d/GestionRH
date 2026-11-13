package com.gestion.enums;

public enum StatutChangementSensible {

    SOUMIS("Soumis"),
    VALIDE("Valide"),
    REFUSE("Refuse"),
    ANNULE("Annule");

    private final String libelle;

    StatutChangementSensible(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
