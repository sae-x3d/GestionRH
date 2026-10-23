package com.gestion.controller;

import com.gestion.service.OffreEmploiService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private static final int APERCU_OFFRES = 3;

    private final OffreEmploiService offreEmploiService;

    @GetMapping("/")
    public String accueil(Model model, Authentication authentication) {
        model.addAttribute("pageTitle", "Accueil");
        model.addAttribute("offres", offreEmploiService.listerPubliees().stream()
                .limit(APERCU_OFFRES)
                .toList());
        model.addAttribute("estAuthentifie", estAuthentifie(authentication));
        return "accueil";
    }

    private boolean estAuthentifie(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getName());
    }
}
