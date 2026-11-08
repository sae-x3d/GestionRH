package com.gestion.config;

import com.gestion.model.Utilisateur;
import com.gestion.repository.UtilisateurRepository;
import com.gestion.service.UtilisateurDetails;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class SecurityVersionFilterTests {
    private final UtilisateurRepository repository = mock(UtilisateurRepository.class);

    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @Test void invalideLaSessionQuandLaVersionDuMotDePasseAChange() throws Exception {
        SecurityVersionFilter filter = new SecurityVersionFilter(repository);
        Utilisateur user = Utilisateur.builder().idUser(3).username("u").passwordHash("hash").build();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession().setAttribute(SecurityVersionFilter.SESSION_VERSION, 0);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new UtilisateurDetails(user), "", List.of()));
        when(repository.findSecurityVersionById(3)).thenReturn(Optional.of(1));

        filter.doFilter(request, response, chain);

        assertEquals("/connexion?sessionExpiree", response.getRedirectedUrl());
        verify(chain, never()).doFilter(any(), any());
    }
}
