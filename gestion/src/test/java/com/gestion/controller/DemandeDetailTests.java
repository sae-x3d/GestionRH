package com.gestion.controller;

import com.gestion.dto.CreateDemandeDTO;
import com.gestion.model.Demande;
import com.gestion.model.Utilisateur;
import com.gestion.service.DemandeService;
import com.gestion.service.UtilisateurDetails;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DemandeDetailTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private DemandeService demandeService;

    @Test
    void unAdministrateurPeutConsulterUneDemande() throws Exception {
        mockMvc.perform(get("/demandes/8").with(user(utilisateur("admin"))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Demande n° 8")))
                .andExpect(content().string(containsString("Responsable Marketing")))
                .andExpect(content().string(containsString("manager.marketing")))
                .andExpect(content().string(containsString("Refusée")));
    }

    @Test
    void unResponsableRhPeutConsulterUneDemande() throws Exception {
        mockMvc.perform(get("/demandes/3").with(user(utilisateur("rh.sophie"))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Demande n° 3")))
                .andExpect(content().string(containsString("Comptable")));
    }

    @Test
    void unManagerConsulteUneDemandeDeSonDepartement() throws Exception {
        mockMvc.perform(get("/demandes/1").with(user(utilisateur("manager.it"))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Demande n° 1")))
                .andExpect(content().string(containsString("Développeur Backend")))
                .andExpect(content().string(containsString("manager.it")))
                .andExpect(content().string(containsString("Création de poste")));
    }

    @Test
    void unManagerNePeutPasConsulterUneDemandeDAutreDepartement() throws Exception {
        mockMvc.perform(get("/demandes/3").with(user(utilisateur("manager.it"))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/demandes"))
                .andExpect(flash().attribute("erreur", containsString("introuvable")));
    }

    @Test
    void unEmployeNePeutPasConsulterUneDemandeQuiNEstPasLaSienne() throws Exception {
        mockMvc.perform(get("/demandes/1").with(user(utilisateur("paul.rabe"))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/demandes"))
                .andExpect(flash().attribute("erreur", containsString("introuvable")));
    }

    @Test
    void unEmployePeutConsulterSaPropreDemande() throws Exception {
        Demande demande = demandeService.creer(nouvelleDemande(), utilisateurEntite("paul.rabe"));

        mockMvc.perform(get("/demandes/" + demande.getIdDemande())
                        .with(user(utilisateur("paul.rabe"))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Demande n° " + demande.getIdDemande())))
                .andExpect(content().string(containsString("paul.rabe")))
                .andExpect(content().string(containsString("Développeur Backend")));
    }

    @Test
    void uneDemandeInexistanteRedirigeAvecUnMessageDErreur() throws Exception {
        mockMvc.perform(get("/demandes/9999").with(user(utilisateur("admin"))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/demandes"))
                .andExpect(flash().attribute("erreur", containsString("Demande introuvable")));
    }

    private CreateDemandeDTO nouvelleDemande() {
        CreateDemandeDTO dto = new CreateDemandeDTO();
        dto.setPosteId(1);
        dto.setDepartementId(1);
        dto.setMotifId(1);
        dto.setBudget(1000000);
        return dto;
    }

    private Utilisateur utilisateurEntite(String username) {
        return ((UtilisateurDetails) userDetailsService.loadUserByUsername(username)).getUtilisateur();
    }

    private UserDetails utilisateur(String username) {
        return userDetailsService.loadUserByUsername(username);
    }
}
