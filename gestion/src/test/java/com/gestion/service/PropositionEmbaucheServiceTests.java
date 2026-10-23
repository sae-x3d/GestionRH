package com.gestion.service;

import com.gestion.model.Candidature;
import com.gestion.model.CandidatureStatus;
import com.gestion.model.PropositionEmbauche;
import com.gestion.model.Utilisateur;
import com.gestion.repository.CandidatureRepository;
import com.gestion.repository.EmployeRepository;
import com.gestion.repository.PropositionEmbaucheRepository;
import com.gestion.repository.UtilisateurRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PropositionEmbaucheServiceTests {

    @Mock
    private PropositionEmbaucheRepository propositionRepository;

    @Mock
    private CandidatureRepository candidatureRepository;

    @Mock
    private CandidatureStatusService candidatureStatusService;

    @Mock
    private EmployeRepository employeRepository;

    @Mock
    private UtilisateurRepository utilisateurRepository;

    @InjectMocks
    private PropositionEmbaucheService service;

    @Test
    void uneCandidatureAccepteeSansPropositionCreeLaRevision1() {
        Candidature candidature = candidature("ACCEPTEE");
        when(candidatureRepository.findById(10)).thenReturn(Optional.of(candidature));
        when(propositionRepository.findByCandidatureIdAvecDetails(10)).thenReturn(List.of());
        when(propositionRepository.save(any(PropositionEmbauche.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PropositionEmbauche proposition = service.genererProposition(10, rh(), "Developpeur", "IT",
                "Antananarivo", 4_000_000, "CDI", 3, LocalDate.now().plusDays(15),
                7, "Mutuelle", "Teletravail", "Bienvenue", null);

        assertThat(proposition.getNumeroRevision()).isEqualTo(1);
        assertThat(proposition.getStatut()).isEqualTo(PropositionEmbaucheService.STATUT_EN_ATTENTE);
        assertThat(proposition.getSalairePropose()).isEqualTo(4_000_000);
    }

    @Test
    void unePropositionRefuseePeutEtreReviseeEtReouvreLaCandidature() {
        Candidature candidature = candidature("REFUSEE");
        candidature.setDateDecision(LocalDateTime.now().minusDays(1));
        PropositionEmbauche precedente = proposition(candidature, 1, "REFUSEE");

        when(candidatureRepository.findById(10)).thenReturn(Optional.of(candidature));
        when(propositionRepository.findByCandidatureIdAvecDetails(10)).thenReturn(List.of(precedente));
        when(candidatureStatusService.trouverParLibelle("ACCEPTEE"))
                .thenReturn(Optional.of(CandidatureStatus.builder().idCandidatureStatus(5).libelle("ACCEPTEE").build()));
        when(propositionRepository.save(any(PropositionEmbauche.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PropositionEmbauche revision = service.genererProposition(10, rh(), null, null,
                null, 4_500_000, null, null, null, 10,
                null, null, "Nouvelle proposition apres negociation", "Salaire revu apres negociation");

        assertThat(revision.getNumeroRevision()).isEqualTo(2);
        assertThat(revision.getSalairePropose()).isEqualTo(4_500_000);
        assertThat(revision.getPostePropose()).isEqualTo("Developpeur");
        assertThat(revision.getMotifRevision()).isEqualTo("Salaire revu apres negociation");
        assertThat(candidature.getStatut().getLibelle()).isEqualTo("ACCEPTEE");
        assertThat(candidature.getDateDecision()).isNull();
        verify(candidatureRepository).save(candidature);
    }

    @Test
    void unePropositionEnAttenteNonExpireeNePeutPasEtreRevisee() {
        Candidature candidature = candidature("ACCEPTEE");
        PropositionEmbauche precedente = proposition(candidature, 1, "EN_ATTENTE");
        precedente.setDateExpiration(LocalDateTime.now().plusDays(3));

        when(candidatureRepository.findById(10)).thenReturn(Optional.of(candidature));
        when(propositionRepository.findByCandidatureIdAvecDetails(10)).thenReturn(List.of(precedente));

        assertThatThrownBy(() -> service.genererProposition(10, rh(), "Developpeur", "IT",
                "Antananarivo", 4_000_000, "CDI", 3, null, 7, null, null, null, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("deja en attente de reponse");

        verify(propositionRepository, never()).save(any(PropositionEmbauche.class));
    }

    @Test
    void unePropositionAccepteeNePeutPlusEtreRevisee() {
        Candidature candidature = candidature("ACCEPTEE");
        PropositionEmbauche precedente = proposition(candidature, 1, "ACCEPTEE");

        when(candidatureRepository.findById(10)).thenReturn(Optional.of(candidature));
        when(propositionRepository.findByCandidatureIdAvecDetails(10)).thenReturn(List.of(precedente));

        assertThatThrownBy(() -> service.genererProposition(10, rh(), "Developpeur", "IT",
                "Antananarivo", 4_000_000, "CDI", 3, null, 7, null, null, null, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("deja ete acceptee");
    }

    @Test
    void unePropositionEnAttenteExpireePeutEtreRevisee() {
        Candidature candidature = candidature("ACCEPTEE");
        PropositionEmbauche precedente = proposition(candidature, 1, "EN_ATTENTE");
        precedente.setDateExpiration(LocalDateTime.now().minusDays(1));

        when(candidatureRepository.findById(10)).thenReturn(Optional.of(candidature));
        when(propositionRepository.findByCandidatureIdAvecDetails(10)).thenReturn(List.of(precedente));
        when(propositionRepository.save(any(PropositionEmbauche.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PropositionEmbauche revision = service.genererProposition(10, rh(), "Developpeur", "IT",
                "Antananarivo", 4_000_000, "CDI", 3, null, 7, null, null, null, "Prolongation");

        assertThat(revision.getNumeroRevision()).isEqualTo(2);
        assertThat(precedente.getStatut()).isEqualTo(PropositionEmbaucheService.STATUT_EXPIREE);
    }

    private Candidature candidature(String statut) {
        return Candidature.builder()
                .idCandidature(10)
                .utilisateur(Utilisateur.builder().idUser(2).username("candidat").build())
                .statut(CandidatureStatus.builder().idCandidatureStatus(1).libelle(statut).build())
                .dateCandidature(LocalDateTime.now().minusDays(10))
                .build();
    }

    private PropositionEmbauche proposition(Candidature candidature, int numeroRevision, String statut) {
        return PropositionEmbauche.builder()
                .idProposition(1)
                .candidature(candidature)
                .candidat(candidature.getUtilisateur())
                .postePropose("Developpeur")
                .departementPropose("IT")
                .salairePropose(4_000_000)
                .dateProposition(LocalDateTime.now().minusDays(2))
                .statut(statut)
                .numeroRevision(numeroRevision)
                .build();
    }

    private Utilisateur rh() {
        return Utilisateur.builder().idUser(3).username("rh.sophie").build();
    }
}
