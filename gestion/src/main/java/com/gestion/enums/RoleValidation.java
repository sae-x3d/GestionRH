package com.gestion.enums;

public enum RoleValidation {

    MANAGER("Manager"),
    RH("RH"),
    DIRECTION("Direction");

    private final String libelle;

    RoleValidation(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
