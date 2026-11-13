package com.gestion.controller;

import com.gestion.enums.TypeAbsence;
import com.gestion.model.Employe;
import com.gestion.model.Role;
import com.gestion.model.Utilisateur;
import com.gestion.service.AbsenceService;
import com.gestion.service.UtilisateurDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AbsenceControllerTests {
    private AbsenceService service;
    private MockMvc mvc;
    private final Utilisateur user = Utilisateur.builder().idUser(3).username("employe")
            .role(Role.builder().nomRole("EMPLOYE").build()).employe(Employe.builder().idEmp(4).build()).build();

    @BeforeEach void setUp() {
        service = mock(AbsenceService.class);
        mvc = MockMvcBuilders.standaloneSetup(new AbsenceController(service)).build();
    }

    @Test void afficheLesAbsencesDuSalarieConnecte() throws Exception {
        when(service.lister(user.getEmploye(), user)).thenReturn(List.of());
        mvc.perform(get("/absences").principal(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        new UtilisateurDetails(user), "", List.of())))
                .andExpect(status().isOk()).andExpect(view().name("absences/index"))
                .andExpect(model().attributeExists("absences", "types"));
    }

    @Test void transmetLaDemandeEtSonJustificatifAuService() throws Exception {
        MockMultipartFile fichier = new MockMultipartFile("justificatif", "certificat.pdf", "application/pdf", new byte[]{1});
        mvc.perform(multipart("/absences").file(fichier).param("type", TypeAbsence.MALADIE.name())
                        .param("debut", "2026-11-09").param("fin", "2026-11-09")
                        .principal(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                                new UtilisateurDetails(user), "", List.of())))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/absences"));
        verify(service).soumettre(eq(user.getEmploye()), eq(TypeAbsence.MALADIE), any(), any(), any());
    }
}
