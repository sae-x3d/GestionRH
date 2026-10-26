package com.gestion.controller;

import com.gestion.model.Utilisateur;
import com.gestion.service.NotificationService;
import com.gestion.service.UtilisateurDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/notifications")
    public String afficher(Model model, Authentication authentication) {
        Utilisateur utilisateur = utilisateurCourant(authentication);
        model.addAttribute("pageTitle", "Notifications");
        model.addAttribute("notifications", notificationService.lister(utilisateur.getIdUser()));
        return "notifications";
    }

    @PostMapping("/notifications/{id}/lue")
    public String marquerLue(@PathVariable Integer id, Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        boolean marquee = notificationService.marquerLue(id, utilisateurCourant(authentication).getIdUser());
        if (!marquee) {
            redirectAttributes.addFlashAttribute("erreur", "Notification introuvable.");
        }
        return "redirect:/notifications";
    }

    @PostMapping("/notifications/lues")
    public String marquerToutesLues(Authentication authentication) {
        notificationService.marquerToutesLues(utilisateurCourant(authentication).getIdUser());
        return "redirect:/notifications";
    }

    private Utilisateur utilisateurCourant(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof UtilisateurDetails details) {
            return details.getUtilisateur();
        }
        throw new IllegalStateException("Aucun utilisateur authentifie");
    }
}
