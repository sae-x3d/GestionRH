package com.gestion.service;

import com.gestion.model.Utilisateur;
import com.gestion.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UtilisateurService {

    private final UtilisateurRepository utilisateurRepository;

    @Transactional(readOnly = true)
    public Optional<Utilisateur> trouverParUsername(String username) {
        return utilisateurRepository.findByUsernameWithRole(username);
    }
}
