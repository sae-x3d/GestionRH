package com.gestion.enums;

public enum StatutValidation {

    EN_ATTENTE("En attente"),
    APPROUVE("Approuve"),
    REFUSE("Refuse");

    private final String libelle;

    StatutValidation(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
