package com.gestion.service;

import com.gestion.model.Contrat;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ContratPdfServiceTests {

    private final ContratPdfService service = new ContratPdfService();

    @Test
    void genereUnPdfLisibleAvecLesMentionsDuContrat() throws IOException {
        Contrat contrat = base("CDI");
        contrat.setDateGeneration(LocalDateTime.of(2026, 10, 23, 10, 0));

        byte[] pdf = service.generer(contrat);

        assertTrue(new String(pdf, 0, 5, StandardCharsets.US_ASCII).startsWith("%PDF-"));
        assertTrue(pdf.length > 1000);
        String texte = texte(pdf);
        assertTrue(texte.contains("CONTRAT DE TRAVAIL CDI"));
        assertTrue(texte.contains("duree indeterminee"));
    }

    @Test
    void leContratEssaiMentionneLaDureeEtLaFinDEssai() throws IOException {
        Contrat contrat = base("ESSAI");
        contrat.setDureeEssaiMois(3);
        contrat.setDateFinEssai(LocalDate.of(2027, 2, 1));

        String texte = texte(service.generer(contrat));

        assertTrue(texte.contains("contrat d'essai"));
        assertTrue(texte.contains("Duree de la periode d'essai : 3 mois"));
        assertTrue(texte.contains("Fin de la periode d'essai : 01/02/2027"));
    }

    @Test
    void leContratEssaiSansDureeUtiliseTroisMoisParDefaut() throws IOException {
        Contrat contrat = base("ESSAI");

        String texte = texte(service.generer(contrat));

        assertTrue(texte.contains("Duree de la periode d'essai : 3 mois"));
    }

    @Test
    void leContratCddMentionneSaDateFinEtSonMotif() throws IOException {
        Contrat contrat = base("CDD");
        contrat.setDateFin(LocalDate.of(2027, 4, 30));
        contrat.setMotifCdd("Remplacement d'un salarie absent");

        String texte = texte(service.generer(contrat));

        assertTrue(texte.contains("duree determinee"));
        assertTrue(texte.contains("au 30/04/2027"));
        assertTrue(texte.contains("Remplacement d'un salarie absent"));
    }

    private Contrat base(String type) {
        return Contrat.builder()
                .typeContrat(type)
                .numeroContrat("CTR-TEST-0001")
                .statut("ACCEPTE")
                .nomSalarie("Kenny Test")
                .adresseSalarie("Antananarivo")
                .nomEmployeur("Entreprise Exemple")
                .adresseEmployeur("Analamanga")
                .fonction("Developpeur")
                .categorieProfessionnelle("Cadre")
                .indiceClassification("I2")
                .salaire(2500000)
                .dureeTravail("40 heures par semaine")
                .lieuTravail("Antananarivo")
                .dateEffet(LocalDate.of(2026, 11, 1))
                .dateSignature(LocalDate.of(2026, 10, 20))
                .dateGeneration(LocalDateTime.of(2026, 10, 23, 10, 0))
                .dateAcceptation(LocalDateTime.of(2026, 10, 22, 9, 0))
                .build();
    }

    private String texte(byte[] pdf) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(document);
        }
    }
}
