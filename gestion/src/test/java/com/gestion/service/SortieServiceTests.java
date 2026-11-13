package com.gestion.service;

import com.gestion.enums.StatutSortie;
import com.gestion.enums.TypeSortie;
import com.gestion.model.ChecklistSortie;
import com.gestion.model.Employe;
import com.gestion.model.SortieEmploye;
import com.gestion.model.Utilisateur;
import com.gestion.repository.ChecklistSortieRepository;
import com.gestion.repository.EmployeRepository;
import com.gestion.repository.SortieEmployeRepository;
import com.gestion.repository.UtilisateurRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class SortieServiceTests {
    @Mock private SortieEmployeRepository sortieRepository;
    @Mock private EmployeRepository employeRepository;
    @Mock private UtilisateurRepository utilisateurRepository;
    @Mock private ChecklistSortieRepository checklistRepository;
    @Mock private EmployeService employeService;
    @Mock private NotificationService notificationService;
    @Mock private HistoriqueCarriereService historiqueCarriereService;

    @Test void validationClotureLeDossierEtCreeLaChecklistDeDepart() {
        Employe employe = Employe.builder().idEmp(20).nomEmp("Employe Test").build();
        SortieEmploye sortie = SortieEmploye.builder().idSortie(8).employe(employe)
                .typeSortie(TypeSortie.DEMISSION).dateSortie(LocalDate.of(2026, 11, 30))
                .statut(StatutSortie.EN_COURS).build();
        when(sortieRepository.findById(8)).thenReturn(Optional.of(sortie));
        when(sortieRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(employeRepository.save(employe)).thenReturn(employe);
        when(checklistRepository.existsBySortieIdSortie(8)).thenReturn(false);
        when(checklistRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(utilisateurRepository.trouverGestionnairesCandidatures()).thenReturn(List.of());
        SortieService service = new SortieService(sortieRepository, employeRepository, utilisateurRepository,
                checklistRepository, employeService, notificationService, historiqueCarriereService);

        SortieEmploye resultat = service.valider(8, 250000, Utilisateur.builder().idUser(1).build());

        assertEquals(StatutSortie.VALIDEE, resultat.getStatut());
        assertEquals(LocalDate.of(2026, 11, 30).atStartOfDay(), employe.getDateDepart());
        assertEquals("SORTI", employe.getStatut());
        verify(checklistRepository).saveAll(argThat(items -> ((List<?>) items).size() == 5));
    }

    @Test void sortieDejaValideeNePeutPlusEtreAnnulee() {
        SortieEmploye sortie = SortieEmploye.builder().idSortie(8).statut(StatutSortie.VALIDEE).build();
        when(sortieRepository.findById(8)).thenReturn(Optional.of(sortie));
        SortieService service = new SortieService(sortieRepository, employeRepository, utilisateurRepository,
                checklistRepository, employeService, notificationService, historiqueCarriereService);

        assertThrows(IllegalStateException.class, () -> service.annuler(8));

        verify(sortieRepository, never()).save(any());
    }
}
