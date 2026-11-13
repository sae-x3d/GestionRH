package com.gestion.enums;

public enum TypeSortie {

    DEMISSION("Demission"),
    FIN_CONTRAT("Fin de contrat"),
    LICENCIEMENT("Licenciement"),
    RETRAITE("Retraite"),
    RUPTURE_ESSAI("Rupture de periode d'essai");

    private final String libelle;

    TypeSortie(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
