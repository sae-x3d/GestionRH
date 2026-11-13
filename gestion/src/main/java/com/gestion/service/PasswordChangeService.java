package com.gestion.service;

import com.gestion.model.Utilisateur;
import com.gestion.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PasswordChangeService {
    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void changer(Integer idUtilisateur, String ancien, String nouveau) {
        Utilisateur utilisateur = utilisateurRepository.verrouillerParId(idUtilisateur)
                .orElseThrow(() -> new IllegalStateException("Compte introuvable."));
        if (ancien == null || !passwordEncoder.matches(ancien, utilisateur.getPasswordHash()))
            throw new IllegalStateException("Le mot de passe actuel est incorrect.");
        if (nouveau == null || nouveau.length() < 8 || nouveau.length() > 72
                || !nouveau.matches("^(?=.*[A-Za-z])(?=.*\\d).+$"))
            throw new IllegalStateException("Le nouveau mot de passe doit contenir 8 a 72 caracteres, une lettre et un chiffre.");
        if (passwordEncoder.matches(nouveau, utilisateur.getPasswordHash()))
            throw new IllegalStateException("Choisissez un mot de passe different de l'actuel.");
        utilisateur.setPasswordHash(passwordEncoder.encode(nouveau));
        utilisateur.setSecurityVersion(utilisateur.getSecurityVersion() + 1);
        utilisateurRepository.save(utilisateur);
    }
}
