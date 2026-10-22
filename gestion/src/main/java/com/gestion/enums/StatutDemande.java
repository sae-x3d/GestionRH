package com.gestion.enums;

/**
 * Statuts d'une demande de recrutement (cahier de conception, Bloc 1) :
 * brouillon -> en validation -> validée -> refusée -> ouverte.
 *
 * Le libellé correspond à demande_status.libelle_status.
 */
public enum StatutDemande {

    BROUILLON("Brouillon"),
    EN_VALIDATION("En validation"),
    VALIDEE("Validée"),
    REFUSEE("Refusée"),
    OUVERTE("Ouverte");

    private final String libelle;

    StatutDemande(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
