package com.gestion.config;

import com.gestion.service.AuditService;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuditInterceptorTests {
    @AfterEach void nettoyerSecurite() { SecurityContextHolder.clearContext(); }

    @Test void journaliseLesValeursAvantApresEtMasqueLesSecrets() throws Exception {
        AuditService audit = mock(AuditService.class);
        when(audit.instantanePourChemin("/absences/42/decision"))
                .thenReturn(Map.of("statut", "SOUMISE"))
                .thenReturn(Map.of("statut", "VALIDEE_RH"));
        AuditInterceptor interceptor = new AuditInterceptor(audit);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/absences/42/decision");
        request.setParameter("accepter", "true");
        request.setParameter("password", "secret");
        request.setParameter("token", "secret-token");
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "rh.test", "", List.of(new SimpleGrantedAuthority("ROLE_RH"))));

        interceptor.preHandle(request, response, new Object());
        interceptor.afterCompletion(request, response, new Object(), null);

        var details = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(audit).enregistrer(eq("rh.test"), eq("POST"), eq("/absences/42/decision"),
                eq("absences"), eq("42"), details.capture(), eq("127.0.0.1"));
        assertTrue(details.getValue().contains("avant={statut=SOUMISE}"));
        assertTrue(details.getValue().contains("apres={statut=VALIDEE_RH}"));
        assertTrue(details.getValue().contains("accepter=[true]"));
        assertTrue(!details.getValue().contains("secret"));
    }
}
