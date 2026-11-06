package com.gestion.service;

import com.gestion.dto.InscriptionDTO;
import com.gestion.model.Utilisateur;
import com.gestion.repository.UtilisateurRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class InscriptionServiceTests {

    @Autowired
    private InscriptionService inscriptionService;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void creeUnCompteAvecLeRoleUtilisateurEtUnMotDePasseChiffre() {
        Utilisateur utilisateur = inscriptionService.inscrire(dto("service.candidat", "Service@Example.com"));

        assertThat(utilisateur.getRole().getNomRole()).isEqualTo("Utilisateur");
        assertThat(utilisateur.getEmail()).isEqualTo("service@example.com");
        assertThat(utilisateur.getPasswordHash()).isNotEqualTo("MotDePasse2026");
        assertThat(passwordEncoder.matches("MotDePasse2026", utilisateur.getPasswordHash())).isTrue();
    }

    @Test
    void refuseUnNomDUtilisateurDejaUtiliseSansTenirCompteDeLaCasse() {
        InscriptionDTO dto = dto("User.Simple", "cas.insensible@example.com");

        assertThatThrownBy(() -> inscriptionService.inscrire(dto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("nom d'utilisateur");
    }

    @Test
    void refuseUnEmailDejaUtiliseSansTenirCompteDeLaCasse() {
        inscriptionService.inscrire(dto("premier.service", "same@example.com"));

        assertThatThrownBy(() -> inscriptionService.inscrire(dto("second.service", "SAME@example.com")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("email");
    }

    private InscriptionDTO dto(String username, String email) {
        InscriptionDTO dto = new InscriptionDTO();
        dto.setPrenom("Service");
        dto.setNom("Candidat");
        dto.setEmail(email);
        dto.setTelephone("0340000000");
        dto.setUsername(username);
        dto.setPassword("MotDePasse2026");
        dto.setConfirmation("MotDePasse2026");
        return dto;
    }
}
