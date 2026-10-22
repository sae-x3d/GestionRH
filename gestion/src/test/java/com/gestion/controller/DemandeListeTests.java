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
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
                .andExpect(content().string(containsString("Nouvelle demande")));
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

    private UserDetails utilisateur(String username) {
        return userDetailsService.loadUserByUsername(username);
    }
}
