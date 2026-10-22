package com.gestion.service;

import com.gestion.model.Utilisateur;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.text.Normalizer;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

public class UtilisateurDetails extends User {

    private final Utilisateur utilisateur;

    public UtilisateurDetails(Utilisateur utilisateur) {
        super(utilisateur.getUsername(), utilisateur.getPasswordHash(), authorities(utilisateur));
        this.utilisateur = utilisateur;
    }

    public Utilisateur getUtilisateur() {
        return utilisateur;
    }

    private static Collection<GrantedAuthority> authorities(Utilisateur utilisateur) {
        String nomRole = utilisateur.getRole() == null ? "Employe" : utilisateur.getRole().getNomRole();
        return List.of(new SimpleGrantedAuthority("ROLE_" + normaliser(nomRole)));
    }

    private static String normaliser(String nomRole) {
        return Normalizer.normalize(nomRole, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT);
    }
}
