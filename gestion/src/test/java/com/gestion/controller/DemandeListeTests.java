package com.gestion.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DemandeListeTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserDetailsService userDetailsService;

    @Test
    void unAdministrateurVoitToutesLesDemandes() throws Exception {
        mockMvc.perform(get("/demandes").with(user(utilisateur("admin"))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Développeur Backend")))
                .andExpect(content().string(containsString("Comptable")))
                .andExpect(content().string(containsString("Responsable Marketing")))
                .andExpect(content().string(containsString("Nouvelle demande")))
                .andExpect(content().string(containsString("href=\"/demandes/1\"")));
    }

    @Test
    void unResponsableRhVoitToutesLesDemandes() throws Exception {
        mockMvc.perform(get("/demandes").with(user(utilisateur("rh.sophie"))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Développeur Backend")))
                .andExpect(content().string(containsString("Comptable")))
                .andExpect(content().string(containsString("Responsable Marketing")));
    }

    @Test
    void unManagerVoitUniquementLesDemandesDeSonDepartement() throws Exception {
        mockMvc.perform(get("/demandes").with(user(utilisateur("manager.it"))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Développeur Backend")))
                .andExpect(content().string(not(containsString("Comptable"))))
                .andExpect(content().string(not(containsString("Responsable Marketing"))))
                .andExpect(content().string(containsString("Nouvelle demande")));
    }

    @Test
    void unManagerNeVoitQueLesDemandesDeSonPropreDepartement() throws Exception {
        mockMvc.perform(get("/demandes").with(user(utilisateur("manager.finance"))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Comptable")))
                .andExpect(content().string(not(containsString("Développeur Backend"))))
                .andExpect(content().string(not(containsString("Responsable Marketing"))));
    }

    @Test
    void unEmployeNeVoitQueSesPropresDemandes() throws Exception {
        mockMvc.perform(get("/demandes").with(user(utilisateur("paul.rabe"))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Aucune demande.")))
                .andExpect(content().string(not(containsString("Développeur Backend"))))
                .andExpect(content().string(not(containsString("Nouvelle demande"))));
    }

    @Test
    void filtreParStatut() throws Exception {
        mockMvc.perform(get("/demandes")
                        .param("statutId", "3")
                        .with(user(utilisateur("admin"))))
                .andExpect(status().isOk())
                .andExpect(model().attribute("statutId", is(3)))
                .andExpect(model().attribute("demandes", hasSize(2)))
                .andExpect(content().string(containsString("Administrateur Système")))
                .andExpect(content().string(not(containsString("Développeur Frontend"))))
                .andExpect(content().string(not(containsString("Assistant RH"))));
    }

    @Test
    void filtreParDepartement() throws Exception {
        mockMvc.perform(get("/demandes")
                        .param("departementId", "3")
                        .with(user(utilisateur("admin"))))
                .andExpect(status().isOk())
                .andExpect(model().attribute("departementId", is(3)))
                .andExpect(model().attribute("demandes", hasSize(2)))
                .andExpect(content().string(containsString("Comptable")))
                .andExpect(content().string(not(containsString("Développeur Backend"))));
    }

    @Test
    void filtreParStatutEtDepartement() throws Exception {
        mockMvc.perform(get("/demandes")
                        .param("statutId", "2")
                        .param("departementId", "1")
                        .with(user(utilisateur("admin"))))
                .andExpect(status().isOk())
                .andExpect(model().attribute("demandes", hasSize(2)))
                .andExpect(content().string(containsString("Développeur Backend")))
                .andExpect(content().string(containsString("Développeur Frontend")));
    }

    @Test
    void pagination() throws Exception {
        mockMvc.perform(get("/demandes")
                        .param("page", "1")
                        .param("size", "5")
                        .with(user(utilisateur("admin"))))
                .andExpect(status().isOk())
                .andExpect(model().attribute("currentPage", is(1)))
                .andExpect(model().attribute("totalPages", is(3)))
                .andExpect(model().attribute("size", is(5)))
                .andExpect(model().attribute("demandes", hasSize(5)));
    }

    @Test
    void leFiltreDepartementEstReserveAuxAdministrateursEtRh() throws Exception {
        mockMvc.perform(get("/demandes").with(user(utilisateur("manager.it"))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"statutId\"")))
                .andExpect(content().string(not(containsString("id=\"departementId\""))));
    }

    private UserDetails utilisateur(String username) {
        return userDetailsService.loadUserByUsername(username);
    }
}
