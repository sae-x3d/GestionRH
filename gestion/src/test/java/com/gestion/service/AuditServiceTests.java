package com.gestion.service;

import com.gestion.model.Contrat;
import com.gestion.model.DemandeConge;
import com.gestion.model.Absence;
import com.gestion.model.Employe;
import com.gestion.model.HeureSup;
import com.gestion.model.TypeConge;
import com.gestion.repository.ContratRepository;
import com.gestion.repository.DemandeCongeRepository;
import com.gestion.repository.AbsenceRepository;
import com.gestion.repository.HeureSupRepository;
import com.gestion.repository.EmployeRepository;
import com.gestion.repository.JournalAuditRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditServiceTests {
    @Mock private JournalAuditRepository journalAuditRepository;
    @Mock private EmployeRepository employeRepository;
    @Mock private ContratRepository contratRepository;
    @Mock private DemandeCongeRepository demandeCongeRepository;
    @Mock private AbsenceRepository absenceRepository;
    @Mock private HeureSupRepository heureSupRepository;

    @Test void captureLesValeursSensiblesDuContrat() {
        AuditService audit = new AuditService(journalAuditRepository, employeRepository,
                contratRepository, demandeCongeRepository, absenceRepository, heureSupRepository);
        Contrat contrat = Contrat.builder().idContrat(12).numeroContrat("C-12").typeContrat("CDI")
                .fonction("Comptable").salaire(900000).statut("EN_COURS").build();
        when(contratRepository.findById(12)).thenReturn(Optional.of(contrat));

        var valeurs = audit.instantanePourChemin("/contrats/12/avenant");

        assertEquals("900000", valeurs.get("salaire"));
        assertEquals("EN_COURS", valeurs.get("statut"));
    }

    @Test void captureLeStatutEtLaPeriodeDuneDemandeDeConge() {
        AuditService audit = new AuditService(journalAuditRepository, employeRepository,
                contratRepository, demandeCongeRepository, absenceRepository, heureSupRepository);
        DemandeConge demande = DemandeConge.builder().idDemandeConge(13)
                .employe(Employe.builder().idEmp(4).build())
                .typeConge(TypeConge.builder().code("CP").build())
                .dateDebut(java.time.LocalDate.of(2026, 11, 9)).dateFin(java.time.LocalDate.of(2026, 11, 10))
                .nombreJours(new BigDecimal("2.0")).statut(com.gestion.enums.StatutDemandeConge.SOUMISE).build();
        when(demandeCongeRepository.findById(13)).thenReturn(Optional.of(demande));

        var valeurs = audit.instantanePourChemin("/conges/validation/rh/13");

        assertEquals("SOUMISE", valeurs.get("statut"));
        assertEquals("2026-11-09", valeurs.get("dateDebut"));
        assertEquals("CP", valeurs.get("type"));
    }

    @Test void captureLesSnapshotsDesAbsencesEtHeuresSup() {
        AuditService audit = new AuditService(journalAuditRepository, employeRepository,
                contratRepository, demandeCongeRepository, absenceRepository, heureSupRepository);
        when(absenceRepository.findById(14)).thenReturn(Optional.of(Absence.builder()
                .employe(Employe.builder().idEmp(4).build()).typeAbsence(com.gestion.enums.TypeAbsence.MALADIE)
                .dateDebut(java.time.LocalDate.of(2026, 11, 9)).dateFin(java.time.LocalDate.of(2026, 11, 10))
                .statut(com.gestion.enums.StatutAbsence.SOUMISE).build()));
        when(heureSupRepository.findById(15)).thenReturn(Optional.of(HeureSup.builder()
                .employe(Employe.builder().idEmp(4).build()).dateHeureSup(java.time.LocalDate.of(2026, 11, 9))
                .minutes(90).tauxMajoration(25).statut(com.gestion.enums.StatutHeureSup.EN_ATTENTE).build()));

        assertEquals("SOUMISE", audit.instantanePourChemin("/absences/14/decision").get("statut"));
        var heures = audit.instantanePourChemin("/heures-sup/15/traiter");
        assertEquals("90", heures.get("minutes"));
        assertEquals("EN_ATTENTE", heures.get("statut"));
    }
}
