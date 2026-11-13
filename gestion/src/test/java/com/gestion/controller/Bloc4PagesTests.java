package com.gestion.controller;

import com.gestion.model.Accreditation;
import com.gestion.model.Candidature;
import com.gestion.model.Contrat;
import com.gestion.model.Employe;
import com.gestion.model.Onboarding;
import com.gestion.model.Utilisateur;
import com.gestion.repository.AccreditationRepository;
import com.gestion.repository.CandidatureRepository;
import com.gestion.repository.ContratRepository;
import com.gestion.repository.EmployeRepository;
import com.gestion.repository.UtilisateurRepository;
import com.gestion.service.OnboardingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class Bloc4PagesTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private AccreditationRepository accreditationRepository;

    @Autowired
    private CandidatureRepository candidatureRepository;

    @Autowired
    private EmployeRepository employeRepository;

    @Autowired
    private ContratRepository contratRepository;

    @Autowired
    private OnboardingService onboardingService;

    @Test
    void leCandidatAccedeASesAccreditationsEtASonOnboarding() throws Exception {
        mockMvc.perform(get("/mes-accreditations").with(user(utilisateur("user.simple"))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Mes accreditations")));
        mockMvc.perform(get("/mon-onboarding").with(user(utilisateur("user.simple"))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Mon integration")));
    }

    @Test
    void leFormulaireDAccreditationEstAccessible() throws Exception {
        mockMvc.perform(get("/accreditations/nouveau").with(user(utilisateur("user.simple"))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Declarer une accreditation")))
                .andExpect(content().string(containsString("Justificatif")));
    }

    @Test
    void leRhAccedeAuxEcransDeGestion() throws Exception {
        mockMvc.perform(get("/accreditations").with(user(utilisateur("rh.sophie"))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Accreditations")));
        mockMvc.perform(get("/onboarding").with(user(utilisateur("rh.sophie"))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Onboarding")));
        mockMvc.perform(get("/validations/bloc4").with(user(utilisateur("rh.sophie"))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Validations")));
    }

    @Test
    void leDetailDUneAccreditationAfficheLeWorkflow() throws Exception {
        Utilisateur candidat = utilisateurRepository.findByUsernameWithRole("user.simple").orElseThrow();
        Accreditation accreditation = accreditationRepository.save(Accreditation.builder()
                .utilisateur(candidat)
                .typeAccreditation("Certification test")
                .organisme("Organisme test")
                .dateObtention(LocalDate.of(2026, 1, 15))
                .statut("BROUILLON")
                .dateCreation(LocalDateTime.now())
                .build());

        mockMvc.perform(get("/accreditations/{id}", accreditation.getIdAccreditation())
                        .with(user(utilisateur("user.simple"))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Certification test")))
                .andExpect(content().string(containsString("Organisme test")));
    }

    @Test
    void leDetailDUnOnboardingAfficheLesTachesEtLeWorkflow() throws Exception {
        Candidature candidature = candidatureRepository.findById(1).orElseThrow();
        Employe employe = employeRepository.findById(5).orElseThrow();
        Contrat contrat = contratRepository.save(Contrat.builder()
                .candidature(candidature)
                .employe(employe)
                .numeroContrat("CTR-ONB-TEST-" + System.nanoTime())
                .typeContrat("ESSAI")
                .statut("ACCEPTE")
                .nomSalarie(employe.getNomEmp())
                .adresseSalarie("Antananarivo")
                .nomEmployeur("Entreprise Exemple")
                .adresseEmployeur("Analamanga")
                .fonction("Developpeur")
                .categorieProfessionnelle("Cadre")
                .indiceClassification("I2")
                .salaire(2500000)
                .dureeTravail("40 heures par semaine")
                .lieuTravail("Antananarivo")
                .dateEffet(LocalDate.of(2026, 11, 1))
                .dateFinEssai(LocalDate.of(2027, 2, 1))
                .dateGeneration(LocalDateTime.now())
                .build());

        Onboarding onboarding = onboardingService.demarrer(contrat, employe);

        mockMvc.perform(get("/onboarding/{id}", onboarding.getIdOnboarding())
                        .with(user(utilisateur("admin"))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Parcours d'integration")))
                .andExpect(content().string(containsString("Signature du contrat de travail")))
                .andExpect(content().string(containsString("Evaluation de fin de periode d")));

        mockMvc.perform(get("/contrats/{id}", contrat.getIdContrat())
                        .with(user(utilisateur("admin"))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Contrat de travail")))
                .andExpect(content().string(containsString("Periode d'essai")))
                .andExpect(content().string(containsString("CTR-ONB-TEST")));
    }

    private UserDetails utilisateur(String username) {
        return userDetailsService.loadUserByUsername(username);
    }
}
