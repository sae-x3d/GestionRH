package com.gestion.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ParametresContrat {

    private final String employeurNom;
    private final String employeurAdresse;
    private final String lieuSignature;
    private final String dureeTravail;
    private final String categorieProfessionnelle;
    private final String indiceClassification;

    public ParametresContrat(
            @Value("${app.contrat.employeur-nom:}") String employeurNom,
            @Value("${app.contrat.employeur-adresse:}") String employeurAdresse,
            @Value("${app.contrat.lieu-signature:Antananarivo}") String lieuSignature,
            @Value("${app.contrat.duree-travail:}") String dureeTravail,
            @Value("${app.contrat.categorie-professionnelle:}") String categorieProfessionnelle,
            @Value("${app.contrat.indice-classification:}") String indiceClassification) {
        this.employeurNom = employeurNom;
        this.employeurAdresse = employeurAdresse;
        this.lieuSignature = lieuSignature;
        this.dureeTravail = dureeTravail;
        this.categorieProfessionnelle = categorieProfessionnelle;
        this.indiceClassification = indiceClassification;
    }

    public String getEmployeurNom() {
        return employeurNom;
    }

    public String getEmployeurAdresse() {
        return employeurAdresse;
    }

    public String getLieuSignature() {
        return lieuSignature;
    }

    public String getDureeTravail() {
        return dureeTravail;
    }

    public String getCategorieProfessionnelle() {
        return categorieProfessionnelle;
    }

    public String getIndiceClassification() {
        return indiceClassification;
    }
}
