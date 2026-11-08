package com.gestion.controller;

import com.gestion.model.Candidature;
import com.gestion.model.QuestionContrat;
import com.gestion.repository.CandidatureRepository;
import com.gestion.service.CandidatureStatusService;
import com.gestion.service.NegociationContratService;
import com.gestion.service.QuestionContratService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EspacePagesTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private CandidatureRepository candidatureRepository;

    @Autowired
    private CandidatureStatusService candidatureStatusService;

    @Autowired
    private NegociationContratService negociationContratService;

    @Autowired
    private QuestionContratService questionContratService;

    @Test
    void leTableauDeBordAfficheLesRaccourcisDuRole() throws Exception {
        mockMvc.perform(get("/tableau-de-bord").with(user(utilisateur("admin"))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Tableau de bord")))
                .andExpect(content().string(containsString("Gérer les offres")))
                .andExpect(content().string(containsString("Validation Direction")));
    }

    @Test
    void leMenuRhNeMontrePasLaPostulationCandidat() throws Exception {
        mockMvc.perform(get("/demandes").with(user(utilisateur("rh.sophie"))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Candidatures RH")))
                .andExpect(content().string(containsString("Gérer les offres")))
                .andExpect(content().string(not(containsString("href=\"/candidatures\""))))
                .andExpect(content().string(not(containsString("Postuler"))));
    }

    @Test
    void leProfilAfficheLeRoleEtLeMenuDuCandidat() throws Exception {
        mockMvc.perform(get("/profil").with(user(utilisateur("user.simple"))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Mon profil")))
                .andExpect(content().string(containsString("Utilisateur")))
                .andExpect(content().string(containsString("Se deconnecter")))
                .andExpect(content().string(not(containsString("Validation RH"))))
                .andExpect(content().string(not(containsString("Gérer les offres"))));
    }

    @Test
    void laPageNotificationsEstAccessible() throws Exception {
        mockMvc.perform(get("/notifications").with(user(utilisateur("rh.sophie"))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Notifications")))
                .andExpect(content().string(containsString("Aucune notification")));
    }

    @Test
    void lIconeDeNotificationsEstDansLaSidebar() throws Exception {
        mockMvc.perform(get("/profil").with(user(utilisateur("paul.rabe"))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"/notifications\"")))
                .andExpect(content().string(containsString("class=\"sidebar-notifications\"")))
                .andExpect(content().string(not(containsString("header-notifications"))));
    }

    @Test
    void laGestionDuQuestionnaireContratEstAccessibleAuxRh() throws Exception {
        mockMvc.perform(get("/contrats/questionnaire").with(user(utilisateur("rh.sophie"))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Questionnaire du contrat")))
                .andExpect(content().string(containsString("Ajouter une question")));
    }

    @Test
    void leCandidatConsulteLeQuestionnaireDeSaCandidature() throws Exception {
        mockMvc.perform(get("/candidatures/1/questionnaire-contrat").with(user(utilisateur("user.simple"))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Questionnaire du contrat")))
                .andExpect(content().string(containsString("Etat de votre dossier")));
    }

    @Test
    void laPageDEtudeDePropositionEstAccessibleAuxRh() throws Exception {
        mockMvc.perform(get("/candidatures/rh/1/propositions").with(user(utilisateur("rh.sophie"))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Etude de la proposition")));
    }

    @Test
    void leFormulaireDuQuestionnaireSAppuieSurLesQuestionsActives() throws Exception {
        Candidature candidature = candidatureRepository.findById(1).orElseThrow();
        candidature.setStatut(candidatureStatusService.trouverParLibelle("RETENU").orElseThrow());
        candidatureRepository.saveAndFlush(candidature);

        mockMvc.perform(get("/candidatures/1/questionnaire-contrat")
                        .with(user(utilisateur("user.simple"))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Vos choix")))
                .andExpect(content().string(containsString("J'accepte le salaire mensuel brut propose")))
                .andExpect(content().string(containsString("Je refuse le contrat")));
    }

    @Test
    void leDossierRhAfficheUnePropositionDeSalaire() throws Exception {
        Candidature candidature = candidatureRepository.findById(1).orElseThrow();
        candidature.setStatut(candidatureStatusService.trouverParLibelle("RETENU").orElseThrow());
        candidatureRepository.saveAndFlush(candidature);

        Map<Integer, Boolean> acceptations = new HashMap<>();
        for (QuestionContrat question : questionContratService.listerActives()) {
            acceptations.put(question.getIdQuestionContrat(), true);
        }
        negociationContratService.soumettreParCandidat(1, candidature.getUtilisateur().getIdUser(),
                false, false, 3_300_000, acceptations, Map.of());

        mockMvc.perform(get("/candidatures/rh/1").with(user(utilisateur("rh.sophie"))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Etudier la proposition du candidat")))
                .andExpect(content().string(containsString("3300000")));
    }

    private UserDetails utilisateur(String username) {
        return userDetailsService.loadUserByUsername(username);
    }
}
