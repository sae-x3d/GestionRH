package com.gestion.service;

import com.gestion.dto.InscriptionDTO;
import com.gestion.model.Role;
import com.gestion.model.Utilisateur;
import com.gestion.repository.RoleRepository;
import com.gestion.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class InscriptionService {

    private static final String ROLE_UTILISATEUR = "Utilisateur";

    private final UtilisateurRepository utilisateurRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Utilisateur inscrire(InscriptionDTO dto) {
        String username = dto.getUsername().trim();
        String email = dto.getEmail().trim().toLowerCase(Locale.ROOT);

        if (utilisateurRepository.existsByUsernameIgnoreCase(username)) {
            throw new IllegalStateException("Ce nom d'utilisateur est deja utilise.");
        }
        if (utilisateurRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalStateException("Cette adresse email est deja utilisee.");
        }

        Role role = roleRepository.findByNomRoleIgnoreCase(ROLE_UTILISATEUR)
                .orElseThrow(() -> new IllegalStateException(
                        "Le role \"" + ROLE_UTILISATEUR + "\" est absent de la base."));

        Utilisateur utilisateur = Utilisateur.builder()
                .username(username)
                .email(email)
                .passwordHash(passwordEncoder.encode(dto.getPassword()))
                .role(role)
                .build();

        try {
            return utilisateurRepository.save(utilisateur);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalStateException("Ce nom d'utilisateur ou cette adresse email est deja utilise.");
        }
    }
}
