package com.gestion.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import com.gestion.service.UtilisateurDetails;
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
        if (authentication != null && authentication.getPrincipal() instanceof UtilisateurDetails details) {
            request.getSession(true).setAttribute(SecurityVersionFilter.SESSION_VERSION,
                    details.getUtilisateur().getSecurityVersion());
        }
        response.sendRedirect(destination(request, authentication));
    }

    private String destination(HttpServletRequest request, Authentication authentication) {
        if (authentication != null && authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(ROLE_CANDIDAT::equals)) {
            String offreId = request.getParameter("offreId");
            if (offreId != null && offreId.matches("\\d+")) {
                return "/candidatures/postuler?offreId=" + offreId;
            }
            return "/offres";
        }
        return "/demandes";
    }
}
