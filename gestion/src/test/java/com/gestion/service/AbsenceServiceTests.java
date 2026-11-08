package com.gestion.service;

import com.gestion.enums.StatutAbsence;
import com.gestion.enums.TypeAbsence;
import com.gestion.enums.TypeJourPlanning;
import com.gestion.exception.AccesRefuseException;
import com.gestion.model.Absence;
import com.gestion.model.Employe;
import com.gestion.model.PlanningJour;
import com.gestion.model.Role;
import com.gestion.model.Utilisateur;
import com.gestion.model.Departement;
import com.gestion.repository.AbsenceRepository;
import com.gestion.repository.DemandeCongeRepository;
import com.gestion.repository.DepartementRepository;
import com.gestion.repository.EmployeRepository;
import com.gestion.repository.PlanningJourRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AbsenceServiceTests {
    @Mock private AbsenceRepository absenceRepository;
    @Mock private EmployeRepository employeRepository;
    @Mock private PlanningJourRepository planningJourRepository;
    @Mock private DemandeCongeRepository demandeCongeRepository;
    @Mock private DepartementRepository departementRepository;
    @Mock private StockageDocumentService stockageDocumentService;
    @Mock private AuditService auditService;
    private AbsenceService service;
    private Employe employe;

    @BeforeEach void setUp() {
        service = new AbsenceService(absenceRepository, employeRepository, planningJourRepository,
                demandeCongeRepository, departementRepository, stockageDocumentService, auditService);
        employe = Employe.builder().idEmp(10).build();
    }

    @Test void refuseUneAbsenceSansJustificatif() {
        assertThrows(IllegalStateException.class, () -> service.soumettre(employe, TypeAbsence.MALADIE,
                LocalDate.of(2026, 11, 9), LocalDate.of(2026, 11, 10), null));
        verify(absenceRepository, never()).save(any());
    }

    @Test void refuseUneAbsenceQuandLeSeuilEquipeEstDejaAtteint() {
        employe.setDepartement(Departement.builder().idDept(4).build());
        LocalDate jour = LocalDate.of(2026, 11, 9);
        when(employeRepository.countByDepartementIdDeptAndDateDepartIsNull(4)).thenReturn(2L);
        when(demandeCongeRepository.employesAbsentsEquipe(eq(4), eq(jour), anyCollection()))
                .thenReturn(java.util.List.of(11));
        when(absenceRepository.employesAbsentsEquipe(eq(4), eq(jour), anyCollection()))
                .thenReturn(java.util.List.of());
        org.springframework.mock.web.MockMultipartFile justificatif = new org.springframework.mock.web.MockMultipartFile(
                "justificatif", "certificat.pdf", "application/pdf", new byte[]{1});

        IllegalStateException erreur = assertThrows(IllegalStateException.class, () -> service.soumettre(
                employe, TypeAbsence.MALADIE, jour, jour, justificatif));

        assertTrue(erreur.getMessage().contains("seuil d'absents"));
        verify(absenceRepository, never()).save(any());
    }

    @Test void absenceValideePasseLesJoursPlanifiesEnConge() {
        LocalDate lundi = LocalDate.of(2026, 11, 9);
        Absence absence = Absence.builder().idAbsence(5).employe(employe).typeAbsence(TypeAbsence.MALADIE)
                .dateDebut(lundi).dateFin(lundi.plusDays(1)).statut(StatutAbsence.SOUMISE).build();
        PlanningJour jour = PlanningJour.builder().typeJour(TypeJourPlanning.TRAVAIL).build();
        when(absenceRepository.findById(5)).thenReturn(Optional.of(absence));
        when(absenceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(planningJourRepository.findByPlanningEmployeIdEmpAndDateJour(10, lundi)).thenReturn(Optional.of(jour));
        when(planningJourRepository.findByPlanningEmployeIdEmpAndDateJour(10, lundi.plusDays(1))).thenReturn(Optional.empty());

        service.decider(5, true, utilisateur("RH"));

        assertEquals(StatutAbsence.VALIDEE_RH, absence.getStatut());
        assertEquals(TypeJourPlanning.CONGE, jour.getTypeJour());
        verify(planningJourRepository).save(jour);
    }

    @Test void seulUnSalariePeutTelechargerSonJustificatif() {
        Absence absence = Absence.builder().idAbsence(5).employe(employe).justificatifChemin("abs.pdf").build();
        when(absenceRepository.findById(5)).thenReturn(Optional.of(absence));

        assertThrows(AccesRefuseException.class, () -> service.justificatif(5,
                Utilisateur.builder().role(Role.builder().nomRole("EMPLOYE").build())
                        .employe(Employe.builder().idEmp(11).build()).build()));
    }

    @Test void laDirectionNePeutPasDeciderUneAbsenceMedicale() {
        assertThrows(AccesRefuseException.class, () -> service.decider(5, true, utilisateur("DIRECTION")));
        verify(absenceRepository, never()).findById(any());
    }

    private Utilisateur utilisateur(String role) {
        return Utilisateur.builder().idUser(1).role(Role.builder().nomRole(role).build()).build();
    }
}
