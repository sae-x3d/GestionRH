package com.gestion.controller;

import com.gestion.model.Utilisateur;
import com.gestion.repository.UtilisateurRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class InscriptionControllerTests {

    private static final String MOT_DE_PASSE = "MotDePasse2026";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void laPageDInscriptionEstPublique() throws Exception {
        mockMvc.perform(get("/inscription"))
                .andExpect(status().isOk())
                .andExpect(view().name("inscription"))
                .andExpect(model().attributeExists("form"))
                .andExpect(content().string(containsString("Creer un compte")))
                .andExpect(content().string(containsString("data-password-toggle=\"password\"")))
                .andExpect(content().string(containsString("data-password-toggle=\"confirmation\"")))
                .andExpect(content().string(containsString("/js/password-toggle.js")));
    }

    @Test
    void uneInscriptionValideCreeUnCompteCandidatEtConnecte() throws Exception {
        mockMvc.perform(post("/inscription").with(csrf())
                        .param("prenom", "Nouveau")
                        .param("nom", "Candidat")
                        .param("email", "Nouveau.Candidat@Example.com")
                        .param("telephone", "+261 34 00 000 00")
                        .param("username", "nouveau.candidat")
                        .param("password", MOT_DE_PASSE)
                        .param("confirmation", MOT_DE_PASSE))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/offres"));

        Optional<Utilisateur> utilisateur = utilisateurRepository
                .findByUsernameWithRole("nouveau.candidat");
        assertThat(utilisateur).isPresent();
        assertThat(utilisateur.get().getRole().getNomRole()).isEqualTo("Utilisateur");
        assertThat(utilisateur.get().getPrenom()).isEqualTo("Nouveau");
        assertThat(utilisateur.get().getNom()).isEqualTo("Candidat");
        assertThat(utilisateur.get().getEmail()).isEqualTo("nouveau.candidat@example.com");
        assertThat(utilisateur.get().getTelephone()).isEqualTo("+261 34 00 000 00");
        assertThat(utilisateur.get().getEmploye()).isNull();
        assertThat(passwordEncoder.matches(MOT_DE_PASSE, utilisateur.get().getPasswordHash())).isTrue();
    }

    @Test
    void lOffreEstConserveeApresLInscription() throws Exception {
        mockMvc.perform(post("/inscription").with(csrf())
                        .param("prenom", "Nouveau")
                        .param("nom", "Candidat")
                        .param("email", "offre.candidat@example.com")
                        .param("telephone", "0340000000")
                        .param("username", "offre.candidat")
                        .param("password", MOT_DE_PASSE)
                        .param("confirmation", MOT_DE_PASSE)
                        .param("offreId", "7"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/candidatures/postuler?offreId=7"));
    }

    @Test
    void unNomDUtilisateurDejaPrisEstRefuse() throws Exception {
        mockMvc.perform(post("/inscription").with(csrf())
                        .param("prenom", "Nouveau")
                        .param("nom", "Candidat")
                        .param("email", "autre.candidat@example.com")
                        .param("telephone", "0340000000")
                        .param("username", "USER.SIMPLE")
                        .param("password", MOT_DE_PASSE)
                        .param("confirmation", MOT_DE_PASSE))
                .andExpect(status().isOk())
                .andExpect(view().name("inscription"))
                .andExpect(content().string(containsString("deja utilise")));
    }

    @Test
    void unEmailDejaUtiliseEstRefuse() throws Exception {
        mockMvc.perform(post("/inscription").with(csrf())
                        .param("prenom", "Premier")
                        .param("nom", "Candidat")
                        .param("email", "partage@example.com")
                        .param("telephone", "0340000000")
                        .param("username", "premier.candidat")
                        .param("password", MOT_DE_PASSE)
                        .param("confirmation", MOT_DE_PASSE))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(post("/inscription").with(csrf())
                        .param("prenom", "Second")
                        .param("nom", "Candidat")
                        .param("email", "PARTAGE@example.com")
                        .param("telephone", "0340000001")
                        .param("username", "second.candidat")
                        .param("password", MOT_DE_PASSE)
                        .param("confirmation", MOT_DE_PASSE))
                .andExpect(status().isOk())
                .andExpect(view().name("inscription"))
                .andExpect(content().string(containsString("deja utilisee")));
    }

    @Test
    void uneConfirmationDifferenteEstRefusee() throws Exception {
        mockMvc.perform(post("/inscription").with(csrf())
                        .param("prenom", "Nouveau")
                        .param("nom", "Candidat")
                        .param("email", "confirmation@example.com")
                        .param("telephone", "0340000000")
                        .param("username", "confirmation.candidat")
                        .param("password", MOT_DE_PASSE)
                        .param("confirmation", "AutreMotDePasse2026"))
                .andExpect(status().isOk())
                .andExpect(view().name("inscription"))
                .andExpect(content().string(containsString("ne correspondent pas")));

        assertThat(utilisateurRepository.findByUsernameWithRole("confirmation.candidat")).isEmpty();
    }

    @Test
    void unEmailInvalideEstRefuse() throws Exception {
        mockMvc.perform(post("/inscription").with(csrf())
                        .param("prenom", "Nouveau")
                        .param("nom", "Candidat")
                        .param("email", "pas-un-email")
                        .param("telephone", "0340000000")
                        .param("username", "email.invalide")
                        .param("password", MOT_DE_PASSE)
                        .param("confirmation", MOT_DE_PASSE))
                .andExpect(status().isOk())
                .andExpect(view().name("inscription"));

        assertThat(utilisateurRepository.findByUsernameWithRole("email.invalide")).isEmpty();
    }
}
