package com.gestion.controller;

import com.gestion.enums.StatutAnomalie;
import com.gestion.exception.AccesRefuseException;
import com.gestion.exception.RessourceIntrouvableException;
import com.gestion.model.Utilisateur;
import com.gestion.service.TempsTravailService;
import com.gestion.service.UtilisateurDetails;
import lombok.RequiredArgsConstructor;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

@Controller
@RequiredArgsConstructor
public class PointageController {

    private final TempsTravailService tempsTravailService;

    @GetMapping("/pointage")
    public String monPointage(@RequestParam(name = "semaine", required = false) String semaine,
                              Model model, Authentication authentication) {
        Utilisateur utilisateur = utilisateur(authentication);
        LocalDate lundi = tempsTravailService.lundi(parseDate(semaine));
        LocalDate fin = lundi.plusDays(6);
        model.addAttribute("pageTitle", "Mon pointage");
        model.addAttribute("lundi", lundi);
        model.addAttribute("fin", fin);
        model.addAttribute("semainePrecedente", lundi.minusWeeks(1));
        model.addAttribute("semaineSuivante", lundi.plusWeeks(1));
        model.addAttribute("parametre", tempsTravailService.parametre());
        if (utilisateur.getEmploye() != null) {
            model.addAttribute("pointages", tempsTravailService.pointages(
                    utilisateur.getEmploye(), lundi, fin));
            model.addAttribute("totalHeuresSup", tempsTravailService.totalHeuresSup(
                    utilisateur.getEmploye(), lundi, fin));
            model.addAttribute("anomalies", tempsTravailService.anomalies(utilisateur,
                    StatutAnomalie.OUVERTE).stream()
                    .filter(a -> a.getEmploye().getIdEmp().equals(utilisateur.getEmploye().getIdEmp()))
                    .toList());
        }
        return "pointage/index";
    }

    @PostMapping("/pointage/arrivee")
    public String arriver(Authentication authentication, RedirectAttributes flash) {
        try {
            tempsTravailService.pointerArrivee(utilisateur(authentication).getEmploye(), LocalDateTime.now());
            flash.addFlashAttribute("succes", "Arrivee enregistree.");
        } catch (IllegalStateException ex) {
            flash.addFlashAttribute("erreur", ex.getMessage());
        }
        return "redirect:/pointage";
    }

    @PostMapping("/pointage/depart")
    public String partir(Authentication authentication, RedirectAttributes flash) {
        try {
            tempsTravailService.pointerDepart(utilisateur(authentication).getEmploye(), LocalDateTime.now());
            flash.addFlashAttribute("succes", "Depart enregistre.");
        } catch (IllegalStateException ex) {
            flash.addFlashAttribute("erreur", ex.getMessage());
        }
        return "redirect:/pointage";
    }

    @GetMapping("/pointage/equipe")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER','ROLE_ADMINISTRATEUR','ROLE_RH')")
    public String equipe(@RequestParam(name = "semaine", required = false) String semaine,
                         Model model, Authentication authentication) {
        LocalDate lundi = tempsTravailService.lundi(parseDate(semaine));
        model.addAttribute("pageTitle", "Pointage de l'equipe");
        model.addAttribute("lundi", lundi);
        model.addAttribute("fin", lundi.plusDays(6));
        model.addAttribute("pointages", tempsTravailService.pointagesEquipe(
                utilisateur(authentication), lundi, lundi.plusDays(6)));
        return "pointage/equipe";
    }

    @GetMapping("/pointage/anomalies")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER','ROLE_ADMINISTRATEUR','ROLE_RH')")
    public String anomalies(@RequestParam(name = "statut", required = false) StatutAnomalie statut,
                            Model model, Authentication authentication) {
        model.addAttribute("pageTitle", "Anomalies de pointage");
        model.addAttribute("anomalies", tempsTravailService.anomalies(
                utilisateur(authentication), statut));
        model.addAttribute("statuts", StatutAnomalie.values());
        model.addAttribute("statutSelectionne", statut);
        return "pointage/anomalies";
    }

    @PostMapping("/pointage/anomalies/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER','ROLE_ADMINISTRATEUR','ROLE_RH')")
    public String traiterAnomalie(@PathVariable Integer id,
                                  @RequestParam("statut") StatutAnomalie statut,
                                  @RequestParam(name = "commentaire", required = false) String commentaire,
                                  Authentication authentication, RedirectAttributes flash) {
        try {
            tempsTravailService.traiterAnomalie(id, statut, commentaire, utilisateur(authentication));
            flash.addFlashAttribute("succes", "Anomalie mise a jour.");
        } catch (IllegalStateException | AccesRefuseException ex) {
            flash.addFlashAttribute("erreur", ex.getMessage());
        }
        return "redirect:/pointage/anomalies";
    }

    private LocalDate parseDate(String valeur) {
        if (valeur == null || valeur.isBlank()) {
            return LocalDate.now();
        }
        try {
            return LocalDate.parse(valeur);
        } catch (DateTimeParseException ex) {
            return LocalDate.now();
        }
    }

    private Utilisateur utilisateur(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof UtilisateurDetails details) {
            return details.getUtilisateur();
        }
        throw new AccesRefuseException("Vous devez etre connecte.");
    }
}
