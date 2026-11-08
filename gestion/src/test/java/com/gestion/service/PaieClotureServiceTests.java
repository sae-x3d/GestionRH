package com.gestion.service;

import com.gestion.enums.StatutDemandeConge;
import com.gestion.enums.StatutHeureSup;
import com.gestion.enums.StatutPeriodePaie;
import com.gestion.enums.TypeVariablePaie;
import com.gestion.model.DemandeConge;
import com.gestion.model.Employe;
import com.gestion.model.HeureSup;
import com.gestion.model.PeriodePaie;
import com.gestion.model.TypeConge;
import com.gestion.model.Utilisateur;
import com.gestion.model.VariablePaie;
import com.gestion.repository.DemandeCongeRepository;
import com.gestion.repository.HeureSupRepository;
import com.gestion.repository.PeriodePaieRepository;
import com.gestion.repository.VariablePaieRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaieClotureServiceTests {

    @Mock private PeriodePaieRepository periodeRepository;
    @Mock private VariablePaieRepository variableRepository;
    @Mock private HeureSupRepository heureSupRepository;
    @Mock private DemandeCongeRepository demandeCongeRepository;
    @Mock private AuditService auditService;

    private PaieClotureService service;
    private PeriodePaie periode;
    private Employe employe;

    @BeforeEach
    void setUp() {
        service = new PaieClotureService(periodeRepository, variableRepository,
                heureSupRepository, demandeCongeRepository, auditService);
        periode = PeriodePaie.builder().idPeriodePaie(5).annee(2026).mois(11)
                .dateDebut(LocalDate.of(2026, 11, 1))
                .dateFin(LocalDate.of(2026, 11, 30))
                .statut(StatutPeriodePaie.OUVERTE)
                .dateOuverture(LocalDateTime.now())
                .build();
        employe = Employe.builder().idEmp(10).nomEmp("Kenny Test").build();
    }

    @Test
    void generationAgregeHeuresSupValideesEtAbsencesSansSolde() {
        HeureSup heureSup = HeureSup.builder().idHeureSup(1).employe(employe)
                .dateHeureSup(LocalDate.of(2026, 11, 9)).minutes(90).tauxMajoration(25)
                .statut(StatutHeureSup.VALIDEE).build();
        DemandeConge absence = DemandeConge.builder().idDemandeConge(2).employe(employe)
                .typeConge(TypeConge.builder().code("SANS_SOLDE").libelle("Sans solde").build())
                .dateDebut(LocalDate.of(2026, 11, 10)).dateFin(LocalDate.of(2026, 11, 11))
                .nombreJours(new BigDecimal("2.0")).statut(StatutDemandeConge.VALIDEE_RH).build();
        when(periodeRepository.findByAnneeAndMois(2026, 11)).thenReturn(Optional.of(periode));
        when(heureSupRepository.findByStatutAndPeriodePaieIsNullOrderByDateHeureSupAsc(
                StatutHeureSup.VALIDEE)).thenReturn(List.of(heureSup));
        when(demandeCongeRepository
                .findByStatutAndDateDebutLessThanEqualAndDateFinGreaterThanEqualOrderByDateDebutAsc(
                        StatutDemandeConge.VALIDEE_RH, periode.getDateFin(), periode.getDateDebut()))
                .thenReturn(List.of(absence));
        when(variableRepository.save(any(VariablePaie.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        int total = service.genererVariables(2026, 11);

        assertEquals(2, total);
        ArgumentCaptor<VariablePaie> captor = ArgumentCaptor.forClass(VariablePaie.class);
        verify(variableRepository, times(2)).save(captor.capture());
        VariablePaie heures = captor.getAllValues().get(0);
        assertEquals(TypeVariablePaie.HEURE_SUP, heures.getTypeVariable());
        assertEquals(0, new BigDecimal("1.50").compareTo(heures.getQuantite()));
        assertEquals("HS-1", heures.getReference());
        VariablePaie abs = captor.getAllValues().get(1);
        assertEquals(TypeVariablePaie.ABSENCE, abs.getTypeVariable());
        assertEquals(0, new BigDecimal("2.0").compareTo(abs.getQuantite()));
        assertEquals("ABS-2", abs.getReference());
        assertEquals(periode, heureSup.getPeriodePaie());
    }

    @Test
    void generationIgnoreLesHeuresSupHorsPeriode() {
        HeureSup horsPeriode = HeureSup.builder().idHeureSup(3).employe(employe)
                .dateHeureSup(LocalDate.of(2026, 10, 31)).minutes(60).tauxMajoration(25)
                .statut(StatutHeureSup.VALIDEE).build();
        when(periodeRepository.findByAnneeAndMois(2026, 11)).thenReturn(Optional.of(periode));
        when(heureSupRepository.findByStatutAndPeriodePaieIsNullOrderByDateHeureSupAsc(
                StatutHeureSup.VALIDEE)).thenReturn(List.of(horsPeriode));
        when(demandeCongeRepository
                .findByStatutAndDateDebutLessThanEqualAndDateFinGreaterThanEqualOrderByDateDebutAsc(
                        StatutDemandeConge.VALIDEE_RH, periode.getDateFin(), periode.getDateDebut()))
                .thenReturn(List.of());

        int total = service.genererVariables(2026, 11);

        assertEquals(0, total);
        verify(variableRepository, never()).save(any());
        assertNull(horsPeriode.getPeriodePaie());
    }

    @Test
    void generationRefuseeSurPeriodeCloturee() {
        periode.setStatut(StatutPeriodePaie.CLOTUREE);
        when(periodeRepository.findByAnneeAndMois(2026, 11)).thenReturn(Optional.of(periode));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.genererVariables(2026, 11));

        assertTrue(ex.getMessage().contains("cloturee"));
        verify(variableRepository, never()).deleteByPeriodePaieIdPeriodePaie(any());
    }

    @Test
    void cloturerFigeLaPeriodeEtPasseLesHeuresSupEnPayees() {
        HeureSup heureSup = HeureSup.builder().idHeureSup(4).employe(employe)
                .dateHeureSup(LocalDate.of(2026, 11, 9)).minutes(60).tauxMajoration(25)
                .statut(StatutHeureSup.VALIDEE).build();
        when(periodeRepository.findByAnneeAndMois(2026, 11)).thenReturn(Optional.of(periode));
        when(heureSupRepository.findByStatutAndPeriodePaieIsNullOrderByDateHeureSupAsc(
                StatutHeureSup.VALIDEE)).thenReturn(List.of(heureSup));
        when(demandeCongeRepository
                .findByStatutAndDateDebutLessThanEqualAndDateFinGreaterThanEqualOrderByDateDebutAsc(
                        StatutDemandeConge.VALIDEE_RH, periode.getDateFin(), periode.getDateDebut()))
                .thenReturn(List.of());
        when(variableRepository.save(any(VariablePaie.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(heureSupRepository.findByPeriodePaieIdPeriodePaieOrderByEmployeNomEmpAsc(5))
                .thenReturn(List.of(heureSup));
        when(heureSupRepository.save(heureSup)).thenReturn(heureSup);
        when(periodeRepository.save(periode)).thenReturn(periode);

        PeriodePaie cloturee = service.cloturer(2026, 11, utilisateur(1, "PAIE"));

        assertEquals(StatutPeriodePaie.CLOTUREE, cloturee.getStatut());
        assertNotNull(cloturee.getDateCloture());
        assertEquals(StatutHeureSup.PAYEE, heureSup.getStatut());
        assertEquals(utilisateur(1, "PAIE").getIdUser(), cloturee.getCloturePar().getIdUser());
        verify(auditService).enregistrer(eq("user1"), eq("CLOTURE"), eq("/paie/cloture"),
                eq("PeriodePaie"), eq("2026-11"), contains("avant={statut:OUVERTE"), eq(null));
    }

    @Test
    void clotureRefuseeSiDejaCloturee() {
        periode.setStatut(StatutPeriodePaie.CLOTUREE);
        when(periodeRepository.findByAnneeAndMois(2026, 11)).thenReturn(Optional.of(periode));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.cloturer(2026, 11, utilisateur(1, "PAIE")));

        assertTrue(ex.getMessage().contains("deja cloturee"));
        verify(periodeRepository, never()).save(any());
    }

    @Test
    void exportCsvContientEnteteEtLignes() {
        VariablePaie variable = VariablePaie.builder().idVariablePaie(9).periodePaie(periode)
                .employe(employe).typeVariable(TypeVariablePaie.HEURE_SUP)
                .libelle("Heures supplementaires 25%")
                .quantite(new BigDecimal("1.50")).reference("HS-1")
                .dateCreation(LocalDateTime.now()).build();
        when(periodeRepository.findById(5)).thenReturn(Optional.of(periode));
        when(variableRepository.findByPeriodePaieIdPeriodePaieOrderByEmployeNomEmpAscTypeVariableAsc(5))
                .thenReturn(List.of(variable));

        String csv = service.exporterCsv(5);

        assertTrue(csv.startsWith("salarie;type;libelle;quantite;montant;reference\n"));
        assertTrue(csv.contains("Kenny Test;HEURE_SUP;Heures supplementaires 25%;1.50;;HS-1"));
    }

    private Utilisateur utilisateur(Integer id, String role) {
        return Utilisateur.builder().idUser(id).username("user" + id)
                .role(com.gestion.model.Role.builder().idRole(1).nomRole(role).build())
                .build();
    }
}
