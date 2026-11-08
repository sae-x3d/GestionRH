package com.gestion.service;

import com.gestion.model.PasswordResetToken;
import com.gestion.repository.PasswordResetTokenRepository;
import com.gestion.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class PasswordResetService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32;
    private static final int VALIDITE_MINUTES = 30;

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final PasswordResetRateLimiter rateLimiter;

    @Value("${app.url:http://localhost:8080}")
    private String applicationUrl;

    @Value("${app.security.dev-reset-link:false}")
    private boolean exposerLienDeveloppement;

    @Transactional
    public String demanderReinitialisation(String email) {
        return demanderReinitialisation(email, "unknown");
    }

    @Transactional
    public String demanderReinitialisation(String email, String adresseIp) {
        if (email == null || email.isBlank()) return null;
        String emailNormalise = email.trim().toLowerCase(java.util.Locale.ROOT);
        if (!rateLimiter.autoriser("ip:" + adresseIp, 10)
                || !rateLimiter.autoriser("email:" + emailNormalise, 3)) return null;
        return utilisateurRepository.findFirstByEmailIgnoreCaseAndActifTrue(email.trim()).map(utilisateur -> {
            if (utilisateur.getEmail() == null || utilisateur.getEmail().isBlank()) return null;
            tokenRepository.deleteByUtilisateurIdUser(utilisateur.getIdUser());
            byte[] octets = new byte[TOKEN_BYTES];
            RANDOM.nextBytes(octets);
            String token = Base64.getUrlEncoder().withoutPadding().encodeToString(octets);
            LocalDateTime maintenant = LocalDateTime.now();
            tokenRepository.save(PasswordResetToken.builder().utilisateur(utilisateur).tokenHash(hash(token))
                    .dateCreation(maintenant).dateExpiration(maintenant.plusMinutes(VALIDITE_MINUTES)).build());
            String base = applicationUrl == null ? "http://localhost:8080" : applicationUrl.replaceAll("/$", "");
            String lien = base + "/mot-de-passe-reinitialiser?token="
                    + java.net.URLEncoder.encode(token, StandardCharsets.UTF_8);
            emailService.envoyer(utilisateur, "GestionRH - Reinitialisation du mot de passe",
                    "Utilisez ce lien dans les 30 minutes pour choisir un nouveau mot de passe :\n" + lien,
                    true);
            return exposerLienDeveloppement ? lien : null;
        }).orElse(null);
    }

    @Transactional(readOnly = true)
    public boolean tokenValide(String token) {
        if (token == null || token.isBlank()) return false;
        return tokenRepository.findByTokenHashAndDateUtilisationIsNull(hash(token))
                .filter(t -> t.getDateExpiration().isAfter(LocalDateTime.now())).isPresent();
    }

    @Transactional
    public void reinitialiser(String token, String nouveauMotDePasse) {
        if (!motDePasseValide(nouveauMotDePasse))
            throw new IllegalStateException("Le mot de passe doit contenir entre 8 et 72 caracteres, une lettre et un chiffre.");
        PasswordResetToken reset = token == null ? null : tokenRepository
                .findByTokenHashAndDateUtilisationIsNull(hash(token)).orElse(null);
        if (reset == null || reset.getDateUtilisation() != null
                || !reset.getDateExpiration().isAfter(LocalDateTime.now()))
            throw new IllegalStateException("Ce lien de reinitialisation est invalide ou expire.");
        reset.getUtilisateur().setPasswordHash(passwordEncoder.encode(nouveauMotDePasse));
        reset.getUtilisateur().setSecurityVersion(reset.getUtilisateur().getSecurityVersion() + 1);
        reset.setDateUtilisation(LocalDateTime.now());
        utilisateurRepository.save(reset.getUtilisateur());
        tokenRepository.save(reset);
    }

    private boolean motDePasseValide(String valeur) {
        return valeur != null && valeur.length() >= 8 && valeur.length() <= 72
                && valeur.matches("^(?=.*[A-Za-z])(?=.*\\d).+$");
    }

    private String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponible.", e);
        }
    }
}
