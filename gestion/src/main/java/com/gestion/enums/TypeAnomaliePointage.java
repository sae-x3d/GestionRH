package com.gestion.enums;

public enum TypeAnomaliePointage {

    RETARD("Retard"),
    DEPART_ANTICIPE("Depart anticipe"),
    ABSENCE("Absence"),
    POINTAGE_INCOMPLET("Pointage incomplet"),
    HEURES_MANQUANTES("Heures manquantes"),
    DEPASSEMENT_PAUSE("Depassement de pause");

    private final String libelle;

    TypeAnomaliePointage(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
