package com.gestion.enums;

public enum TypeObjetValidation {

    CONTRAT("Contrat"),
    ACCREDITATION("Accreditation"),
    ONBOARDING("Onboarding");

    private final String libelle;

    TypeObjetValidation(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
