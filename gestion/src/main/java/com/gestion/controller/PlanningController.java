package com.gestion.controller;

import com.gestion.enums.TypeJourPlanning;
import com.gestion.exception.AccesRefuseException;
import com.gestion.model.Employe;
import com.gestion.model.Planning;
import com.gestion.model.Utilisateur;
import com.gestion.service.EmployeService;
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
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@PreAuthorize("hasAnyAuthority('ROLE_MANAGER','ROLE_ADMINISTRATEUR','ROLE_RH')")
@RequiredArgsConstructor
public class PlanningController {

    private final TempsTravailService tempsTravailService;
    private final EmployeService employeService;

    @GetMapping("/planning")
    public String index(@RequestParam(name = "semaine", required = false) String semaine,
                        Model model, Authentication authentication) {
        Utilisateur utilisateur = utilisateur(authentication);
        LocalDate lundi = tempsTravailService.lundi(parseDate(semaine));
        List<Planning> plannings = tempsTravailService.planningsEquipe(utilisateur, lundi);
        Map<Integer, List<?>> jours = new HashMap<>();
        for (Planning planning : plannings) {
            jours.put(planning.getIdPlanning(), tempsTravailService.jours(planning.getIdPlanning()));
        }
        model.addAttribute("pageTitle", "Planning de l'equipe");
        model.addAttribute("semaine", lundi);
        model.addAttribute("plannings", plannings);
        model.addAttribute("joursParPlanning", jours);
        model.addAttribute("employes", employesVisibles(utilisateur));
        model.addAttribute("typesJour", TypeJourPlanning.values());
        model.addAttribute("semainePrecedente", lundi.minusWeeks(1));
        model.addAttribute("semaineSuivante", lundi.plusWeeks(1));
        model.addAttribute("parametre", tempsTravailService.parametre());
        model.addAttribute("estRh", tempsTravailService.estRhOuAdmin(utilisateur));
        Map<String, Boolean> sousEffectif = new HashMap<>();
        for (Planning p : plannings) for (var jour : tempsTravailService.jours(p.getIdPlanning()))
            if (jour.getTypeJour() == TypeJourPlanning.TRAVAIL)
                sousEffectif.put(p.getIdPlanning() + ":" + jour.getDateJour(),
                        tempsTravailService.sousEffectif(p.getIdPlanning(), jour.getDateJour()));
        model.addAttribute("sousEffectif", sousEffectif);
        return "planning/index";
    }

    @PostMapping("/planning/employes/{idEmp}")
    public String creer(@PathVariable Integer idEmp,
                        @RequestParam("semaine") String semaine,
                        Authentication authentication, RedirectAttributes flash) {
        try {
            Employe employe = employeService.trouver(idEmp);
            tempsTravailService.creerPlanning(employe, parseDate(semaine), utilisateur(authentication));
            flash.addFlashAttribute("succes", "Planning cree.");
        } catch (IllegalStateException | AccesRefuseException ex) {
            flash.addFlashAttribute("erreur", ex.getMessage());
        }
        return "redirect:/planning?semaine=" + semaine;
    }

    @PostMapping("/planning/{id}/jour")
    public String modifierJour(@PathVariable Integer id,
                               @RequestParam("semaine") String semaine,
                               @RequestParam("date") String date,
                               @RequestParam("typeJour") TypeJourPlanning typeJour,
                               @RequestParam(name = "heureDebut", required = false) String heureDebut,
                               @RequestParam(name = "heureFin", required = false) String heureFin,
                               @RequestParam(name = "pauseMinutes", required = false) Integer pauseMinutes,
                               Authentication authentication, RedirectAttributes flash) {
        try {
            tempsTravailService.modifierJour(id, LocalDate.parse(date), typeJour,
                    parseHeure(heureDebut), parseHeure(heureFin), pauseMinutes,
                    utilisateur(authentication));
            flash.addFlashAttribute("succes", "Jour de planning mis a jour.");
        } catch (IllegalStateException | AccesRefuseException | DateTimeParseException ex) {
            flash.addFlashAttribute("erreur", ex.getMessage());
        }
        return "redirect:/planning?semaine=" + semaine;
    }

    @PostMapping("/planning/{id}/publier")
    public String publier(@PathVariable Integer id,
                          @RequestParam("semaine") String semaine,
                          Authentication authentication, RedirectAttributes flash) {
        try {
            tempsTravailService.publier(id, utilisateur(authentication));
            flash.addFlashAttribute("succes", "Planning publie et notifie au salarie.");
        } catch (IllegalStateException | AccesRefuseException ex) {
            flash.addFlashAttribute("erreur", ex.getMessage());
        }
        return "redirect:/planning?semaine=" + semaine;
    }

    private List<Employe> employesVisibles(Utilisateur utilisateur) {
        List<Employe> actifs = employeService.listerActifs();
        if (tempsTravailService.estRhOuAdmin(utilisateur)
                || utilisateur.getEmploye() == null || utilisateur.getEmploye().getDepartement() == null) {
            return actifs;
        }
        Integer idDept = utilisateur.getEmploye().getDepartement().getIdDept();
        return actifs.stream()
                .filter(e -> e.getDepartement() != null
                        && idDept.equals(e.getDepartement().getIdDept()))
                .toList();
    }

    private LocalDate parseDate(String valeur) {
        if (valeur == null || valeur.isBlank()) {
            return LocalDate.now();
        }
        return LocalDate.parse(valeur);
    }

    private LocalTime parseHeure(String valeur) {
        return valeur == null || valeur.isBlank() ? null : LocalTime.parse(valeur);
    }

    private Utilisateur utilisateur(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof UtilisateurDetails details) {
            return details.getUtilisateur();
        }
        throw new AccesRefuseException("Vous devez etre connecte.");
    }
}
