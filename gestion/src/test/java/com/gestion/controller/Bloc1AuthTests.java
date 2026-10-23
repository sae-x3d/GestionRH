package com.gestion.controller;

import com.gestion.model.Demande;
import com.gestion.model.Utilisateur;
import com.gestion.repository.DemandeRepository;
import com.gestion.repository.RoleRepository;
import com.gestion.repository.UtilisateurRepository;
import com.gestion.service.DemandeStatusService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class Bloc1AuthTests {

    private static final String MOT_DE_PASSE = "MotDePasse2026";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private DemandeRepository demandeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void lesRolesAttendusExistentEnBase() {
        assertThat(roleRepository.findAll())
                .extracting(role -> role.getNomRole())
                .contains("Administrateur", "RH", "Manager", "Employe", "Finance", "Utilisateur");
    }

    @Test
    void leCompteFinanceEstRattacheAuRoleEtAuDepartementFinance() {
        Utilisateur finance = utilisateurRepository.findByUsernameWithRole("finance.claire").orElseThrow();

        assertThat(finance.getRole().getNomRole()).isEqualTo("Finance");
        assertThat(finance.getEmploye()).isNotNull();
        assertThat(finance.getEmploye().getDepartement().getNomDept()).isEqualTo("Finance");
        assertThat(passwordEncoder.matches(MOT_DE_PASSE, finance.getPasswordHash())).isTrue();
    }

    @Test
    void chaqueCompteDeTestRecoitLeBonRole() {
        assertAuthority("admin", "ROLE_ADMINISTRATEUR");
        assertAuthority("rh.sophie", "ROLE_RH");
        assertAuthority("manager.it", "ROLE_MANAGER");
        assertAuthority("paul.rabe", "ROLE_EMPLOYE");
        assertAuthority("finance.claire", "ROLE_FINANCE");
        assertAuthority("user.simple", "ROLE_UTILISATEUR");
    }

    @Test
    void lesPagesProtegeesExigentUneAuthentification() throws Exception {
        mockMvc.perform(get("/demandes/nouvelle"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/connexion"));

        mockMvc.perform(get("/offres/gestion"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/candidatures/rh"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void unManagerNePeutPasAccederAuxEcransRhNiGererLesOffres() throws Exception {
        mockMvc.perform(get("/candidatures/rh").with(user(utilisateur("manager.it"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/offres/gestion").with(user(utilisateur("manager.it"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void unCandidatNePeutPasCreerUneDemande() throws Exception {
        mockMvc.perform(post("/demandes")
                        .with(user(utilisateur("user.simple")))
                        .with(csrf())
                        .param("posteId", "1")
                        .param("departementId", "1")
                        .param("motifId", "3")
                        .param("budget", "3500000")
                        .param("dateSouhaite", "2030-11-01T09:00"))
                .andExpect(status().isForbidden());
    }

    @Test
    void leProfilFinanceVoitLesDemandesEnAttenteDeBudgetDeTousLesDepartements() throws Exception {
        mockMvc.perform(get("/demandes/validation/finance").with(user(utilisateur("finance.claire"))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Developpeur Backend")))
                .andExpect(content().string(containsString("Comptable")));
    }

    @Test
    void leManagerFinanceVoitAussiLaFileDeValidationBudget() throws Exception {
        mockMvc.perform(get("/demandes/validation/finance").with(user(utilisateur("manager.finance"))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Developpeur Backend")))
                .andExpect(content().string(containsString("Comptable")));
    }

    @Test
    void unManagerHorsFinanceNePeutPasAccederALaFileBudget() throws Exception {
        mockMvc.perform(get("/demandes/validation/finance").with(user(utilisateur("manager.it"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void laRhNePeutPasValiderLeBudget() throws Exception {
        mockMvc.perform(post("/demandes/9/validation/finance/accepter")
                        .with(user(utilisateur("rh.sophie")))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void financePeutValiderLeBudgetDUnAutreDepartement() throws Exception {
        mockMvc.perform(post("/demandes/9/validation/finance/accepter")
                        .with(user(utilisateur("finance.claire")))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/demandes/validation/finance"));

        Demande demande = demandeRepository.findById(9).orElseThrow();
        assertThat(demande.getDemandeStatus().getLibelleStatus())
                .isEqualTo(DemandeStatusService.EN_ATTENTE_VALIDATION_DIRECTION);
    }

    @Test
    void financeNePeutPasValiderLeBudgetDeSonPropreDepartement() throws Exception {
        mockMvc.perform(post("/demandes/10/validation/finance/accepter")
                        .with(user(utilisateur("finance.claire")))
                        .with(csrf()))
                .andExpect(status().isForbidden());

        Demande demande = demandeRepository.findById(10).orElseThrow();
        assertThat(demande.getDemandeStatus().getLibelleStatus())
                .isEqualTo(DemandeStatusService.EN_ATTENTE_VALIDATION_BUDGET);
    }

    @Test
    void laDirectionPeutValiderLeBudgetDuDepartementFinance() throws Exception {
        mockMvc.perform(post("/demandes/10/validation/finance/accepter")
                        .with(user(utilisateur("admin")))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/demandes/validation/finance"));

        Demande demande = demandeRepository.findById(10).orElseThrow();
        assertThat(demande.getDemandeStatus().getLibelleStatus())
                .isEqualTo(DemandeStatusService.EN_ATTENTE_VALIDATION_DIRECTION);
    }

    @Test
    void leCsrfEstObligatoirePourValiderUnBudget() throws Exception {
        mockMvc.perform(post("/demandes/9/validation/finance/accepter")
                        .with(user(utilisateur("finance.claire"))))
                .andExpect(status().isForbidden());

        Demande demande = demandeRepository.findById(9).orElseThrow();
        assertThat(demande.getDemandeStatus().getLibelleStatus())
                .isEqualTo(DemandeStatusService.EN_ATTENTE_VALIDATION_BUDGET);
    }

    @Test
    void leCandidatNaPasAccesALaValidationDuBudget() throws Exception {
        mockMvc.perform(get("/demandes/validation/finance").with(user(utilisateur("user.simple"))))
                .andExpect(status().isForbidden());
    }

    private void assertAuthority(String username, String authority) {
        UserDetails details = userDetailsService.loadUserByUsername(username);
        assertThat(details.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly(authority);
    }

    private UserDetails utilisateur(String username) {
        return userDetailsService.loadUserByUsername(username);
    }
}
