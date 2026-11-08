package com.gestion.controller;

import com.gestion.service.PasswordResetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.beans.factory.annotation.Value;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class PasswordResetController {
    private final PasswordResetService passwordResetService;

    @Value("${app.security.dev-reset-link:false}")
    private boolean afficherLienDeveloppement;

    @GetMapping("/mot-de-passe-oublie")
    public String demanderPage() { return "security/mot-de-passe-oublie"; }

    @PostMapping("/mot-de-passe-oublie")
    public String demander(@RequestParam String email, HttpServletRequest request, RedirectAttributes flash) {
        String lien = passwordResetService.demanderReinitialisation(email, request.getRemoteAddr());
        flash.addFlashAttribute("message", "Si un compte actif correspond a cette adresse, un lien de reinitialisation a ete envoye.");
        if (afficherLienDeveloppement && lien != null) flash.addFlashAttribute("lienDeveloppement", lien);
        return "redirect:/mot-de-passe-oublie";
    }

    @GetMapping("/mot-de-passe-reinitialiser")
    public String reinitialiserPage(@RequestParam(required = false) String token, Model model) {
        model.addAttribute("token", token);
        model.addAttribute("tokenValide", passwordResetService.tokenValide(token));
        return "security/mot-de-passe-reinitialiser";
    }

    @PostMapping("/mot-de-passe-reinitialiser")
    public String reinitialiser(@RequestParam String token, @RequestParam String nouveauMotDePasse,
                                @RequestParam String confirmation, RedirectAttributes flash) {
        if (!nouveauMotDePasse.equals(confirmation)) {
            flash.addFlashAttribute("erreur", "Les deux mots de passe ne correspondent pas.");
            return "redirect:/mot-de-passe-reinitialiser?token=" + java.net.URLEncoder.encode(token,
                    java.nio.charset.StandardCharsets.UTF_8);
        }
        try {
            passwordResetService.reinitialiser(token, nouveauMotDePasse);
            flash.addFlashAttribute("message", "Votre mot de passe a ete modifie. Vous pouvez vous connecter.");
            return "redirect:/connexion?reinitialise";
        } catch (IllegalStateException e) {
            flash.addFlashAttribute("erreur", e.getMessage());
            return "redirect:/mot-de-passe-reinitialiser?token=" + java.net.URLEncoder.encode(token,
                    java.nio.charset.StandardCharsets.UTF_8);
        }
    }
}
