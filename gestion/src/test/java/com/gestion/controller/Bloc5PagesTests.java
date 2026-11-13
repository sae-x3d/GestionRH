package com.gestion.controller;

import com.gestion.model.Employe;
import com.gestion.repository.EmployeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class Bloc5PagesTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private EmployeRepository employeRepository;

    @Test
    void lesPagesDeGestionSontRendues() throws Exception {
        mockMvc.perform(get("/employes").with(user(utilisateur("admin"))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Salaries")));

        mockMvc.perform(get("/sorties").with(user(utilisateur("admin"))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Gestion des sorties")));

        mockMvc.perform(get("/paie").with(user(utilisateur("admin"))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Inscriptions paie")));

        mockMvc.perform(get("/reporting").with(user(utilisateur("admin"))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Reporting RH")));

        mockMvc.perform(get("/audit").with(user(utilisateur("admin"))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Journal d'audit")));
    }

    @Test
    void laGedSalarieEstRendue() throws Exception {
        Employe employe = employeRepository.findAll().stream().findFirst()
                .orElseGet(() -> employeRepository.save(Employe.builder()
                        .nomEmp("Test GED")
                        .dateEmbauche(LocalDateTime.now())
                        .build()));

        mockMvc.perform(get("/employes/{id}/documents", employe.getIdEmp())
                        .with(user(utilisateur("admin"))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ajouter un document")));
    }

    @Test
    void lesNouvellesRoutesSontProtegees() throws Exception {
        mockMvc.perform(get("/audit").with(user(utilisateur("rh.sophie"))))
                .andExpect(status().isForbidden());
    }

    private UserDetails utilisateur(String username) {
        return userDetailsService.loadUserByUsername(username);
    }
}
