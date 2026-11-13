package com.gestion.service;

import com.gestion.model.PasswordResetToken;
import com.gestion.model.Utilisateur;
import com.gestion.repository.PasswordResetTokenRepository;
import com.gestion.repository.UtilisateurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTests {
    @Mock private UtilisateurRepository utilisateurRepository;
    @Mock private PasswordResetTokenRepository tokenRepository;
    @Mock private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    @Mock private EmailService emailService;
    private PasswordResetService service;

    @BeforeEach void setUp() {
        service = new PasswordResetService(utilisateurRepository, tokenRepository, passwordEncoder, emailService,
                new PasswordResetRateLimiter());
        ReflectionTestUtils.setField(service, "applicationUrl", "https://rh.example.org");
    }

    @Test void envoieUnLienOpaqueEtAccepteUnTokenUneSeuleFois() {
        Utilisateur utilisateur = Utilisateur.builder().idUser(7).username("rh.user")
                .email("rh@example.org").actif(true).build();
        when(utilisateurRepository.findFirstByEmailIgnoreCaseAndActifTrue("RH@example.org"))
                .thenReturn(Optional.of(utilisateur));
        when(tokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        service.demanderReinitialisation(" RH@example.org ");

        ArgumentCaptor<String> corps = ArgumentCaptor.forClass(String.class);
        verify(emailService).envoyer(eq(utilisateur), anyString(), corps.capture(), eq(true));
        Matcher matcher = Pattern.compile("token=([A-Za-z0-9_-]+)").matcher(corps.getValue());
        assertTrue(matcher.find());
        String tokenClair = matcher.group(1);
        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(tokenCaptor.capture());
        PasswordResetToken saved = tokenCaptor.getValue();
        assertNotEquals(tokenClair, saved.getTokenHash());
        assertTrue(saved.getDateExpiration().isAfter(LocalDateTime.now().plusMinutes(29)));
        when(tokenRepository.findByTokenHashAndDateUtilisationIsNull(anyString())).thenReturn(Optional.of(saved));
        when(passwordEncoder.encode("Nouveau123")).thenReturn("hash-bcrypt");

        assertTrue(service.tokenValide(tokenClair));
        service.reinitialiser(tokenClair, "Nouveau123");

        assertEquals("hash-bcrypt", utilisateur.getPasswordHash());
        assertEquals(1, utilisateur.getSecurityVersion());
        assertNotNull(saved.getDateUtilisation());
        verify(utilisateurRepository).save(utilisateur);
        assertThrows(IllegalStateException.class, () -> service.reinitialiser(tokenClair, "Nouveau123"));
    }

    @Test void nEnvoiePasDeMailSiAdresseInconnueEtRefuseUnMotDePasseFaible() {
        when(utilisateurRepository.findFirstByEmailIgnoreCaseAndActifTrue("inconnu@example.org"))
                .thenReturn(Optional.empty());
        service.demanderReinitialisation("inconnu@example.org");
        verifyNoInteractions(emailService);

        assertThrows(IllegalStateException.class, () -> service.reinitialiser("token", "faible"));
        verifyNoInteractions(tokenRepository);
    }

    @Test void limiteLesDemandesRepeteesParEmailEtIP() {
        PasswordResetRateLimiter limiteur = new PasswordResetRateLimiter();
        for (int i = 0; i < 3; i++) assertTrue(limiteur.autoriser("email:personne@example.org", 3));
        assertFalse(limiteur.autoriser("email:personne@example.org", 3));
    }
}
