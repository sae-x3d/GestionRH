package com.gestion.controller;

import com.gestion.dto.MenuSection;
import com.gestion.model.Utilisateur;
import com.gestion.service.NavigationService;
import com.gestion.service.NotificationService;
import com.gestion.service.UtilisateurDetails;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAttributes {

    private final NavigationService navigationService;
    private final NotificationService notificationService;

    @ModelAttribute("demandeur")
    public Utilisateur demandeur(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof UtilisateurDetails details) {
            return details.getUtilisateur();
        }
        return null;
    }

    @ModelAttribute("nombreNotifications")
    public long nombreNotifications(Authentication authentication) {
        Utilisateur utilisateur = demandeur(authentication);
        return utilisateur == null ? 0 : notificationService.compterNonLues(utilisateur.getIdUser());
    }

    @ModelAttribute("menu")
    public List<MenuSection> menu(Authentication authentication, HttpServletRequest request) {
        return navigationService.construire(demandeur(authentication), uri(request));
    }

    @ModelAttribute("notificationsActif")
    public boolean notificationsActif(HttpServletRequest request) {
        return "/notifications".equals(uri(request));
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
        if (aRole(authentication, "ROLE_FINANCE")) {
            return true;
        }
        if (!(authentication != null && authentication.getPrincipal() instanceof UtilisateurDetails details)) {
            return false;
        }
        Utilisateur utilisateur = details.getUtilisateur();
        return utilisateur.getEmploye() != null
                && utilisateur.getEmploye().getDepartement() != null
                && "Finance".equalsIgnoreCase(utilisateur.getEmploye().getDepartement().getNomDept());
    }

    private String uri(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contexte = request.getContextPath();
        if (contexte != null && !contexte.isEmpty() && uri.startsWith(contexte)) {
            uri = uri.substring(contexte.length());
        }
        return uri;
    }

    private boolean aRole(Authentication authentication, String role) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> role.equals(authority.getAuthority()));
    }
}
