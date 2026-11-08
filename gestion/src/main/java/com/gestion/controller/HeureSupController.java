package com.gestion.controller;

import com.gestion.enums.StatutHeureSup;
import com.gestion.exception.AccesRefuseException;
import com.gestion.model.Employe;
import com.gestion.model.HeureSup;
import com.gestion.model.Utilisateur;
import com.gestion.service.TempsTravailService;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class HeureSupController {

    private final TempsTravailService tempsTravailService;

    @GetMapping("/heures-sup")
    public String index(@RequestParam(name = "debut", required = false)
                        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
                        @RequestParam(name = "fin", required = false)
                        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin,
                        Model model, Authentication authentication) {
        Utilisateur utilisateur = utilisateur(authentication);
        LocalDate dateDebut = debut == null ? tempsTravailService.lundi(LocalDate.now()) : debut;
        LocalDate dateFin = fin == null ? dateDebut.plusDays(6) : fin;
        model.addAttribute("pageTitle", "Heures supplementaires");
        model.addAttribute("debut", dateDebut);
        model.addAttribute("fin", dateFin);
        model.addAttribute("parametre", tempsTravailService.parametre());
        if (tempsTravailService.estRhOuAdmin(utilisateur)) {
            model.addAttribute("periodesCloturees", tempsTravailService.periodesCloturees());
            model.addAttribute("employesActifs", tempsTravailService.employesActifs());
        }
        model.addAttribute("peutValider", tempsTravailService.estRhOuAdmin(utilisateur)
                || "MANAGER".equals(role(utilisateur)));
        if (utilisateur.getEmploye() != null) {
            List<HeureSup> mesHeures = tempsTravailService.heuresSup(
                    utilisateur.getEmploye(), dateDebut, dateFin);
            model.addAttribute("mesHeures", mesHeures);
            model.addAttribute("totalHeures", tempsTravailService.totalHeuresSup(
                    utilisateur.getEmploye(), dateDebut, dateFin));
            model.addAttribute("joursTravailles", tempsTravailService.joursTravailles(
                    utilisateur.getEmploye(), dateDebut, dateFin));
        }
        if (tempsTravailService.estRhOuAdmin(utilisateur) || "MANAGER".equals(role(utilisateur))) {
            model.addAttribute("aValider", tempsTravailService.heuresSupEquipe(
                    utilisateur, StatutHeureSup.EN_ATTENTE));
        }
        return "heures-sup/index";
    }

    @PostMapping("/heures-sup/regularisation")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMINISTRATEUR','ROLE_RH')")
    public String regulariser(@RequestParam Integer idEmp, @RequestParam Integer idPeriodeSource,
                              @RequestParam Integer minutes, @RequestParam(required = false) String motif,
                              RedirectAttributes flash) {
        try { tempsTravailService.regulariserApresCloture(idEmp, idPeriodeSource, minutes, motif);
            flash.addFlashAttribute("succes", "Correction ajoutee a la periode suivante ouverte.");
        } catch (IllegalStateException e) { flash.addFlashAttribute("erreur", e.getMessage()); }
        return "redirect:/heures-sup";
    }

    @PostMapping("/heures-sup")
    public String declarer(@RequestParam("date")
                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                           @RequestParam("minutes") Integer minutes,
                           @RequestParam(name = "motif", required = false) String motif,
                           Authentication authentication, RedirectAttributes flash) {
        try {
            Employe employe = utilisateur(authentication).getEmploye();
            tempsTravailService.declarerHeureSup(employe, date, minutes, motif);
            flash.addFlashAttribute("succes", "Heures supplementaires declarees.");
        } catch (IllegalStateException ex) {
            flash.addFlashAttribute("erreur", ex.getMessage());
        }
        return "redirect:/heures-sup";
    }

    @PostMapping("/heures-sup/{id}/traiter")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER','ROLE_ADMINISTRATEUR','ROLE_RH')")
    public String traiter(@PathVariable Integer id,
                          @RequestParam("accepter") boolean accepter,
                          @RequestParam(name = "repos", defaultValue = "false") boolean repos,
                          Authentication authentication, RedirectAttributes flash) {
        try {
            tempsTravailService.traiterHeureSup(id, accepter, repos, utilisateur(authentication));
            flash.addFlashAttribute("succes", accepter
                    ? (repos ? "Heures converties en repos." : "Heures supplementaires validees.")
                    : "Heures supplementaires refusees.");
        } catch (IllegalStateException | AccesRefuseException ex) {
            flash.addFlashAttribute("erreur", ex.getMessage());
        }
        return "redirect:/heures-sup";
    }

    private String role(Utilisateur utilisateur) {
        if (utilisateur == null || utilisateur.getRole() == null
                || utilisateur.getRole().getNomRole() == null) {
            return "";
        }
        return utilisateur.getRole().getNomRole().replace("ROLE_", "").toUpperCase();
    }

    private Utilisateur utilisateur(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof UtilisateurDetails details) {
            return details.getUtilisateur();
        }
        throw new AccesRefuseException("Vous devez etre connecte.");
    }
}
