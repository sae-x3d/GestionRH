package com.gestion.controller;

import com.gestion.dto.CreateCandidatureDTO;
import com.gestion.dto.LigneParcoursDTO;
import com.gestion.enums.StatutOffre;
import com.gestion.model.Candidature;
import com.gestion.model.OffreEmploi;
import com.gestion.model.Utilisateur;
import com.gestion.repository.OffreEmploiRepository;
import com.gestion.service.CandidatureService;
import com.gestion.service.UtilisateurDetails;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CandidatureRhListeTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private CandidatureService candidatureService;

    @Autowired
    private OffreEmploiRepository offreRepository;

    @Test
    void laListeRhSansFiltreRepondSansErreur() throws Exception {
        mockMvc.perform(get("/candidatures/rh").with(user(utilisateur("rh.sophie"))))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("candidatures"));
    }

    @Test
    void laListeRhAvecFiltreStatutRepondSansErreur() throws Exception {
        mockMvc.perform(get("/candidatures/rh")
                        .param("statut", "RECUE")
                        .with(user(utilisateur("rh.sophie"))))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("candidatures"));
    }

    @Test
    void laListeRhAvecFiltreOffreEtStatutRepondSansErreur() throws Exception {
        mockMvc.perform(get("/candidatures/rh")
                        .param("offreId", "1")
                        .param("statut", "RECUE")
                        .with(user(utilisateur("rh.sophie"))))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("candidatures"));
    }

    @Test
    void laListeRhEstInterditeAUnEmploye() throws Exception {
        mockMvc.perform(get("/candidatures/rh").with(user(utilisateur("paul.rabe"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void leDossierRhAfficheLeCandidat() throws Exception {
        mockMvc.perform(get("/candidatures/rh/1").with(user(utilisateur("rh.sophie"))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("user.simple")));
    }

    @Test
    void leServiceRefuseUnDoublonDeCandidature() {
        Utilisateur utilisateur = utilisateurEntite("user.simple");

        assertThatThrownBy(() -> candidatureService.postuler(candidaturePour(1), null, utilisateur))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("deja postule");
    }

    @Test
    void leServiceRefuseUnePostulationAUneOffreNonPubliee() {
        Utilisateur utilisateur = utilisateurEntite("paul.rabe");

        assertThatThrownBy(() -> candidatureService.postuler(candidaturePour(3), null, utilisateur))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ouverte aux candidatures");
    }

    @Test
    void leServiceRefuseUnePostulationAUneOffreAuStatutCloturee() {
        Utilisateur utilisateur = utilisateurEntite("marie.randria");
        OffreEmploi offre = offreRepository.findById(2).orElseThrow();
        StatutOffre statutInitial = offre.getStatut();
        offre.setStatut(StatutOffre.CLOTUREE);
        offreRepository.saveAndFlush(offre);
        try {
            assertThatThrownBy(() -> candidatureService.postuler(candidaturePour(2), null, utilisateur))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("ouverte aux candidatures");
        } finally {
            offre.setStatut(statutInitial);
            offreRepository.saveAndFlush(offre);
        }
    }

    @Test
    void leServiceRefuseUnePostulationAUneOffreDontLaDateDeClotureEstDepassee() {
        Utilisateur utilisateur = utilisateurEntite("marie.randria");
        OffreEmploi offre = offreRepository.findById(2).orElseThrow();
        LocalDateTime dateCloture = offre.getDateCloture();
        offre.setDateCloture(LocalDateTime.now().minusDays(1));
        offreRepository.saveAndFlush(offre);
        try {
            assertThatThrownBy(() -> candidatureService.postuler(candidaturePour(2), null, utilisateur))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("fermee");
        } finally {
            offre.setDateCloture(dateCloture);
            offreRepository.saveAndFlush(offre);
        }
    }

    @Test
    void leServiceEnregistreUneCandidatureValideAvecStatutInitial() {
        Utilisateur utilisateur = utilisateurEntite("paul.rabe");

        Candidature candidature = candidatureService.postuler(candidaturePour(2), cvFactice(), utilisateur);

        assertThat(candidature.getIdCandidature()).isNotNull();
        assertThat(candidature.getStatut().getLibelle()).isEqualTo("RECUE");
        assertThat(candidature.getDateCandidature()).isNotNull();
        assertThat(candidature.getCvNum()).isNotBlank();
    }

    @Test
    void leServiceRefuseUneCandidatureSansCv() {
        Utilisateur utilisateur = utilisateurEntite("paul.rabe");

        assertThatThrownBy(() -> candidatureService.postuler(candidaturePour(2), null, utilisateur))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CV");
    }

    private MockMultipartFile cvFactice() {
        return new MockMultipartFile(
                "cv", "cv.pdf", "application/pdf", "contenu".getBytes());
    }    private CreateCandidatureDTO candidaturePour(Integer offreId) {
        CreateCandidatureDTO dto = new CreateCandidatureDTO();
        dto.setOffreId(offreId);
        LigneParcoursDTO ligne = new LigneParcoursDTO();
        ligne.setParcoursId(1);
        ligne.setNiveauId(3);
        dto.getParcours().add(ligne);
        return dto;
    }

    private UserDetails utilisateur(String username) {
        return userDetailsService.loadUserByUsername(username);
    }

    private Utilisateur utilisateurEntite(String username) {
        return ((UtilisateurDetails) userDetailsService.loadUserByUsername(username)).getUtilisateur();
    }
}
