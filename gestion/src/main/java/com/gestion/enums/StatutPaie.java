package com.gestion.enums;

public enum StatutPaie {

    NON_INSCRIT("Non inscrit"),
    EN_ATTENTE("En attente"),
    INSCRIT("Inscrit");

    private final String libelle;

    StatutPaie(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
