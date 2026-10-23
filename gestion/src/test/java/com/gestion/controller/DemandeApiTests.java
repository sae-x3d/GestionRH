package com.gestion.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DemandeApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserDetailsService userDetailsService;

    @Test
    void unManagerPeutCreerUneDemandeViaApi() throws Exception {
        mockMvc.perform(post("/api/demandes")
                        .with(user(utilisateur("manager.it")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "posteId": 1,
                                  "departementId": 1,
                                  "motifId": 1,
                                  "budget": 2500,
                                  "dateSouhaite": "2050-01-01T10:00:00"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idDemande").exists())
                .andExpect(jsonPath("$.demandeur").value("manager.it"))
                .andExpect(jsonPath("$.statut").value("EN_ATTENTE_VALIDATION_RH"))
                .andExpect(jsonPath("$.departement").value("Informatique"));
    }

    @Test
    void unAdministrateurPeutCreerUneDemandePourNImporteQuelDepartement() throws Exception {
        mockMvc.perform(post("/api/demandes")
                        .with(user(utilisateur("admin")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "posteId": 3,
                                  "departementId": 3,
                                  "motifId": 2
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.departement").value("Finance"));
    }

    @Test
    void laCreationViaApiEstRefuseeAUnUtilisateurSimple() throws Exception {
        mockMvc.perform(post("/api/demandes")
                        .with(user(utilisateur("user.simple")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "posteId": 1,
                                  "departementId": 1,
                                  "motifId": 1
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void unManagerNePeutPasCreerUneDemandeHorsDeSonDepartement() throws Exception {
        mockMvc.perform(post("/api/demandes")
                        .with(user(utilisateur("manager.it")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "posteId": 7,
                                  "departementId": 3,
                                  "motifId": 1
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void laCreationAvecDesDonneesInvalidesRetourne400() throws Exception {
        mockMvc.perform(post("/api/demandes")
                        .with(user(utilisateur("manager.it")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "budget": -100,
                                  "dateSouhaite": "2000-01-01T10:00:00"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.posteId").exists())
                .andExpect(jsonPath("$.fieldErrors.departementId").exists())
                .andExpect(jsonPath("$.fieldErrors.motifId").exists())
                .andExpect(jsonPath("$.fieldErrors.budget").exists())
                .andExpect(jsonPath("$.fieldErrors.dateSouhaite").exists());
    }

    @Test
    void unAdministrateurListeToutesLesDemandes() throws Exception {
        mockMvc.perform(get("/api/demandes").with(user(utilisateur("admin"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(12)));
    }

    @Test
    void unManagerNeListeQueLesDemandesDeSonDepartement() throws Exception {
        mockMvc.perform(get("/api/demandes").with(user(utilisateur("manager.it"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(6)));
    }

    @Test
    void unUtilisateurNeListeQueSesPropresDemandes() throws Exception {
        mockMvc.perform(get("/api/demandes").with(user(utilisateur("user.simple"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void leDetailEstAccessibleAuDemandeur() throws Exception {
        mockMvc.perform(get("/api/demandes/1").with(user(utilisateur("manager.it"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idDemande").value(1))
                .andExpect(jsonPath("$.demandeur").value("manager.it"));
    }

    @Test
    void leDetailEstRefusePourUneDemandeDUnAutreDepartement() throws Exception {
        mockMvc.perform(get("/api/demandes/1").with(user(utilisateur("manager.finance"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void leDetailDuneDemandeInexistanteRetourne404() throws Exception {
        mockMvc.perform(get("/api/demandes/9999").with(user(utilisateur("admin"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    private org.springframework.security.core.userdetails.UserDetails utilisateur(String username) {
        return userDetailsService.loadUserByUsername(username);
    }
}
