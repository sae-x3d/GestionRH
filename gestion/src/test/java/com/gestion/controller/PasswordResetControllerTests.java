package com.gestion.controller;

import com.gestion.service.PasswordResetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PasswordResetControllerTests {
    private PasswordResetService service;
    private MockMvc mvc;

    @BeforeEach void setUp() {
        service = mock(PasswordResetService.class);
        PasswordResetController controller = new PasswordResetController(service);
        ReflectionTestUtils.setField(controller, "afficherLienDeveloppement", true);
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test void afficheLeLienDeTestSeulementDansLaConfigurationDev() throws Exception {
        when(service.demanderReinitialisation("rh@example.org", "127.0.0.1"))
                .thenReturn("http://localhost:8080/mot-de-passe-reinitialiser?token=abc");
        mvc.perform(post("/mot-de-passe-oublie").param("email", "rh@example.org"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("lienDeveloppement",
                        "http://localhost:8080/mot-de-passe-reinitialiser?token=abc"));
    }

    @Test void neRevelePasSiLeTokenEstInvalide() throws Exception {
        when(service.tokenValide("invalide")).thenReturn(false);
        mvc.perform(get("/mot-de-passe-reinitialiser").param("token", "invalide"))
                .andExpect(status().isOk()).andExpect(model().attribute("tokenValide", false));
    }
}
