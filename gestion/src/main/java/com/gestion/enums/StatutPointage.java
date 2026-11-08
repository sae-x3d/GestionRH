package com.gestion.enums;

public enum StatutPointage {

    PRESENT("Present"),
    RETARD("Retard"),
    DEPART_ANTICIPE("Depart anticipe"),
    INCOMPLET("Pointage incomplet"),
    ABSENT("Absent"),
    CONGE("Conge"),
    FERIE("Ferie");

    private final String libelle;

    StatutPointage(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
