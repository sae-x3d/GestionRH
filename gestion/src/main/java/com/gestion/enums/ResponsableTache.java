package com.gestion.enums;

public enum ResponsableTache {

    IT("IT"),
    RH("RH"),
    MANAGER("Manager"),
    SERVICES_GENERAUX("Services generaux"),
    EMPLOYE("Employe");

    private final String libelle;

    ResponsableTache(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
