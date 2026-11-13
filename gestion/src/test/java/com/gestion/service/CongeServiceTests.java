package com.gestion.service;

import com.gestion.enums.StatutDemandeConge;
import com.gestion.exception.AccesRefuseException;
import com.gestion.model.ConsommationConge;
import com.gestion.model.DemandeConge;
import com.gestion.model.Departement;
import com.gestion.model.Employe;
import com.gestion.model.JourFerie;
import com.gestion.model.Role;
import com.gestion.model.SoldeConge;
import com.gestion.model.TypeConge;
import com.gestion.model.Utilisateur;
import com.gestion.repository.ConsommationCongeRepository;
import com.gestion.repository.AbsenceRepository;
import com.gestion.repository.DemandeCongeRepository;
import com.gestion.repository.DepartementRepository;
import com.gestion.repository.DroitCongeRepository;
import com.gestion.repository.EmployeRepository;
import com.gestion.repository.JourFerieRepository;
import com.gestion.repository.SoldeCongeRepository;
import com.gestion.repository.TypeCongeRepository;
import com.gestion.repository.PlanningJourRepository;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CongeServiceTests {

    @Mock private DemandeCongeRepository demandeRepository;
    @Mock private SoldeCongeRepository soldeRepository;
    @Mock private ConsommationCongeRepository consommationRepository;
    @Mock private TypeCongeRepository typeCongeRepository;
    @Mock private PlanningJourRepository planningJourRepository;
    @Mock private StockageDocumentService stockageDocumentService;
    @Mock private AuditService auditService;
    @Mock private AbsenceRepository absenceRepository;
    @Mock private JourFerieRepository jourFerieRepository;
    @Mock private DroitCongeRepository droitCongeRepository;
    @Mock private EmployeRepository employeRepository;
    @Mock private DepartementRepository departementRepository;
    @Mock private UtilisateurRepository utilisateurRepository;
    @Mock private NotificationService notificationService;

    private CongeService service;
    private Employe employe;
    private TypeConge congePaye;

    @BeforeEach
    void setUp() {
        service = new CongeService(demandeRepository, soldeRepository, consommationRepository,
                typeCongeRepository, planningJourRepository, stockageDocumentService, auditService, absenceRepository, jourFerieRepository, droitCongeRepository,
                employeRepository, departementRepository, utilisateurRepository, notificationService);
        employe = Employe.builder().idEmp(10).nomEmp("Kenny Test")
                .departement(Departement.builder().idDept(4).nomDept("Informatique").build())
                .build();
        congePaye = TypeConge.builder().idTypeConge(1).code("CONGE_PAYE")
                .libelle("Conges payes").decompteSolde(true).actif(true).build();
    }

    @Test
    void calculerJoursExclutWeekendEtJoursFeries() {
        LocalDate vendredi = LocalDate.of(2026, 11, 6);
        LocalDate mardi = LocalDate.of(2026, 11, 10);
        when(jourFerieRepository.findByDateJourFerieBetween(vendredi, mardi))
                .thenReturn(List.of(JourFerie.builder()
                        .dateJourFerie(LocalDate.of(2026, 11, 9)).libelle("Ferie").build()));

        BigDecimal jours = service.calculerJours(vendredi, mardi, false, false);

        assertEquals(0, new BigDecimal("2.0").compareTo(jours));
    }

    @Test
    void calculerJoursGereLesDemiJourneesSurUneJourneeUnique() {
        LocalDate lundi = LocalDate.of(2026, 11, 9);
        when(jourFerieRepository.findByDateJourFerieBetween(lundi, lundi)).thenReturn(List.of());

        BigDecimal jours = service.calculerJours(lundi, lundi, true, true);

        assertEquals(0, new BigDecimal("0.5").compareTo(jours));
    }

    @Test
    void soumettreRefuseLeDepassementDuSeuilDAbsentsDeLequipe() {
        LocalDate jour = LocalDate.of(2026, 11, 9);
        when(typeCongeRepository.findById(1)).thenReturn(Optional.of(congePaye));
        when(demandeRepository.existsChevauchement(eq(10), eq(jour), eq(jour), anyCollection())).thenReturn(false);
        when(employeRepository.countByDepartementIdDeptAndDateDepartIsNull(4)).thenReturn(2L);
        when(demandeRepository.employesAbsentsEquipe(eq(4), eq(jour), anyCollection())).thenReturn(List.of(11));
        when(absenceRepository.employesAbsentsEquipe(eq(4), eq(jour), anyCollection())).thenReturn(List.of());

        IllegalStateException erreur = assertThrows(IllegalStateException.class,
                () -> service.soumettre(employe, 1, jour, jour, false, false, null, null, null));

        assertTrue(erreur.getMessage().contains("seuil d'absents"));
        verify(demandeRepository, never()).save(any());
    }

    @Test
    void soumettreReserveLeSoldeFifoEnConsommantLeLotLePlusProcheDeLExpiration() {
        LocalDate debut = LocalDate.of(2026, 11, 9);
        LocalDate fin = LocalDate.of(2026, 11, 11);
        when(typeCongeRepository.findById(1)).thenReturn(Optional.of(congePaye));
        when(demandeRepository.existsChevauchement(eq(10), eq(debut), eq(fin), anyCollection()))
                .thenReturn(false);
        when(jourFerieRepository.findByDateJourFerieBetween(debut, fin)).thenReturn(List.of());
        when(soldeRepository.totalDisponible(eq(10), eq(1), any(LocalDate.class)))
                .thenReturn(new BigDecimal("7.0"));
        SoldeConge premier = lot(100, "2.0", LocalDate.of(2026, 12, 31));
        SoldeConge second = lot(101, "5.0", LocalDate.of(2027, 12, 31));
        when(soldeRepository.verrouillerLotsFifo(10, 1))
                .thenReturn(List.of(premier, second));
        when(demandeRepository.save(any(DemandeConge.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DemandeConge demande = service.soumettre(employe, 1, debut, fin, false, false, "Vacances",
                utilisateur(20, "MANAGER"));

        assertEquals(0, new BigDecimal("0.0").compareTo(premier.getQuantiteRestante()));
        assertEquals(0, new BigDecimal("4.0").compareTo(second.getQuantiteRestante()));
        assertEquals(0, new BigDecimal("3.0").compareTo(demande.getNombreJours()));
        assertEquals(StatutDemandeConge.SOUMISE, demande.getStatut());
        verify(departementRepository).verrouillerPourReservation(4);
        verify(employeRepository).verrouillerPourDemande(10);
        verify(soldeRepository).verrouillerLotsFifo(10, 1);
        verify(auditService).enregistrer(eq("user20"), eq("CREATION"), eq("/conges"), eq("DemandeConge"),
                any(), org.mockito.ArgumentMatchers.contains("avant={}; apres={"), eq(null));
        ArgumentCaptor<ConsommationConge> captor = ArgumentCaptor.forClass(ConsommationConge.class);
        verify(consommationRepository, times(2)).save(captor.capture());
        assertEquals(0, new BigDecimal("2.0").compareTo(captor.getAllValues().get(0).getQuantite()));
        assertEquals(0, new BigDecimal("1.0").compareTo(captor.getAllValues().get(1).getQuantite()));
    }

    @Test
    void soumettreRefuseUnChevauchementAvecUneDemandeActive() {
        LocalDate debut = LocalDate.of(2026, 11, 9);
        LocalDate fin = LocalDate.of(2026, 11, 10);
        when(typeCongeRepository.findById(1)).thenReturn(Optional.of(congePaye));
        when(demandeRepository.existsChevauchement(eq(10), eq(debut), eq(fin), anyCollection()))
                .thenReturn(true);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                service.soumettre(employe, 1, debut, fin, false, false, null, utilisateur(20, "MANAGER")));

        assertTrue(ex.getMessage().contains("chevauche"));
        verify(demandeRepository, never()).save(any());
    }

    @Test
    void soumettreRespecteLePreavisConfigure() {
        congePaye.setPreavisJours(10);
        LocalDate debut = LocalDate.now().plusDays(3);
        when(typeCongeRepository.findById(1)).thenReturn(Optional.of(congePaye));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                service.soumettre(employe, 1, debut, debut, false, false, null, utilisateur(20, "MANAGER")));

        assertTrue(ex.getMessage().contains("preavis"));
        verify(demandeRepository, never()).save(any());
    }

    @Test
    void soumettreRespecteLaDureeMaximaleConfiguree() {
        congePaye.setDureeMaxJours(new BigDecimal("2.0"));
        LocalDate debut = LocalDate.of(2026, 11, 9);
        LocalDate fin = LocalDate.of(2026, 11, 11);
        when(typeCongeRepository.findById(1)).thenReturn(Optional.of(congePaye));
        when(demandeRepository.existsChevauchement(eq(10), eq(debut), eq(fin), anyCollection())).thenReturn(false);
        when(jourFerieRepository.findByDateJourFerieBetween(debut, fin)).thenReturn(List.of());

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                service.soumettre(employe, 1, debut, fin, false, false, null, utilisateur(20, "MANAGER")));

        assertTrue(ex.getMessage().contains("maximale"));
        verify(demandeRepository, never()).save(any());
    }

    @Test
    void soumettreRefuseUnSoldeInsuffisant() {
        LocalDate debut = LocalDate.of(2026, 11, 9);
        LocalDate fin = LocalDate.of(2026, 11, 11);
        when(typeCongeRepository.findById(1)).thenReturn(Optional.of(congePaye));
        when(demandeRepository.existsChevauchement(eq(10), eq(debut), eq(fin), anyCollection()))
                .thenReturn(false);
        when(jourFerieRepository.findByDateJourFerieBetween(debut, fin)).thenReturn(List.of());
        when(soldeRepository.totalDisponible(eq(10), eq(1), any(LocalDate.class)))
                .thenReturn(new BigDecimal("2.0"));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                service.soumettre(employe, 1, debut, fin, false, false, null, utilisateur(20, "MANAGER")));

        assertTrue(ex.getMessage().contains("Solde insuffisant"));
        verify(demandeRepository, never()).save(any());
    }

    @Test
    void typeSansDecompteNeConsommePasDeSolde() {
        TypeConge maladie = TypeConge.builder().idTypeConge(3).code("MALADIE")
                .libelle("Maladie").decompteSolde(false).actif(true).build();
        LocalDate debut = LocalDate.of(2026, 11, 9);
        LocalDate fin = LocalDate.of(2026, 11, 10);
        when(typeCongeRepository.findById(3)).thenReturn(Optional.of(maladie));
        when(demandeRepository.existsChevauchement(eq(10), eq(debut), eq(fin), anyCollection()))
                .thenReturn(false);
        when(jourFerieRepository.findByDateJourFerieBetween(debut, fin)).thenReturn(List.of());
        when(demandeRepository.save(any(DemandeConge.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.soumettre(employe, 3, debut, fin, false, false, null, utilisateur(20, "MANAGER"));

        verify(soldeRepository, never()).totalDisponible(any(), any(), any());
        verify(consommationRepository, never()).save(any());
    }

    @Test
    void refusManagerRecrediteLesJoursReserves() {
        DemandeConge demande = DemandeConge.builder().idDemandeConge(55).employe(employe)
                .typeConge(congePaye).statut(StatutDemandeConge.SOUMISE)
                .dateDebut(LocalDate.of(2026, 11, 9)).dateFin(LocalDate.of(2026, 11, 10))
                .nombreJours(new BigDecimal("2.0")).build();
        SoldeConge lot = lot(100, "8.0", LocalDate.of(2027, 12, 31));
        ConsommationConge consommation = ConsommationConge.builder()
                .soldeConge(lot).demandeConge(demande).quantite(new BigDecimal("2.0")).build();
        when(demandeRepository.findById(55)).thenReturn(Optional.of(demande));
        when(consommationRepository.findByDemandeCongeIdDemandeConge(55)).thenReturn(List.of(consommation));
        when(demandeRepository.save(demande)).thenReturn(demande);

        service.validerManager(55, false, "Periode chargee", utilisateur(1, "ADMINISTRATEUR"));

        assertEquals(StatutDemandeConge.REFUSEE, demande.getStatut());
        assertEquals(0, new BigDecimal("10.0").compareTo(lot.getQuantiteRestante()));
        verify(consommationRepository).deleteByDemandeCongeIdDemandeConge(55);
    }

    @Test
    void demandeLongueAttendLaDirectionApresAccordManager() {
        DemandeConge demande = DemandeConge.builder().idDemandeConge(80).employe(employe)
                .typeConge(TypeConge.builder().idTypeConge(9).validationDirection(true).build())
                .statut(StatutDemandeConge.SOUMISE).nombreJours(new BigDecimal("15"))
                .build();
        when(demandeRepository.findById(80)).thenReturn(Optional.of(demande));
        when(demandeRepository.save(demande)).thenReturn(demande);

        service.validerManager(80, true, null, utilisateur(1, "ADMINISTRATEUR"));

        assertEquals(StatutDemandeConge.ATTENTE_DIRECTION, demande.getStatut());
    }

    @Test
    void validationDirectionAccordeeEnvoieLaDemandeAlaRh() {
        DemandeConge demande = DemandeConge.builder().idDemandeConge(81).employe(employe)
                .typeConge(congePaye).statut(StatutDemandeConge.ATTENTE_DIRECTION).build();
        when(demandeRepository.findById(81)).thenReturn(Optional.of(demande));
        when(demandeRepository.save(demande)).thenReturn(demande);

        service.validerDirection(81, true, "Accord", utilisateur(2, "DIRECTION"));

        assertEquals(StatutDemandeConge.VALIDEE_MANAGER, demande.getStatut());
        assertEquals("Accord", demande.getCommentaireDirection());
    }

    @Test
    void fileDeValidationManagerSuitLaPremiereDateDeSoumission() {
        Utilisateur manager = utilisateur(20, "MANAGER", Employe.builder().idEmp(20)
                .departement(Departement.builder().idDept(4).build()).build());
        List<DemandeConge> ordreArrivee = List.of(
                DemandeConge.builder().idDemandeConge(2).dateSoumission(LocalDateTime.of(2026, 1, 1, 9, 0)).build(),
                DemandeConge.builder().idDemandeConge(1).dateSoumission(LocalDateTime.of(2026, 1, 1, 10, 0)).build());
        when(utilisateurRepository.trouverManagersParDepartement(4)).thenReturn(List.of(manager));
        when(demandeRepository.findByStatutInAndEmployeDepartementIdDeptOrderByDateSoumissionAscIdDemandeCongeAsc(
                List.of(StatutDemandeConge.SOUMISE), 4)).thenReturn(ordreArrivee);

        assertEquals(ordreArrivee, service.demandesEnAttenteManager(manager));
        verify(demandeRepository).findByStatutInAndEmployeDepartementIdDeptOrderByDateSoumissionAscIdDemandeCongeAsc(
                List.of(StatutDemandeConge.SOUMISE), 4);
    }

    @Test
    void unManagerHorsDepartementNePeutPasValider() {
        Employe autreDept = Employe.builder().idEmp(99).nomEmp("Autre")
                .departement(Departement.builder().idDept(9).nomDept("Ventes").build()).build();
        Utilisateur manager = utilisateur(20, "MANAGER");
        manager.setEmploye(autreDept);
        DemandeConge demande = DemandeConge.builder().idDemandeConge(56).employe(employe)
                .typeConge(congePaye).statut(StatutDemandeConge.SOUMISE).build();
        when(demandeRepository.findById(56)).thenReturn(Optional.of(demande));

        assertThrows(AccesRefuseException.class,
                () -> service.validerManager(56, true, null, manager));

        verify(demandeRepository, never()).save(any());
    }

    @Test
    void annulationRefuseeApresValidationRh() {
        DemandeConge demande = DemandeConge.builder().idDemandeConge(57).employe(employe)
                .typeConge(congePaye).statut(StatutDemandeConge.VALIDEE_RH).build();
        when(demandeRepository.findById(57)).thenReturn(Optional.of(demande));

        assertThrows(IllegalStateException.class,
                () -> service.annuler(57, utilisateur(30, "EMPLOYE", employe)));

        verify(consommationRepository, never()).deleteByDemandeCongeIdDemandeConge(any());
    }

    private SoldeConge lot(Integer id, String restant, LocalDate expiration) {
        BigDecimal quantite = new BigDecimal(restant);
        return SoldeConge.builder().idSoldeConge(id).employe(employe).typeConge(congePaye)
                .dateAcquisition(LocalDate.of(2026, 1, 1)).dateExpiration(expiration)
                .quantiteInitiale(quantite).quantiteRestante(quantite)
                .dateCreation(LocalDateTime.now()).build();
    }

    private Utilisateur utilisateur(Integer id, String role) {
        return utilisateur(id, role, null);
    }

    private Utilisateur utilisateur(Integer id, String role, Employe rattache) {
        return Utilisateur.builder().idUser(id).username("user" + id)
                .role(Role.builder().idRole(1).nomRole(role).build())
                .employe(rattache).build();
    }
}
