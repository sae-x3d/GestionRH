package com.gestion.controller;

import com.gestion.model.Demande;
import com.gestion.repository.DemandeRepository;
import com.gestion.service.DemandeStatusService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DemandeControllerTests {

    private static final String MOT_DE_PASSE = "MotDePasse2026";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DemandeRepository demandeRepository;

    @Autowired
    private UserDetailsService userDetailsService;

    @Test
    void sansAuthentification_leFormulaireRedirigeVersLaConnexion() throws Exception {
        mockMvc.perform(get("/demandes/nouvelle"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/connexion"));
    }

    @Test
    void connexionAvecUnIdentifiantValide_redirigeVersLaListe() throws Exception {
        mockMvc.perform(post("/connexion")
                        .with(csrf())
                        .param("username", "manager.it")
                        .param("password", MOT_DE_PASSE))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/demandes"));
    }

    @Test
    void connexionAvecUnIdentifiantInvalide_estRefusee() throws Exception {
        mockMvc.perform(post("/connexion")
                        .with(csrf())
                        .param("username", "manager.it")
                        .param("password", "mauvais-mot-de-passe"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/connexion?error"));
    }

    @Test
    void formulaireAuthentifie_chargeLesReferentielsDepuisLaBase() throws Exception {
        mockMvc.perform(get("/demandes/nouvelle").with(user(utilisateur("manager.it"))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("manager.it")))
                .andExpect(content().string(containsString("Developpeur Backend")))
                .andExpect(content().string(containsString("Informatique")))
                .andExpect(content().string(containsString("Creation de poste")))
                .andExpect(content().string(containsString("En attente validation RH")))
                .andExpect(content().string(containsString("Ressources Humaines")))
                .andExpect(content().string(containsString("name=\"_csrf\"")));
    }

    @Test
    void soumissionInvalide_afficheLesErreursSansCreerDeDemande() throws Exception {
        long nombreDemandesAvant = demandeRepository.count();

        mockMvc.perform(post("/demandes")
                        .with(user(utilisateur("manager.it")))
                        .with(csrf())
                        .param("posteId", "")
                        .param("departementId", "1")
                        .param("motifId", "1")
                        .param("budget", "-1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Le poste est obligatoire")))
                .andExpect(content().string(containsString("Le budget ne peut pas etre negatif")));

        assertThat(demandeRepository.count()).isEqualTo(nombreDemandesAvant);
    }

    @Test
    void soumissionValide_creeLaDemandeAvecLeDemandeurEtLeStatutInitial() throws Exception {
        long nombreDemandesAvant = demandeRepository.count();

        MvcResult resultat = mockMvc.perform(post("/demandes")
                        .with(user(utilisateur("manager.it")))
                        .with(csrf())
                        .param("posteId", "1")
                        .param("departementId", "1")
                        .param("motifId", "3")
                        .param("budget", "3500000")
                        .param("dateSouhaite", "2030-11-01T09:00"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        assertThat(demandeRepository.count()).isEqualTo(nombreDemandesAvant + 1);

        Demande demande = demandeRepository.findAll().stream()
                .max(Comparator.comparing(Demande::getIdDemande))
                .orElseThrow();
        assertThat(resultat.getResponse().getRedirectedUrl())
                .isEqualTo("/demandes/" + demande.getIdDemande());
        assertThat(demande.getDemandeur().getIdUser()).isEqualTo(3);
        assertThat(demande.getDemandeStatus().getLibelleStatus()).isEqualTo("EN_ATTENTE_VALIDATION_RH");
        assertThat(demande.getPoste().getIdPoste()).isEqualTo(1);
        assertThat(demande.getDepartement().getIdDept()).isEqualTo(1);
        assertThat(demande.getMotif().getIdMotif()).isEqualTo(3);
        assertThat(demande.getBudget()).isEqualTo(3500000);
        assertThat(demande.getDateCreation()).isNotNull();
    }

    @Test
    void soumissionAvecUneReferenceInexistante_afficheUneErreurEtNeCreePasDeDemande() throws Exception {
        long nombreDemandesAvant = demandeRepository.count();

        mockMvc.perform(post("/demandes")
                        .with(user(utilisateur("manager.it")))
                        .with(csrf())
                        .param("posteId", "9999")
                        .param("departementId", "1")
                        .param("motifId", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Référence introuvable : poste")));

        assertThat(demandeRepository.count()).isEqualTo(nombreDemandesAvant);
    }

    @Test
    void unAdministrateurPeutOuvrirLeFormulaire() throws Exception {
        mockMvc.perform(get("/demandes/nouvelle").with(user(utilisateur("admin"))))
                .andExpect(status().isOk());
    }

    @Test
    void unAdministrateurPeutCreerUneDemandePourNImporteQuelDepartement() throws Exception {
        long nombreDemandesAvant = demandeRepository.count();

        MvcResult resultat = mockMvc.perform(post("/demandes")
                        .with(user(utilisateur("admin")))
                        .with(csrf())
                        .param("posteId", "1")
                        .param("departementId", "3")
                        .param("motifId", "1")
                        .param("budget", "2500000"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        assertThat(demandeRepository.count()).isEqualTo(nombreDemandesAvant + 1);

        Demande demande = demandeRepository.findAll().stream()
                .max(Comparator.comparing(Demande::getIdDemande))
                .orElseThrow();
        assertThat(resultat.getResponse().getRedirectedUrl())
                .isEqualTo("/demandes/" + demande.getIdDemande());
        assertThat(demande.getDemandeur().getIdUser()).isEqualTo(1);
        assertThat(demande.getDepartement().getIdDept()).isEqualTo(3);
    }

    @Test
    void unEmployeNePeutPasOuvrirLeFormulaire() throws Exception {
        mockMvc.perform(get("/demandes/nouvelle").with(user(utilisateur("paul.rabe"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void unEmployeNePeutPasSoumettreUneDemande() throws Exception {
        long nombreDemandesAvant = demandeRepository.count();

        mockMvc.perform(post("/demandes")
                        .with(user(utilisateur("paul.rabe")))
                        .with(csrf())
                        .param("posteId", "1")
                        .param("departementId", "1")
                        .param("motifId", "1"))
                .andExpect(status().isForbidden());

        assertThat(demandeRepository.count()).isEqualTo(nombreDemandesAvant);
    }

    @Test
    void unManagerNePeutPasCreerUneDemandeHorsDeSonDepartement() throws Exception {
        long nombreDemandesAvant = demandeRepository.count();

        mockMvc.perform(post("/demandes")
                        .with(user(utilisateur("manager.it")))
                        .with(csrf())
                        .param("posteId", "1")
                        .param("departementId", "3")
                        .param("motifId", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(
                        "Vous ne pouvez créer une demande que pour votre département.")));

        assertThat(demandeRepository.count()).isEqualTo(nombreDemandesAvant);
    }

    @Test
    void leFormulairePreselectionneLeDepartementDuManager() throws Exception {
        mockMvc.perform(get("/demandes/nouvelle").with(user(utilisateur("manager.it"))))
                .andExpect(status().isOk())
                .andExpect(model().attribute("demandeForm", hasProperty("departementId", is(1))));
    }

    @Test
    void apresConnexionLeFormulaireEstAccessibleAvecLaSession() throws Exception {
        MvcResult resultatConnexion = mockMvc.perform(post("/connexion")
                        .with(csrf())
                        .param("username", "manager.it")
                        .param("password", MOT_DE_PASSE))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        MockHttpSession session = (MockHttpSession) resultatConnexion.getRequest().getSession(false);

        mockMvc.perform(get("/demandes/nouvelle").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("manager.it")))
                .andExpect(content().string(containsString("En attente validation RH")));
    }

    private UserDetails utilisateur(String username) {
        return userDetailsService.loadUserByUsername(username);
    }
}
