package com.gestion.controller;

import com.gestion.exception.AccesRefuseException;
import com.gestion.exception.DecisionRhImpossibleException;
import com.gestion.model.Demande;
import com.gestion.model.Utilisateur;
import com.gestion.repository.DemandeRepository;
import com.gestion.service.DemandeService;
import com.gestion.service.DemandeStatusService;
import com.gestion.service.UtilisateurDetails;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DemandeValidationRhTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private DemandeService demandeService;

    @Autowired
    private DemandeRepository demandeRepository;

    @Test
    void laFileRhAfficheLesDemandesEnAttente() throws Exception {
        mockMvc.perform(get("/demandes/validation-rh").with(user(utilisateur("rh.sophie"))))
                .andExpect(status().isOk())
                .andExpect(model().attribute("demandes", hasSize(3)))
                .andExpect(content().string(containsString("Développeur Backend")))
                .andExpect(content().string(containsString("Développeur Frontend")))
                .andExpect(content().string(containsString("Comptable")))
                .andExpect(content().string(not(containsString("Assistant RH"))));
    }

    @Test
    void unManagerNePeutPasAccederALaFileRh() throws Exception {
        mockMvc.perform(get("/demandes/validation-rh").with(user(utilisateur("manager.it"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void unEmployeNePeutPasAccederALaFileRh() throws Exception {
        mockMvc.perform(get("/demandes/validation-rh").with(user(utilisateur("paul.rabe"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void unAdministrateurNePeutPasAccederALaFileRh() throws Exception {
        mockMvc.perform(get("/demandes/validation-rh").with(user(utilisateur("admin"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void unNonAuthentifieEstRedirigeVersLaConnexion() throws Exception {
        mockMvc.perform(get("/demandes/validation-rh"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/connexion"));
    }

    @Test
    void laRhAccepteUneDemandeEtEnregistreLaDateDeReponse() throws Exception {
        mockMvc.perform(post("/demandes/1/validation-rh/accepter")
                        .with(user(utilisateur("rh.sophie")))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/demandes/validation-rh"))
                .andExpect(flash().attribute("succes", containsString("acceptée")));

        Demande demande = demandeRepository.findById(1).orElseThrow();
        assertThat(demande.getDemandeStatus().getLibelleStatus())
                .isEqualTo(DemandeStatusService.EN_ATTENTE_VALIDATION_BUDGET);
        assertThat(demande.getDateReponse()).isNotNull();
    }

    @Test
    void laRhRefuseUneDemandeEtEnregistreLaDateDeReponse() throws Exception {
        mockMvc.perform(post("/demandes/2/validation-rh/refuser")
                        .with(user(utilisateur("rh.nina")))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/demandes/validation-rh"))
                .andExpect(flash().attribute("succes", containsString("refusée")));

        Demande demande = demandeRepository.findById(2).orElseThrow();
        assertThat(demande.getDemandeStatus().getLibelleStatus())
                .isEqualTo(DemandeStatusService.REFUSE);
        assertThat(demande.getDateReponse()).isNotNull();
    }

    @Test
    void uneDemandeQuiNEstPasEnAttenteRhNePeutPasEtreTraitee() throws Exception {
        LocalDateTime dateReponseAvant = demandeRepository.findById(4).orElseThrow().getDateReponse();

        mockMvc.perform(post("/demandes/4/validation-rh/accepter")
                        .with(user(utilisateur("rh.sophie")))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/demandes/validation-rh"))
                .andExpect(flash().attribute("erreur", containsString("pas en attente")));

        Demande demande = demandeRepository.findById(4).orElseThrow();
        assertThat(demande.getDemandeStatus().getLibelleStatus()).isEqualTo(DemandeStatusService.APPROUVE);
        assertThat(demande.getDateReponse()).isEqualTo(dateReponseAvant);
    }

    @Test
    void uneDemandeNePeutPasEtreTraiteeDeuxFois() throws Exception {
        mockMvc.perform(post("/demandes/1/validation-rh/accepter")
                        .with(user(utilisateur("rh.sophie")))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());

        LocalDateTime premiereDateReponse = demandeRepository.findById(1).orElseThrow().getDateReponse();

        mockMvc.perform(post("/demandes/1/validation-rh/refuser")
                        .with(user(utilisateur("rh.sophie")))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("erreur", containsString("pas en attente")));

        Demande demande = demandeRepository.findById(1).orElseThrow();
        assertThat(demande.getDemandeStatus().getLibelleStatus())
                .isEqualTo(DemandeStatusService.EN_ATTENTE_VALIDATION_BUDGET);
        assertThat(demande.getDateReponse()).isEqualTo(premiereDateReponse);
    }

    @Test
    void unManagerNePeutPasDecider() throws Exception {
        mockMvc.perform(post("/demandes/1/validation-rh/accepter")
                        .with(user(utilisateur("manager.it")))
                        .with(csrf()))
                .andExpect(status().isForbidden());

        assertThat(demandeRepository.findById(1).orElseThrow().getDemandeStatus().getLibelleStatus())
                .isEqualTo(DemandeStatusService.EN_ATTENTE_VALIDATION_RH);
    }

    @Test
    void unEmployeNePeutPasDecider() throws Exception {
        mockMvc.perform(post("/demandes/1/validation-rh/refuser")
                        .with(user(utilisateur("paul.rabe")))
                        .with(csrf()))
                .andExpect(status().isForbidden());

        assertThat(demandeRepository.findById(1).orElseThrow().getDemandeStatus().getLibelleStatus())
                .isEqualTo(DemandeStatusService.EN_ATTENTE_VALIDATION_RH);
    }

    @Test
    void unAdministrateurNePeutPasDecider() throws Exception {
        mockMvc.perform(post("/demandes/1/validation-rh/accepter")
                        .with(user(utilisateur("admin")))
                        .with(csrf()))
                .andExpect(status().isForbidden());

        assertThat(demandeRepository.findById(1).orElseThrow().getDemandeStatus().getLibelleStatus())
                .isEqualTo(DemandeStatusService.EN_ATTENTE_VALIDATION_RH);
    }

    @Test
    void leCsrfEstObligatoirePourDecider() throws Exception {
        mockMvc.perform(post("/demandes/1/validation-rh/accepter")
                        .with(user(utilisateur("rh.sophie"))))
                .andExpect(status().isForbidden());

        assertThat(demandeRepository.findById(1).orElseThrow().getDemandeStatus().getLibelleStatus())
                .isEqualTo(DemandeStatusService.EN_ATTENTE_VALIDATION_RH);
    }

    @Test
    void uneNouvelleDemandeEntreDansLaFileRh() throws Exception {
        mockMvc.perform(post("/demandes")
                        .with(user(utilisateur("manager.it")))
                        .with(csrf())
                        .param("posteId", "1")
                        .param("departementId", "1")
                        .param("motifId", "3")
                        .param("budget", "3500000")
                        .param("dateSouhaite", "2030-11-01T09:00"))
                .andExpect(status().is3xxRedirection());

        Demande nouvelle = demandeRepository.findAll().stream()
                .max(Comparator.comparing(Demande::getIdDemande))
                .orElseThrow();
        assertThat(nouvelle.getDemandeStatus().getLibelleStatus())
                .isEqualTo(DemandeStatusService.EN_ATTENTE_VALIDATION_RH);

        mockMvc.perform(get("/demandes/validation-rh").with(user(utilisateur("rh.sophie"))))
                .andExpect(status().isOk())
                .andExpect(model().attribute("demandes", hasSize(4)));
    }

    @Test
    void leServiceRefuseUneDecisionParUnNonRh() {
        Utilisateur manager = utilisateurEntite("manager.it");

        assertThatThrownBy(() -> demandeService.deciderRh(1, manager, true))
                .isInstanceOf(AccesRefuseException.class);

        assertThat(demandeRepository.findById(1).orElseThrow().getDemandeStatus().getLibelleStatus())
                .isEqualTo(DemandeStatusService.EN_ATTENTE_VALIDATION_RH);
    }

    @Test
    void leServiceRefuseUneDemandeQuiNEstPasEnAttenteRh() {
        Utilisateur rh = utilisateurEntite("rh.sophie");

        assertThatThrownBy(() -> demandeService.deciderRh(4, rh, true))
                .isInstanceOf(DecisionRhImpossibleException.class);
    }

    private UserDetails utilisateur(String username) {
        return userDetailsService.loadUserByUsername(username);
    }

    private Utilisateur utilisateurEntite(String username) {
        return ((UtilisateurDetails) userDetailsService.loadUserByUsername(username)).getUtilisateur();
    }
}
