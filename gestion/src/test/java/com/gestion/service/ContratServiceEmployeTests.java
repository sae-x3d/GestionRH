package com.gestion.service;

import com.gestion.config.ParametresContrat;
import com.gestion.model.Candidature;
import com.gestion.model.Contrat;
import com.gestion.model.Demande;
import com.gestion.model.Departement;
import com.gestion.model.Employe;
import com.gestion.model.OffreEmploi;
import com.gestion.model.Poste;
import com.gestion.model.Provenance;
import com.gestion.model.Utilisateur;
import com.gestion.repository.CandidatureRepository;
import com.gestion.repository.ContratRepository;
import com.gestion.repository.EmployeRepository;
import com.gestion.repository.ProvenanceRepository;
import com.gestion.repository.UtilisateurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContratServiceEmployeTests {

    @Mock private ContratRepository contratRepository;
    @Mock private CandidatureRepository candidatureRepository;
    @Mock private EmployeRepository employeRepository;
    @Mock private UtilisateurRepository utilisateurRepository;
    @Mock private ProvenanceRepository provenanceRepository;
    @Mock private ContratPdfService pdfService;
    @Mock private ParametresContrat parametresContrat;
    @Mock private CandidatureStatusService candidatureStatusService;
    @Mock private NotificationService notificationService;

    private ContratService service;
    private Utilisateur utilisateur;
    private Contrat contrat;
    private Provenance provenance;
    private Poste poste;
    private Departement departement;

    @BeforeEach
    void setUp() {
        service = new ContratService(contratRepository, candidatureRepository, employeRepository,
                utilisateurRepository, provenanceRepository, pdfService, parametresContrat,
                candidatureStatusService, notificationService);

        utilisateur = Utilisateur.builder().idUser(7).username("candidat").build();
        poste = Poste.builder().idPoste(3).nomPoste("Developpeur").build();
        departement = Departement.builder().idDept(4).nomDept("Informatique").build();
        Demande demande = Demande.builder().poste(poste).departement(departement).build();
        OffreEmploi offre = OffreEmploi.builder().demande(demande).titre("Developpeur").build();
        Candidature candidature = Candidature.builder().idCandidature(12)
                .utilisateur(utilisateur).offre(offre).build();
        contrat = Contrat.builder().idContrat(21).candidature(candidature)
                .nomSalarie("Kenny Test").dateEffet(LocalDate.of(2026, 11, 1))
                .salaire(2_500_000).statut("EN_ATTENTE_ACCEPTATION")
                .dateGeneration(LocalDateTime.of(2026, 10, 23, 10, 0)).build();
        provenance = Provenance.builder().idProvenance(1).libelle("Recrutement externe").build();

    }

    @Test
    void acceptationCreeEtRelieUneFicheEmployeAvecLesDonneesDuContratEtDeLaDemande() {
        stubAcceptation();
        when(provenanceRepository.findByLibelleIgnoreCase("Recrutement externe"))
                .thenReturn(Optional.of(provenance));
        when(employeRepository.save(any(Employe.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Contrat resultat = service.repondre(21, 7, true);

        Employe employe = resultat.getEmploye();
        assertNotNull(employe);
        assertEquals("Kenny Test", employe.getNomEmp());
        assertEquals(LocalDate.of(2026, 11, 1).atStartOfDay(), employe.getDateEmbauche());
        assertSame(poste, employe.getPoste());
        assertSame(departement, employe.getDepartement());
        assertEquals(2_500_000, employe.getSalaireEmp());
        assertSame(provenance, employe.getProvenance());
        assertSame(employe, utilisateur.getEmploye());
        assertEquals("ACCEPTE", resultat.getStatut());
        assertArrayEquals(new byte[]{1, 2, 3}, resultat.getPdf());
        verify(employeRepository, times(1)).save(any(Employe.class));
        verify(utilisateurRepository).save(utilisateur);
    }

    @Test
    void nouvelAppelApresAcceptationNeCreePasUneSecondeFiche() {
        stubAcceptation();
        when(provenanceRepository.findByLibelleIgnoreCase("Recrutement externe"))
                .thenReturn(Optional.of(provenance));
        when(employeRepository.save(any(Employe.class))).thenAnswer(invocation -> invocation.getArgument(0));
        service.repondre(21, 7, true);

        assertThrows(IllegalStateException.class, () -> service.repondre(21, 7, true));

        verify(employeRepository, times(1)).save(any(Employe.class));
    }

    @Test
    void candidatDejaLieAUnEmployeEstReutiliseSansDoublon() {
        stubAcceptation();
        Employe existant = Employe.builder().idEmp(99).nomEmp("Kenny Test").build();
        utilisateur.setEmploye(existant);

        Contrat resultat = service.repondre(21, 7, true);

        assertSame(existant, resultat.getEmploye());
        verifyNoInteractions(employeRepository, provenanceRepository);
        verify(utilisateurRepository, never()).save(any(Utilisateur.class));
    }

    @Test
    void refuseLaReponseDUnAutreUtilisateurSansCreerDeFiche() {
        when(contratRepository.findById(21)).thenReturn(Optional.of(contrat));
        assertThrows(IllegalStateException.class, () -> service.repondre(21, 8, true));

        verifyNoInteractions(employeRepository, provenanceRepository);
        verify(utilisateurRepository, never()).verrouillerParId(anyInt());
    }

    @Test
    void refuseDeCreerLaFicheSiPosteOuDepartementManquant() {
        when(contratRepository.findById(21)).thenReturn(Optional.of(contrat));
        when(utilisateurRepository.verrouillerParId(7)).thenReturn(Optional.of(utilisateur));
        contrat.getCandidature().getOffre().getDemande().setDepartement(null);

        assertThrows(IllegalStateException.class, () -> service.repondre(21, 7, true));

        verifyNoInteractions(employeRepository, provenanceRepository);
    }

    private void stubAcceptation() {
        when(contratRepository.findById(21)).thenReturn(Optional.of(contrat));
        when(utilisateurRepository.verrouillerParId(7)).thenReturn(Optional.of(utilisateur));
        when(contratRepository.save(any(Contrat.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pdfService.generer(contrat)).thenReturn(new byte[]{1, 2, 3});
    }
}
