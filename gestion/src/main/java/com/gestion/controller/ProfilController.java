package com.gestion.controller;

import com.gestion.service.PasswordChangeService;
import com.gestion.service.UtilisateurDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class ProfilController {
    private final PasswordChangeService passwordChangeService;

    @GetMapping("/profil")
    public String afficher(Model model) {
        model.addAttribute("pageTitle", "Mon profil");
        return "profil";
    }

    @PostMapping("/profil/mot-de-passe")
    public String changerMotDePasse(@RequestParam String ancienMotDePasse,
                                    @RequestParam String nouveauMotDePasse,
                                    @RequestParam String confirmation,
                                    Authentication authentication, RedirectAttributes flash) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UtilisateurDetails details))
            return "redirect:/connexion";
        if (!nouveauMotDePasse.equals(confirmation)) {
            flash.addFlashAttribute("erreurMotDePasse", "Les deux nouveaux mots de passe ne correspondent pas.");
            return "redirect:/profil";
        }
        try {
            passwordChangeService.changer(details.getUtilisateur().getIdUser(), ancienMotDePasse, nouveauMotDePasse);
            flash.addFlashAttribute("messageMotDePasse", "Mot de passe modifie. Reconnectez-vous avec votre nouveau mot de passe.");
            return "redirect:/connexion?motDePasseModifie";
        } catch (IllegalStateException e) {
            flash.addFlashAttribute("erreurMotDePasse", e.getMessage());
            return "redirect:/profil";
        }
    }
}
