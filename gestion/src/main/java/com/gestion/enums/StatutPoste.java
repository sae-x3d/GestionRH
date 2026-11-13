package com.gestion.enums;

public enum StatutPoste {

    VACANT("Vacant"),
    OCCUPE("Occupe"),
    GELE("Gele");

    private final String libelle;

    StatutPoste(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
