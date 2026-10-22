package com.gestion.controller;

import com.gestion.model.Utilisateur;
import com.gestion.service.UtilisateurDetails;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAttributes {

    @ModelAttribute("demandeur")
    public Utilisateur demandeur(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof UtilisateurDetails details) {
            return details.getUtilisateur();
        }
        return null;
    }

    @ModelAttribute("estRh")
    public boolean estRh(Authentication authentication) {
        return aRole(authentication, "ROLE_RH");
    }

    @ModelAttribute("estAdministrateur")
    public boolean estAdministrateur(Authentication authentication) {
        return aRole(authentication, "ROLE_ADMINISTRATEUR");
    }

    @ModelAttribute("estManager")
    public boolean estManager(Authentication authentication) {
        return aRole(authentication, "ROLE_MANAGER");
    }

    @ModelAttribute("estEmploye")
    public boolean estEmploye(Authentication authentication) {
        return aRole(authentication, "ROLE_EMPLOYE");
    }

    @ModelAttribute("estFinance")
    public boolean estFinance(Authentication authentication) {
        if (!(authentication != null && authentication.getPrincipal() instanceof UtilisateurDetails details)) {
            return false;
        }
        Utilisateur utilisateur = details.getUtilisateur();
        return utilisateur.getEmploye() != null
                && utilisateur.getEmploye().getDepartement() != null
                && "Finance".equalsIgnoreCase(utilisateur.getEmploye().getDepartement().getNomDept());
    }

    private boolean aRole(Authentication authentication, String role) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> role.equals(authority.getAuthority()));
    }
}
