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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class Bloc6PagesTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserDetailsService userDetailsService;

    @Test
    void lesPagesDeTempsDeTravailSontRendues() throws Exception {
        mockMvc.perform(get("/planning").with(user(utilisateur("admin"))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Planning de l'equipe")));

        mockMvc.perform(get("/pointage").with(user(utilisateur("admin"))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Mon pointage")));

        mockMvc.perform(get("/pointage/equipe").with(user(utilisateur("admin"))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Pointage de l'equipe")));

        mockMvc.perform(get("/pointage/anomalies").with(user(utilisateur("admin"))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Anomalies de pointage")));

        mockMvc.perform(get("/heures-sup").with(user(utilisateur("admin"))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Heures supplementaires")));

        mockMvc.perform(get("/paie/cloture").with(user(utilisateur("admin"))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Cloture de paie")));
    }

    @Test
    void leReportingExposeLesNouveauxIndicateurs() throws Exception {
        mockMvc.perform(get("/reporting").with(user(utilisateur("admin"))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Absenteisme par departement")))
                .andExpect(content().string(containsString("Heures supplementaires par departement")));
    }

    @Test
    void lesPagesSensiblesSontProtegees() throws Exception {
        mockMvc.perform(get("/planning").with(user(utilisateur("user.simple"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/pointage/anomalies").with(user(utilisateur("user.simple"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/paie/cloture").with(user(utilisateur("user.simple"))))
                .andExpect(status().isForbidden());
    }

    private UserDetails utilisateur(String username) {
        return userDetailsService.loadUserByUsername(username);
    }
}
