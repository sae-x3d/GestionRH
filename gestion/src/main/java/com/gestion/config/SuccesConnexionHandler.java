package com.gestion.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class SuccesConnexionHandler implements AuthenticationSuccessHandler {

    private static final String ROLE_CANDIDAT = "ROLE_UTILISATEUR";

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        response.sendRedirect(destination(authentication));
    }

    private String destination(Authentication authentication) {
        if (authentication != null && authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(ROLE_CANDIDAT::equals)) {
            return "/offres";
        }
        return "/demandes";
    }
}
