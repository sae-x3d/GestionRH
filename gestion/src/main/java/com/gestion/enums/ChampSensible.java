package com.gestion.enums;

public enum ChampSensible {

    RIB("RIB"),
    ADRESSE("Adresse"),
    TELEPHONE("Telephone"),
    ETAT_CIVIL("Etat civil"),
    PIECE_IDENTITE("Piece d'identite"),
    CONTACT_URGENCE("Contact d'urgence");

    private final String libelle;

    ChampSensible(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
