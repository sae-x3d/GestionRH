package com.gestion.config;

import com.gestion.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class AuditInterceptor implements HandlerInterceptor {

    private static final String AVANT = AuditInterceptor.class.getName() + ".avant";

    private final AuditService auditService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            request.setAttribute(AVANT, auditService.instantanePourChemin(request.getRequestURI()));
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        String chemin = request.getRequestURI();
        if (chemin.startsWith("/css") || chemin.startsWith("/js")
                || chemin.equals("/connexion") || chemin.startsWith("/error")
                || chemin.startsWith("/favicon")) {
            return;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            return;
        }
        String[] segments = chemin.split("/");
        String entite = segments.length > 1 && !segments[1].isBlank() ? segments[1] : "accueil";
        String idEntite = null;
        for (int i = segments.length - 1; i >= 0; i--) {
            if (segments[i].matches("\\d+")) {
                idEntite = segments[i];
                break;
            }
        }
        StringBuilder details = new StringBuilder();
        Object avant = request.getAttribute(AVANT);
        if (avant instanceof Map<?, ?> map && !map.isEmpty()) {
            Map<String, String> apres = auditService.instantanePourChemin(chemin);
            details.append("avant=").append(map).append("; apres=").append(apres).append(';');
        }
        request.getParameterMap().forEach((key, values) -> {
            String k = key.toLowerCase(java.util.Locale.ROOT);
            if (k.contains("password") || k.contains("token") || k.contains("csrf")
                    || k.contains("motdepasse") || k.contains("confirmation")
                    || k.contains("file") || k.contains("fichier") || k.contains("justificatif")) return;
            details.append(key).append('=').append(java.util.Arrays.toString(values)).append(';');
        });
        details.append("status=").append(response.getStatus());
        auditService.enregistrer(authentication.getName(), request.getMethod(), chemin,
                entite, idEntite, details.toString(), adresseIp(request));
    }

    private String adresseIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
