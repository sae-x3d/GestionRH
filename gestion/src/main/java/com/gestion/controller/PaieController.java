package com.gestion.controller;

import com.gestion.exception.AccesRefuseException;
import com.gestion.model.Utilisateur;
import com.gestion.service.EmployeService;
import com.gestion.service.InscriptionPaieService;
import com.gestion.service.UtilisateurDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@PreAuthorize("hasAnyAuthority('ROLE_ADMINISTRATEUR','ROLE_RH','ROLE_FINANCE')")
@RequiredArgsConstructor
public class PaieController {

    private final InscriptionPaieService inscriptionService;
    private final EmployeService employeService;

    @GetMapping("/paie")
    public String liste(Model model, Authentication authentication) {
        model.addAttribute("pageTitle", "Inscriptions paie");
        model.addAttribute("inscriptions", inscriptionService.lister());
        model.addAttribute("employes", employeService.listerActifs());
        model.addAttribute("demandeur", utilisateur(authentication));
        return "paie/liste";
    }

    @PostMapping("/paie/employes/{idEmp}")
    public String inscrire(@PathVariable Integer idEmp,
                           @RequestParam(name = "numeroPaie", required = false) String numeroPaie,
                           @RequestParam(name = "organisme", required = false) String organisme,
                           @RequestParam(name = "dateDebut", required = false)
                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
                           @RequestParam(name = "commentaire", required = false) String commentaire,
                           RedirectAttributes flash) {
        try {
            inscriptionService.inscrire(idEmp, numeroPaie, organisme, dateDebut, commentaire);
            flash.addFlashAttribute("succes", "Salarie inscrit a la paie.");
        } catch (IllegalStateException ex) {
            flash.addFlashAttribute("erreur", ex.getMessage());
        }
        return "redirect:/paie";
    }

    @PostMapping("/paie/{id}/exclure")
    public String exclure(@PathVariable Integer id, RedirectAttributes flash) {
        try {
            inscriptionService.exclure(id);
            flash.addFlashAttribute("succes", "Inscription paie desactivee.");
        } catch (IllegalStateException ex) {
            flash.addFlashAttribute("erreur", ex.getMessage());
        }
        return "redirect:/paie";
    }

    private Utilisateur utilisateur(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof UtilisateurDetails details) {
            return details.getUtilisateur();
        }
        throw new AccesRefuseException("Vous devez etre connecte.");
    }
}
