package com.gestion.service;

import com.gestion.enums.StatutAnomalie;
import com.gestion.enums.StatutHeureSup;
import com.gestion.enums.StatutPointage;
import com.gestion.enums.StatutPlanning;
import com.gestion.enums.StatutPeriodePaie;
import com.gestion.enums.TypeAnomaliePointage;
import com.gestion.enums.TypeJourPlanning;
import com.gestion.exception.AccesRefuseException;
import com.gestion.model.AnomaliePointage;
import com.gestion.model.Departement;
import com.gestion.model.Employe;
import com.gestion.model.HeureSup;
import com.gestion.model.JourFerie;
import com.gestion.model.JourTravaille;
import com.gestion.model.ParametreTempsTravail;
import com.gestion.model.Planning;
import com.gestion.model.PlanningJour;
import com.gestion.model.PeriodePaie;
import com.gestion.model.Pointage;
import com.gestion.model.Role;
import com.gestion.model.Utilisateur;
import com.gestion.repository.AnomaliePointageRepository;
import com.gestion.repository.EmployeRepository;
import com.gestion.repository.DemandeCongeRepository;
import com.gestion.repository.AbsenceRepository;
import com.gestion.repository.JourTravailleRepository;
import com.gestion.repository.PeriodePaieRepository;
import com.gestion.repository.HeureSupRepository;
import com.gestion.repository.JourFerieRepository;
import com.gestion.repository.ParametreTempsTravailRepository;
import com.gestion.repository.PlanningJourRepository;
import com.gestion.repository.PlanningRepository;
import com.gestion.repository.PointageRepository;
import com.gestion.repository.UtilisateurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TempsTravailServiceTests {

    @Mock private ParametreTempsTravailRepository parametreRepository;
    @Mock private PlanningRepository planningRepository;
    @Mock private PlanningJourRepository planningJourRepository;
    @Mock private PointageRepository pointageRepository;
    @Mock private AnomaliePointageRepository anomalieRepository;
    @Mock private HeureSupRepository heureSupRepository;
    @Mock private EmployeRepository employeRepository;
    @Mock private JourFerieRepository jourFerieRepository;
    @Mock private UtilisateurRepository utilisateurRepository;
    @Mock private NotificationService notificationService;
    @Mock private DemandeCongeRepository demandeCongeRepository;
    @Mock private AbsenceRepository absenceRepository;
    @Mock private JourTravailleRepository jourTravailleRepository;
    @Mock private PeriodePaieRepository periodePaieRepository;
    @Mock private AuditService auditService;

    private TempsTravailService service;
    private Employe employe;
    private ParametreTempsTravail parametre;

    @BeforeEach
    void setUp() {
        service = new TempsTravailService(parametreRepository, planningRepository,
                planningJourRepository, pointageRepository, anomalieRepository,
                heureSupRepository, employeRepository, jourFerieRepository,
                utilisateurRepository, notificationService, demandeCongeRepository, absenceRepository, jourTravailleRepository,
                periodePaieRepository, auditService);
        employe = Employe.builder().idEmp(10).nomEmp("Kenny Test")
                .departement(Departement.builder().idDept(4).nomDept("Informatique").build())
                .minutesRecuperation(0)
                .build();
        parametre = ParametreTempsTravail.builder()
                .idParametre(1)
                .heuresHebdomadaires(new BigDecimal("35.0"))
                .seuilHebdoHeuresSup(new BigDecimal("35.0"))
                .heureDebut(LocalTime.of(8, 0))
                .heureFin(LocalTime.of(17, 0))
                .pauseMinutes(60)
                .toleranceRetardMinutes(10)
                .actif(true)
                .build();
    }

    @Test
    void arriveeEnRetardCreeUneAnomalie() {
        LocalDate lundi = LocalDate.of(2026, 11, 9);
        when(parametreRepository.findFirstByActifTrueOrderByIdParametreAsc())
                .thenReturn(Optional.of(parametre));
        when(pointageRepository.findByEmployeIdEmpAndDatePointage(10, lundi))
                .thenReturn(Optional.empty());
        when(planningJourRepository.findByPlanningEmployeIdEmpAndDateJour(10, lundi))
                .thenReturn(Optional.empty());
        when(pointageRepository.save(any(Pointage.class)))
                .thenAnswer(invocation -> {
                    Pointage p = invocation.getArgument(0);
                    p.setIdPointage(500);
                    return p;
                });
        when(anomalieRepository.existsByPointageIdPointageAndTypeAnomalie(
                500, TypeAnomaliePointage.RETARD)).thenReturn(false);
        when(anomalieRepository.save(any(AnomaliePointage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Pointage pointage = service.pointerArrivee(employe, LocalDateTime.of(2026, 11, 9, 8, 40));

        assertEquals(StatutPointage.RETARD, pointage.getStatut());
        ArgumentCaptor<AnomaliePointage> captor = ArgumentCaptor.forClass(AnomaliePointage.class);
        verify(anomalieRepository).save(captor.capture());
        assertEquals(TypeAnomaliePointage.RETARD, captor.getValue().getTypeAnomalie());
        assertEquals(40, captor.getValue().getMinutes());
        assertEquals(StatutAnomalie.OUVERTE, captor.getValue().getStatut());
    }

    @Test
    void arriveeDansLaToleranceNeCreePasDAnomalie() {
        LocalDate lundi = LocalDate.of(2026, 11, 9);
        when(parametreRepository.findFirstByActifTrueOrderByIdParametreAsc())
                .thenReturn(Optional.of(parametre));
        when(pointageRepository.findByEmployeIdEmpAndDatePointage(10, lundi))
                .thenReturn(Optional.empty());
        when(planningJourRepository.findByPlanningEmployeIdEmpAndDateJour(10, lundi))
                .thenReturn(Optional.empty());
        when(pointageRepository.save(any(Pointage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Pointage pointage = service.pointerArrivee(employe, LocalDateTime.of(2026, 11, 9, 8, 10));

        assertEquals(StatutPointage.PRESENT, pointage.getStatut());
        verify(anomalieRepository, never()).save(any());
    }

    @Test
    void departAnticipeCreeDeuxAnomaliesDontLesHeuresManquantes() {
        LocalDate lundi = LocalDate.of(2026, 11, 9);
        Pointage existant = Pointage.builder().idPointage(501).employe(employe)
                .datePointage(lundi).heureArrivee(LocalTime.of(8, 0)).pauseMinutes(60)
                .statut(StatutPointage.PRESENT).dateCreation(LocalDateTime.now()).build();
        when(pointageRepository.findByEmployeIdEmpAndDatePointage(10, lundi))
                .thenReturn(Optional.of(existant));
        when(parametreRepository.findFirstByActifTrueOrderByIdParametreAsc())
                .thenReturn(Optional.of(parametre));
        when(planningJourRepository.findByPlanningEmployeIdEmpAndDateJour(10, lundi))
                .thenReturn(Optional.empty());
        when(pointageRepository.save(any(Pointage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(anomalieRepository.existsByPointageIdPointageAndTypeAnomalie(
                anyInt(), any(TypeAnomaliePointage.class))).thenReturn(false);
        when(anomalieRepository.save(any(AnomaliePointage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Pointage pointage = service.pointerDepart(employe, LocalDateTime.of(2026, 11, 9, 16, 0));

        assertEquals(StatutPointage.DEPART_ANTICIPE, pointage.getStatut());
        ArgumentCaptor<AnomaliePointage> captor = ArgumentCaptor.forClass(AnomaliePointage.class);
        verify(anomalieRepository, times(2)).save(captor.capture());
        assertTrue(captor.getAllValues().stream()
                .anyMatch(a -> a.getTypeAnomalie() == TypeAnomaliePointage.DEPART_ANTICIPE));
        AnomaliePointage manquantes = captor.getAllValues().stream()
                .filter(a -> a.getTypeAnomalie() == TypeAnomaliePointage.HEURES_MANQUANTES)
                .findFirst().orElseThrow();
        assertEquals(60, manquantes.getMinutes());
    }

    @Test
    void departSansArriveeMarqueLePointageIncomplet() {
        LocalDate lundi = LocalDate.of(2026, 11, 9);
        when(pointageRepository.findByEmployeIdEmpAndDatePointage(10, lundi))
                .thenReturn(Optional.empty());
        when(pointageRepository.save(any(Pointage.class)))
                .thenAnswer(invocation -> {
                    Pointage p = invocation.getArgument(0);
                    p.setIdPointage(502);
                    return p;
                });
        when(anomalieRepository.existsByPointageIdPointageAndTypeAnomalie(
                502, TypeAnomaliePointage.POINTAGE_INCOMPLET)).thenReturn(false);
        when(anomalieRepository.save(any(AnomaliePointage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Pointage pointage = service.pointerDepart(employe, LocalDateTime.of(2026, 11, 9, 17, 0));

        assertEquals(StatutPointage.INCOMPLET, pointage.getStatut());
        ArgumentCaptor<AnomaliePointage> captor = ArgumentCaptor.forClass(AnomaliePointage.class);
        verify(anomalieRepository).save(captor.capture());
        assertEquals(TypeAnomaliePointage.POINTAGE_INCOMPLET,
                captor.getValue().getTypeAnomalie());
    }

    @Test
    void declarationHeuresSupScindeEnTranches25Et50() {
        LocalDate lundi = LocalDate.of(2026, 11, 2);
        when(heureSupRepository.totalMinutes(10, lundi, lundi.plusDays(6))).thenReturn(300L);
        when(heureSupRepository.save(any(HeureSup.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<HeureSup> heures = service.declarerHeureSup(
                employe, lundi.plusDays(2), 300, "Inventaire");

        assertEquals(2, heures.size());
        assertEquals(180, heures.get(0).getMinutes());
        assertEquals(25, heures.get(0).getTauxMajoration());
        assertEquals(120, heures.get(1).getMinutes());
        assertEquals(50, heures.get(1).getTauxMajoration());
        assertTrue(heures.stream().allMatch(h -> h.getStatut() == StatutHeureSup.EN_ATTENTE));
    }

    @Test
    void declarationRefuseUnVolumeHorsBornes() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.declarerHeureSup(employe, LocalDate.now().minusDays(1), 0, null));

        assertTrue(ex.getMessage().contains("1 et 600"));
        verify(heureSupRepository, never()).save(any());
    }

    @Test
    void validationEnReposCrediteLesMinutesDeRecuperation() {
        HeureSup heureSup = HeureSup.builder().idHeureSup(77).employe(employe)
                .dateHeureSup(LocalDate.of(2026, 11, 9)).minutes(90).tauxMajoration(25)
                .statut(StatutHeureSup.EN_ATTENTE).build();
        when(heureSupRepository.findById(77)).thenReturn(Optional.of(heureSup));
        when(heureSupRepository.save(heureSup)).thenReturn(heureSup);
        when(employeRepository.save(employe)).thenReturn(employe);
        when(utilisateurRepository.findByEmployeIdEmp(10)).thenReturn(Optional.empty());

        HeureSup traitee = service.traiterHeureSup(
                77, true, true, utilisateur(1, "RH"));

        assertEquals(StatutHeureSup.REPOS, traitee.getStatut());
        assertEquals(90, employe.getMinutesRecuperation());
        verify(employeRepository).save(employe);
    }

    @Test
    void unManagerHorsDepartementNePeutPasValiderDesHeuresSup() {
        HeureSup heureSup = HeureSup.builder().idHeureSup(78).employe(employe)
                .statut(StatutHeureSup.EN_ATTENTE).build();
        when(heureSupRepository.findById(78)).thenReturn(Optional.of(heureSup));
        Utilisateur manager = utilisateur(20, "MANAGER");
        manager.setEmploye(Employe.builder().idEmp(99).departement(
                Departement.builder().idDept(9).nomDept("Ventes").build()).build());

        assertThrows(AccesRefuseException.class,
                () -> service.traiterHeureSup(78, true, false, manager));

        verify(heureSupRepository, never()).save(any());
    }

    @Test
    void creationPlanningGenereLesJoursOuvresEtLesRepos() {
        LocalDate lundi = LocalDate.of(2026, 11, 9);
        when(planningRepository.findByEmployeIdEmpAndDateDebut(10, lundi))
                .thenReturn(Optional.empty());
        when(parametreRepository.findFirstByActifTrueOrderByIdParametreAsc())
                .thenReturn(Optional.of(parametre));
        when(jourFerieRepository.findByDateJourFerieBetween(lundi, lundi.plusDays(6)))
                .thenReturn(List.of());
        when(planningRepository.save(any(Planning.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Planning planning = service.creerPlanning(employe, lundi, utilisateur(1, "ADMINISTRATEUR"));

        assertEquals(StatutPlanning.BROUILLON, planning.getStatut());
        assertEquals(7, planning.getJours().size());
        assertEquals(TypeJourPlanning.TRAVAIL, planning.getJours().get(0).getTypeJour());
        assertEquals(LocalTime.of(8, 0), planning.getJours().get(0).getHeureDebut());
        assertEquals(TypeJourPlanning.REPOS, planning.getJours().get(5).getTypeJour());
        assertEquals(TypeJourPlanning.REPOS, planning.getJours().get(6).getTypeJour());
    }

    @Test
    void batchDuSoirCreeUneAnomaliePourUneAbsenceSansPointage() {
        LocalDate hier = LocalDate.now().minusDays(1);
        Planning planning = Planning.builder().statut(StatutPlanning.PUBLIE).build();
        PlanningJour jour = PlanningJour.builder().planning(planning).dateJour(hier)
                .typeJour(TypeJourPlanning.TRAVAIL).heureDebut(LocalTime.of(8, 0))
                .heureFin(LocalTime.of(17, 0)).pauseMinutes(60).build();
        when(employeRepository.findByDateDepartIsNullOrderByNomEmpAsc()).thenReturn(List.of(employe));
        when(planningJourRepository.findByPlanningEmployeIdEmpAndDateJour(10, hier)).thenReturn(Optional.of(jour));

        int detectees = service.detecterAbsencesVeille();

        assertEquals(1, detectees);
        verify(anomalieRepository).save(argThat(a -> a.getTypeAnomalie() == TypeAnomaliePointage.ABSENCE
                && a.getPointage() == null && a.getDateAnomalie().equals(hier)));
    }

    @Test
    void batchHebdomadaireCalculeLesHeuresSupDepuisLesPointages() {
        LocalDate lundi = service.lundi(LocalDate.now().minusDays(1));
        when(parametreRepository.findFirstByActifTrueOrderByIdParametreAsc()).thenReturn(Optional.of(parametre));
        when(employeRepository.findByDateDepartIsNullOrderByNomEmpAsc()).thenReturn(List.of(employe));
        LocalDate fin = LocalDate.now().minusDays(1);
        List<Pointage> semaine = List.of(
                pointage(lundi, 8, 17), pointage(lundi.plusDays(1), 8, 16),
                pointage(lundi.plusDays(2), 8, 16), pointage(lundi.plusDays(3), 8, 16));
        when(pointageRepository.findByEmployeIdEmpAndDatePointageBetweenOrderByDatePointageAsc(10, lundi, fin))
                .thenReturn(semaine);
        when(heureSupRepository.totalMinutes(10, lundi, fin)).thenReturn(0L);

        int employees = service.calculerHeuresSupSemaine();

        assertEquals(1, employees);
        ArgumentCaptor<HeureSup> captor = ArgumentCaptor.forClass(HeureSup.class);
        verify(heureSupRepository).save(captor.capture());
        assertEquals(60, captor.getValue().getMinutes());
        assertEquals(25, captor.getValue().getTauxMajoration());
        assertNotEquals(java.time.DayOfWeek.SUNDAY, captor.getValue().getDateHeureSup().getDayOfWeek());
    }

    @Test
    void modifierJourExigeOnzeHeuresDeReposEntreDeuxJournees() {
        LocalDate lundi = LocalDate.of(2026, 11, 9);
        Planning planning = Planning.builder().idPlanning(8).employe(employe).dateDebut(lundi)
                .dateFin(lundi.plusDays(6)).statut(StatutPlanning.BROUILLON).build();
        PlanningJour veille = PlanningJour.builder().planning(planning).dateJour(lundi.minusDays(1))
                .typeJour(TypeJourPlanning.TRAVAIL).heureFin(LocalTime.of(23, 0)).build();
        when(planningRepository.findById(8)).thenReturn(Optional.of(planning));
        when(planningJourRepository.findByPlanningEmployeIdEmpAndDateJour(10, lundi.minusDays(1)))
                .thenReturn(Optional.of(veille));

        IllegalStateException erreur = assertThrows(IllegalStateException.class, () -> service.modifierJour(
                8, lundi, TypeJourPlanning.TRAVAIL, LocalTime.of(8, 0), LocalTime.of(16, 0), 0,
                utilisateur(1, "ADMINISTRATEUR")));

        assertTrue(erreur.getMessage().contains("11 heures"));
        verify(planningJourRepository, never()).save(any());
    }

    @Test
    void modifierJourRefuseLeDepassementHebdomadaire() {
        LocalDate lundi = LocalDate.of(2026, 11, 9);
        Planning planning = Planning.builder().idPlanning(8).employe(employe).dateDebut(lundi)
                .dateFin(lundi.plusDays(6)).statut(StatutPlanning.BROUILLON).build();
        List<PlanningJour> jours = java.util.stream.IntStream.rangeClosed(1, 4).mapToObj(i -> PlanningJour.builder()
                .planning(planning).dateJour(lundi.plusDays(i)).typeJour(TypeJourPlanning.TRAVAIL)
                .heureDebut(LocalTime.of(8, 0)).heureFin(LocalTime.of(16, 0)).pauseMinutes(60).build()).toList();
        when(planningRepository.findById(8)).thenReturn(Optional.of(planning));
        when(planningJourRepository.findByPlanningEmployeIdEmpAndDateJour(10, lundi.minusDays(1)))
                .thenReturn(Optional.empty());
        when(parametreRepository.findFirstByActifTrueOrderByIdParametreAsc()).thenReturn(Optional.of(parametre));
        when(planningJourRepository.findByPlanningIdPlanningOrderByDateJourAsc(8)).thenReturn(jours);

        IllegalStateException erreur = assertThrows(IllegalStateException.class, () -> service.modifierJour(
                8, lundi, TypeJourPlanning.TRAVAIL, LocalTime.of(8, 0), LocalTime.of(17, 0), 60,
                utilisateur(1, "ADMINISTRATEUR")));

        assertTrue(erreur.getMessage().contains("hebdomadaire"));
        verify(planningJourRepository, never()).save(any());
    }

    @Test
    void batchHeuresSupDeduitLesHeuresDejaDeclareesSansIgnorerLaSemaine() {
        LocalDate fin = LocalDate.of(2026, 11, 8);
        LocalDate lundi = service.lundi(fin);
        List<Pointage> semaine = java.util.stream.IntStream.range(0, 5)
                .mapToObj(i -> pointage(lundi.plusDays(i), 8, 17)).toList();
        when(parametreRepository.findFirstByActifTrueOrderByIdParametreAsc()).thenReturn(Optional.of(parametre));
        when(employeRepository.findByDateDepartIsNullOrderByNomEmpAsc()).thenReturn(List.of(employe));
        when(pointageRepository.findByEmployeIdEmpAndDatePointageBetweenOrderByDatePointageAsc(10, lundi, fin))
                .thenReturn(semaine);
        when(heureSupRepository.totalMinutes(10, lundi, fin)).thenReturn(60L);

        assertEquals(1, service.calculerHeuresSupSemaine(fin));

        ArgumentCaptor<HeureSup> captor = ArgumentCaptor.forClass(HeureSup.class);
        verify(heureSupRepository, atLeastOnce()).save(captor.capture());
        assertEquals(240, captor.getAllValues().stream().mapToInt(HeureSup::getMinutes).sum());
    }

    @Test
    void batchHeuresSupRetireLesCongesPayesDuSeuilHebdomadaire() {
        LocalDate fin = LocalDate.of(2026, 11, 8);
        LocalDate lundi = service.lundi(fin);
        LocalDate vendredi = lundi.plusDays(4);
        List<Pointage> semaine = java.util.stream.IntStream.range(0, 4)
                .mapToObj(i -> pointage(lundi.plusDays(i), 8, 17)).toList();
        when(parametreRepository.findFirstByActifTrueOrderByIdParametreAsc()).thenReturn(Optional.of(parametre));
        when(employeRepository.findByDateDepartIsNullOrderByNomEmpAsc()).thenReturn(List.of(employe));
        when(pointageRepository.findByEmployeIdEmpAndDatePointageBetweenOrderByDatePointageAsc(10, lundi, fin))
                .thenReturn(semaine);
        when(demandeCongeRepository.existsByEmployeIdEmpAndStatutAndDateDebutLessThanEqualAndDateFinGreaterThanEqual(
                eq(10), eq(com.gestion.enums.StatutDemandeConge.VALIDEE_RH), any(LocalDate.class), any(LocalDate.class)))
                .thenAnswer(inv -> inv.getArgument(2, LocalDate.class).equals(vendredi));

        assertEquals(1, service.calculerHeuresSupSemaine(fin));

        ArgumentCaptor<HeureSup> captor = ArgumentCaptor.forClass(HeureSup.class);
        verify(heureSupRepository, atLeastOnce()).save(captor.capture());
        assertEquals(240, captor.getAllValues().stream().mapToInt(HeureSup::getMinutes).sum());
    }

    @Test
    void batchHeuresSupAppliqueLaMajorationJourFerieEtNuit() {
        LocalDate fin = LocalDate.of(2026, 11, 8);
        LocalDate lundi = service.lundi(fin);
        LocalDate vendredi = lundi.plusDays(4);
        List<Pointage> semaine = java.util.stream.IntStream.range(0, 5)
                .mapToObj(i -> pointage(lundi.plusDays(i), i == 4 ? 5 : 8, i == 4 ? 14 : 17)).toList();
        when(parametreRepository.findFirstByActifTrueOrderByIdParametreAsc()).thenReturn(Optional.of(parametre));
        when(employeRepository.findByDateDepartIsNullOrderByNomEmpAsc()).thenReturn(List.of(employe));
        when(pointageRepository.findByEmployeIdEmpAndDatePointageBetweenOrderByDatePointageAsc(10, lundi, fin))
                .thenReturn(semaine);
        when(jourFerieRepository.findByDateJourFerieBetween(any(LocalDate.class), any(LocalDate.class)))
                .thenAnswer(inv -> inv.getArgument(0, LocalDate.class).equals(vendredi)
                        ? List.of(JourFerie.builder().dateJourFerie(vendredi).libelle("Ferie").build()) : List.of());

        assertEquals(1, service.calculerHeuresSupSemaine(fin));

        ArgumentCaptor<HeureSup> captor = ArgumentCaptor.forClass(HeureSup.class);
        verify(heureSupRepository, atLeastOnce()).save(captor.capture());
        assertTrue(captor.getAllValues().stream().anyMatch(h -> h.getDateHeureSup().equals(vendredi)
                && h.getTauxMajoration() == 125));
    }

    @Test
    void batchHeuresSupAppliqueLaMajorationNuitAuxPointagesDeNuit() {
        LocalDate fin = LocalDate.of(2026, 11, 8);
        LocalDate lundi = service.lundi(fin);
        LocalDate vendredi = lundi.plusDays(4);
        List<Pointage> semaine = java.util.stream.IntStream.range(0, 5)
                .mapToObj(i -> pointage(lundi.plusDays(i), i == 4 ? 5 : 8, i == 4 ? 14 : 17)).toList();
        when(parametreRepository.findFirstByActifTrueOrderByIdParametreAsc()).thenReturn(Optional.of(parametre));
        when(employeRepository.findByDateDepartIsNullOrderByNomEmpAsc()).thenReturn(List.of(employe));
        when(pointageRepository.findByEmployeIdEmpAndDatePointageBetweenOrderByDatePointageAsc(10, lundi, fin))
                .thenReturn(semaine);

        assertEquals(1, service.calculerHeuresSupSemaine(fin));

        ArgumentCaptor<HeureSup> captor = ArgumentCaptor.forClass(HeureSup.class);
        verify(heureSupRepository, atLeastOnce()).save(captor.capture());
        assertTrue(captor.getAllValues().stream().anyMatch(h -> h.getDateHeureSup().equals(vendredi)
                && h.getTauxMajoration() == 50));
    }

    @Test
    void batchHeuresSupAppliqueLaMajorationDuDimanche() {
        LocalDate dimanche = LocalDate.of(2026, 11, 8);
        LocalDate lundi = service.lundi(dimanche);
        List<Pointage> semaine = new java.util.ArrayList<>();
        for (int i = 0; i < 5; i++) semaine.add(pointage(lundi.plusDays(i), 8, 16));
        semaine.add(pointage(dimanche, 8, 17));
        when(parametreRepository.findFirstByActifTrueOrderByIdParametreAsc()).thenReturn(Optional.of(parametre));
        when(employeRepository.findByDateDepartIsNullOrderByNomEmpAsc()).thenReturn(List.of(employe));
        when(pointageRepository.findByEmployeIdEmpAndDatePointageBetweenOrderByDatePointageAsc(10, lundi, dimanche))
                .thenReturn(semaine);

        assertEquals(1, service.calculerHeuresSupSemaine(dimanche));

        ArgumentCaptor<HeureSup> captor = ArgumentCaptor.forClass(HeureSup.class);
        verify(heureSupRepository, atLeastOnce()).save(captor.capture());
        assertTrue(captor.getAllValues().stream().anyMatch(h -> h.getDateHeureSup().equals(dimanche)
                && h.getTauxMajoration() == 75));
    }

    @Test
    void batchJourTravailleEnregistreBrutNetNormalEtSupplementaire() {
        LocalDate hier = LocalDate.now().minusDays(1);
        when(employeRepository.findByDateDepartIsNullOrderByNomEmpAsc()).thenReturn(List.of(employe));
        when(pointageRepository.findByEmployeIdEmpAndDatePointage(10, hier))
                .thenReturn(Optional.of(pointage(hier, 8, 17)));
        when(planningJourRepository.findByPlanningEmployeIdEmpAndDateJour(10, hier))
                .thenReturn(Optional.of(PlanningJour.builder().typeJour(TypeJourPlanning.TRAVAIL)
                        .heureDebut(LocalTime.of(8, 0)).heureFin(LocalTime.of(17, 0)).pauseMinutes(60).build()));

        assertEquals(1, service.calculerJoursTravaillesVeille());

        verify(jourTravailleRepository).save(argThat(j -> j.getMinutesBrutes() == 540
                && j.getMinutesNet() == 480 && j.getMinutesNormales() == 480
                && j.getMinutesSupplementaires() == 0));
    }

    @Test
    void regularisationApresClotureEstReporteSurLaPeriodeSuivante() {
        PeriodePaie source = PeriodePaie.builder().idPeriodePaie(1).annee(2026).mois(10)
                .dateDebut(LocalDate.of(2026, 10, 1)).dateFin(LocalDate.of(2026, 10, 31))
                .statut(StatutPeriodePaie.CLOTUREE).build();
        PeriodePaie cible = PeriodePaie.builder().idPeriodePaie(2).annee(2026).mois(11)
                .dateDebut(LocalDate.of(2026, 11, 1)).dateFin(LocalDate.of(2026, 11, 30))
                .statut(StatutPeriodePaie.OUVERTE).build();
        when(periodePaieRepository.findById(1)).thenReturn(Optional.of(source));
        when(periodePaieRepository.findByAnneeAndMois(2026, 11)).thenReturn(Optional.of(cible));
        when(employeRepository.findById(10)).thenReturn(Optional.of(employe));
        when(heureSupRepository.save(any(HeureSup.class))).thenAnswer(invocation -> invocation.getArgument(0));

        HeureSup correction = service.regulariserApresCloture(10, 1, 90, "Oubli pointage");

        assertEquals(LocalDate.of(2026, 11, 1), correction.getDateHeureSup());
        assertEquals(90, correction.getMinutes());
        assertEquals(StatutHeureSup.VALIDEE, correction.getStatut());
        assertTrue(correction.getMotif().contains("Oubli pointage"));
    }

    private Pointage pointage(LocalDate date, int arrivee, int depart) {
        return Pointage.builder().employe(employe).datePointage(date)
                .heureArrivee(LocalTime.of(arrivee, 0)).heureDepart(LocalTime.of(depart, 0))
                .pauseMinutes(60).build();
    }

    @Test
    void unManagerNePeutPasPlanifierUnSalarieDUnAutreDepartement() {
        Utilisateur manager = utilisateur(20, "MANAGER");
        manager.setEmploye(Employe.builder().idEmp(99).departement(
                Departement.builder().idDept(9).nomDept("Ventes").build()).build());

        assertThrows(AccesRefuseException.class,
                () -> service.creerPlanning(employe, LocalDate.of(2026, 11, 9), manager));

        verify(planningRepository, never()).save(any());
    }

    @Test
    void sousEffectifPlanningSignaleUneCouvertureSousLeSeuil() {
        LocalDate jour = LocalDate.of(2026, 11, 9);
        Planning planning = Planning.builder().idPlanning(30).employe(employe).build();
        when(planningRepository.findById(30)).thenReturn(Optional.of(planning));
        when(employeRepository.countByDepartementIdDeptAndDateDepartIsNull(4)).thenReturn(10L);
        when(planningJourRepository.countByPlanningEmployeDepartementIdDeptAndDateJourAndTypeJour(
                4, jour, TypeJourPlanning.TRAVAIL)).thenReturn(4L);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "couvertureMinimale", 0.5);

        assertTrue(service.sousEffectif(30, jour));
    }

    @Test
    void unCongeValideBloqueLaModificationDuJourEnTravail() {
        LocalDate jour = LocalDate.of(2026, 11, 9);
        Planning planning = Planning.builder().idPlanning(31).employe(employe).dateDebut(jour)
                .dateFin(jour.plusDays(6)).statut(StatutPlanning.BROUILLON).build();
        when(planningRepository.findById(31)).thenReturn(Optional.of(planning));
        when(demandeCongeRepository.existsByEmployeIdEmpAndStatutAndDateDebutLessThanEqualAndDateFinGreaterThanEqual(
                10, com.gestion.enums.StatutDemandeConge.VALIDEE_RH, jour, jour)).thenReturn(true);

        IllegalStateException erreur = assertThrows(IllegalStateException.class, () -> service.modifierJour(
                31, jour, TypeJourPlanning.TRAVAIL, LocalTime.of(8, 0), LocalTime.of(16, 0), 0,
                utilisateur(1, "ADMINISTRATEUR")));

        assertTrue(erreur.getMessage().contains("conge valide"));
        verify(planningJourRepository, never()).save(any());
    }

    private Utilisateur utilisateur(Integer id, String role) {
        return Utilisateur.builder().idUser(id).username("user" + id)
                .role(Role.builder().idRole(1).nomRole(role).build())
                .build();
    }
}
