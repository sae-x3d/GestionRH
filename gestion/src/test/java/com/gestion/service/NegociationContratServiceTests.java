package com.gestion.service;

import com.gestion.model.Candidature;
import com.gestion.model.QuestionContrat;
import com.gestion.model.ReponseContrat;
import com.gestion.repository.CandidatureRepository;
import com.gestion.repository.ReponseContratRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class NegociationContratServiceTests {

    @Autowired
    private NegociationContratService service;

    @Autowired
    private CandidatureRepository candidatureRepository;

    @Autowired
    private ReponseContratRepository reponseContratRepository;

    @Autowired
    private CandidatureStatusService candidatureStatusService;

    @Autowired
    private QuestionContratService questionContratService;

    private Candidature candidature;
    private Integer idCandidature;
    private Integer idUser;

    @BeforeEach
    void setUp() {
        candidature = candidatureRepository.findByIdAvecDetails(1).orElseThrow();
        idCandidature = candidature.getIdCandidature();
        idUser = candidature.getUtilisateur().getIdUser();
        candidature.setStatut(candidatureStatusService.trouverParLibelle("RETENU").orElseThrow());
        candidature.setSalaireConvenu(null);
        candidatureRepository.saveAndFlush(candidature);
    }

    @Test
    void leCandidatQuiAccepteToutPasseLaCandidatureEnAccepteeAvecLeSalaireDeLOffre() {
        Candidature resultat = service.soumettreParCandidat(idCandidature, idUser,
                false, true, null, toutesAcceptees(), Map.of());

        assertThat(resultat.getStatut().getLibelle()).isEqualTo("ACCEPTEE");
        assertThat(resultat.getSalaireConvenu()).isEqualTo(candidature.getOffre().getSalaire());

        List<ReponseContrat> tour = service.dernierTour(idCandidature);
        assertThat(tour).hasSize(questionContratService.listerActives().size() + 1);
        assertThat(service.salaireCourant(resultat, tour)).isEqualTo(candidature.getOffre().getSalaire());
    }

    @Test
    void uneNegociationSurLeSalairePasseEnPropositionPuisPermetUneContrePropositionRh() {
        Candidature proposition = service.soumettreParCandidat(idCandidature, idUser,
                false, false, 3_500_000, toutesAcceptees(), Map.of());
        assertThat(proposition.getStatut().getLibelle()).isEqualTo("PROPOSITION");

        Candidature contreProposition = service.contreProposer(idCandidature, 3_400_000,
                toutesAcceptees(), Map.of(), "Montant ajuste");
        assertThat(contreProposition.getStatut().getLibelle()).isEqualTo("RETENU");
        assertThat(service.dernierTour(idCandidature))
                .allMatch(reponse -> NegociationContratService.AUTEUR_RH.equals(reponse.getAuteur()));
        assertThat(service.salaireCourant(contreProposition, service.dernierTour(idCandidature)))
                .isEqualTo(3_400_000);

        Candidature acceptation = service.soumettreParCandidat(idCandidature, idUser,
                false, true, null, toutesAcceptees(), Map.of());
        assertThat(acceptation.getStatut().getLibelle()).isEqualTo("ACCEPTEE");
        assertThat(acceptation.getSalaireConvenu()).isEqualTo(3_400_000);
    }

    @Test
    void lesRhPeuventAccepterLaPropositionDuCandidat() {
        service.soumettreParCandidat(idCandidature, idUser,
                false, false, 3_500_000, toutesAcceptees(), Map.of());

        Candidature resultat = service.accepterProposition(idCandidature);

        assertThat(resultat.getStatut().getLibelle()).isEqualTo("ACCEPTEE");
        assertThat(resultat.getSalaireConvenu()).isEqualTo(3_500_000);
        assertThat(reponseContratRepository
                .findByCandidatureIdCandidatureAndNumeroTourOrderByIdReponseContratAsc(idCandidature, 1))
                .isNotEmpty();
    }

    @Test
    void unRefusGlobalCloseLaCandidature() {
        Candidature resultat = service.soumettreParCandidat(idCandidature, idUser,
                true, false, null, Map.of(), Map.of());

        assertThat(resultat.getStatut().getLibelle()).isEqualTo("CONTRAT_REFUSE");
        assertThat(resultat.getDateDecision()).isNotNull();
    }

    private Map<Integer, Boolean> toutesAcceptees() {
        Map<Integer, Boolean> acceptations = new HashMap<>();
        for (QuestionContrat question : questionContratService.listerActives()) {
            acceptations.put(question.getIdQuestionContrat(), true);
        }
        return acceptations;
    }
}
