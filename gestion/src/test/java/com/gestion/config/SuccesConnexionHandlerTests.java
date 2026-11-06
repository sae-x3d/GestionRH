package com.gestion.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SuccesConnexionHandlerTests {

    @Autowired
    private SuccesConnexionHandler handler;

    @Autowired
    private UserDetailsService userDetailsService;

    @Test
    void unCandidatEstRedirigeVersLesOffres() throws Exception {
        assertThat(redirectionPour("user.simple")).isEqualTo("/offres");
    }

    @Test
    void unCandidatQuiVientDUneOffreEstRedirigeVersLeFormulaireDePostulation() throws Exception {
        assertThat(redirectionPour("user.simple", "7"))
                .isEqualTo("/candidatures/postuler?offreId=7");
    }

    @Test
    void unOffreIdInvalideNeDetournePasLaRedirection() throws Exception {
        assertThat(redirectionPour("user.simple", "https://example.com"))
                .isEqualTo("/offres");
    }

    @Test
    void lePersonnelEstRedirigeVersLesDemandes() throws Exception {
        assertThat(redirectionPour("rh.sophie")).isEqualTo("/demandes");
        assertThat(redirectionPour("manager.it")).isEqualTo("/demandes");
        assertThat(redirectionPour("admin")).isEqualTo("/demandes");
    }

    private String redirectionPour(String username) throws Exception {
        return redirectionPour(username, null);
    }

    private String redirectionPour(String username, String offreId) throws Exception {
        UserDetails details = userDetailsService.loadUserByUsername(username);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                details, details.getPassword(), details.getAuthorities());
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (offreId != null) {
            request.setParameter("offreId", offreId);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, authentication);

        return response.getRedirectedUrl();
    }
}
