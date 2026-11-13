package com.gestion.config;

import com.gestion.repository.UtilisateurRepository;
import com.gestion.service.UtilisateurDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class SecurityVersionFilter extends OncePerRequestFilter {
    static final String SESSION_VERSION = SecurityVersionFilter.class.getName() + ".version";
    private final UtilisateurRepository utilisateurRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof UtilisateurDetails details) {
            Integer versionActuelle = utilisateurRepository.findSecurityVersionById(
                    details.getUtilisateur().getIdUser()).orElse(null);
            Object versionSession = request.getSession(false) == null ? null
                    : request.getSession(false).getAttribute(SESSION_VERSION);
            if (versionActuelle == null || (versionSession != null && !versionSession.equals(versionActuelle))) {
                if (request.getSession(false) != null) request.getSession(false).invalidate();
                SecurityContextHolder.clearContext();
                response.sendRedirect(request.getContextPath() + "/connexion?sessionExpiree");
                return;
            }
            if (versionSession == null) request.getSession(true).setAttribute(SESSION_VERSION, versionActuelle);
        }
        chain.doFilter(request, response);
    }
}
