package com.gestion.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class HomeControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserDetailsService userDetailsService;

    @Test
    void laPageDAccueilEstPubliqueEtAfficheLaVitrine() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("accueil"))
                .andExpect(model().attributeExists("offres"))
                .andExpect(content().string(containsString("Rejoignez notre equipe")))
                .andExpect(content().string(containsString("Nos offres a la une")));
    }

    @Test
    void leFilDesOffresEstPublic() throws Exception {
        mockMvc.perform(get("/offres"))
                .andExpect(status().isOk())
                .andExpect(view().name("offre/liste"));
    }

    @Test
    void leDetailDUneOffrePublieeEstPublic() throws Exception {
        mockMvc.perform(get("/offres/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("offre/detail"));
    }

    @Test
    void laGestionDesOffresResteProtegee() throws Exception {
        mockMvc.perform(get("/offres/gestion"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void laPageDAccueilAfficheLeCtaSeConnecterPourUnVisiteur() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("estAuthentifie", false))
                .andExpect(content().string(containsString("Se connecter")));
    }

    @Test
    void laPageDAccueilAdapteLeCtaPourUnUtilisateurConnecte() throws Exception {
        mockMvc.perform(get("/").with(user(userDetailsService.loadUserByUsername("user.simple"))))
                .andExpect(status().isOk())
                .andExpect(model().attribute("estAuthentifie", true))
                .andExpect(content().string(containsString("Mon espace")))
                .andExpect(content().string(containsString("Mes candidatures")))
                .andExpect(content().string(not(containsString("Postuler maintenant"))));
    }
}
