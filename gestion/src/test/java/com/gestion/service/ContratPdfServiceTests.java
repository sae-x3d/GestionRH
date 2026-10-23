package com.gestion.service;

import com.gestion.model.Contrat;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

class ContratPdfServiceTests {
    @Test
    void genereUnPdfLisibleAvecLesMentionsDuContrat() {
        Contrat contrat = Contrat.builder().typeContrat("CDI").nomSalarie("Kenny Test")
                .adresseSalarie("Antananarivo").nomEmployeur("Entreprise Exemple")
                .adresseEmployeur("Analamanga").fonction("Developpeur")
                .categorieProfessionnelle("Cadre").indiceClassification("I2")
                .salaire(2500000).dureeTravail("40 heures par semaine")
                .lieuTravail("Antananarivo").dateEffet(LocalDate.of(2026, 11, 1))
                .dateGeneration(LocalDateTime.of(2026, 10, 23, 10, 0)).build();

        byte[] pdf = new ContratPdfService().generer(contrat);

        assertTrue(new String(pdf, 0, 5, StandardCharsets.US_ASCII).startsWith("%PDF-"));
        assertTrue(pdf.length > 1000);
    }
}
