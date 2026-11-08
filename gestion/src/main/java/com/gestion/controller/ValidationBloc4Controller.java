package com.gestion.controller;

import com.gestion.enums.TypeObjetValidation;
import com.gestion.exception.AccesRefuseException;
import com.gestion.exception.RessourceIntrouvableException;
import com.gestion.model.Utilisateur;
import com.gestion.model.ValidationDossier;
import com.gestion.service.UtilisateurDetails;
import com.gestion.service.ValidationWorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/validations/bloc4")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class ValidationBloc4Controller {

    private final ValidationWorkflowService workflowService;

    @GetMapping
    public String liste(@RequestParam(name = "type", required = false) TypeObjetValidation type,
                        Model model, Authentication authentication) {
        Utilisateur utilisateur = utilisateur(authentication);
        List<ValidationDossier> dossiers = workflowService.dossiersPour(utilisateur).stream()
                .filter(dossier -> type == null || dossier.getTypeObjet() == type)
                .toList();
        model.addAttribute("pageTitle", "Validations a traiter");
        model.addAttribute("dossiers", dossiers);
        model.addAttribute("types", TypeObjetValidation.values());
        model.addAttribute("type", type);
        model.addAttribute("demandeur", utilisateur);
        return "validation/bloc4-liste";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Integer id, Model model, Authentication authentication) {
        ValidationDossier dossier = workflowService.charger(id);
        model.addAttribute("pageTitle", "Dossier de validation");
        model.addAttribute("dossier", dossier);
        model.addAttribute("roleAttendu", workflowService.roleAttendu(dossier));
        model.addAttribute("peutDecider", workflowService.peutDecider(dossier, utilisateur(authentication)));
        model.addAttribute("demandeur", utilisateur(authentication));
        return "validation/bloc4-detail";
    }

    @PostMapping("/{id}/approuver")
    public String approuver(@PathVariable Integer id,
                            @RequestParam(name = "commentaire", required = false) String commentaire,
                            Authentication authentication,
                            RedirectAttributes flash) {
        return decider(id, true, commentaire, authentication, flash);
    }

    @PostMapping("/{id}/refuser")
    public String refuser(@PathVariable Integer id,
                          @RequestParam(name = "commentaire", required = false) String commentaire,
                          Authentication authentication,
                          RedirectAttributes flash) {
        return decider(id, false, commentaire, authentication, flash);
    }

    private String decider(Integer id, boolean approuve, String commentaire,
                           Authentication authentication, RedirectAttributes flash) {
        try {
            if (approuve) {
                workflowService.approuver(id, utilisateur(authentication), commentaire);
                flash.addFlashAttribute("succes", "Etape de validation approuvee.");
            } else {
                workflowService.refuser(id, utilisateur(authentication), commentaire);
                flash.addFlashAttribute("succes", "Dossier renvoye pour correction.");
            }
        } catch (AccesRefuseException | RessourceIntrouvableException | IllegalStateException ex) {
            flash.addFlashAttribute("erreur", ex.getMessage());
        }
        return "redirect:/validations/bloc4";
    }

    private Utilisateur utilisateur(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof UtilisateurDetails details) {
            return details.getUtilisateur();
        }
        throw new AccesRefuseException("Vous devez etre connecte.");
    }
}
