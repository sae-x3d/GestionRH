package com.gestion.controller;

import com.gestion.exception.RessourceIntrouvableException;
import com.gestion.model.PropositionEmbauche;
import com.gestion.model.Utilisateur;
import com.gestion.service.CandidatureService;
import com.gestion.service.PropositionEmbaucheService;
import com.gestion.service.UtilisateurDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class PropositionEmbaucheController {

    private final PropositionEmbaucheService propositionService;
    private final CandidatureService candidatureService;

    @PreAuthorize("hasAnyAuthority('ROLE_ADMINISTRATEUR', 'ROLE_RH')")
    @GetMapping("/candidatures/rh/{id}/proposition")
    public String formulaireProposition(@PathVariable("id") Integer idCandidature, Model model, Authentication authentication) {
        var candidature = candidatureService.trouverParIdAvecDetails(idCandidature);
        Optional<PropositionEmbauche> proposition = propositionService.trouverParCandidature(idCandidature);
        model.addAttribute("pageTitle", "Proposition d'embauche");
        model.addAttribute("candidature", candidature);
        model.addAttribute("proposition", proposition.orElse(null));
        model.addAttribute("demandeur", utilisateurCourant(authentication));
        return "candidature/proposition-form";
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMINISTRATEUR', 'ROLE_RH')")
    @PostMapping("/candidatures/rh/{id}/proposition")
    public String genererProposition(@PathVariable("id") Integer idCandidature,
                                     @RequestParam(value = "postePropose", required = false) String postePropose,
                                     @RequestParam(value = "departementPropose", required = false) String departementPropose,
                                     @RequestParam(value = "lieuTravail", required = false) String lieuTravail,
                                     @RequestParam(value = "salairePropose", required = false) Integer salairePropose,
                                     @RequestParam(value = "typeContrat", required = false) String typeContrat,
                                     @RequestParam(value = "periodeEssaiMois", required = false) Integer periodeEssaiMois,
                                     @RequestParam(value = "dateEmbaucheSouhaitee", required = false)
                                     @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateEmbaucheSouhaitee,
                                     @RequestParam(value = "delaiReponseJours", required = false) Integer delaiReponseJours,
                                     @RequestParam(value = "avantages", required = false) String avantages,
                                     @RequestParam(value = "autresConditions", required = false) String autresConditions,
                                     @RequestParam(value = "message", required = false) String message,
                                     Authentication authentication,
                                     RedirectAttributes redirectAttributes) {
        try {
            propositionService.genererProposition(idCandidature, utilisateurCourant(authentication),
                    postePropose, departementPropose, lieuTravail, salairePropose, typeContrat,
                    periodeEssaiMois, dateEmbaucheSouhaitee, delaiReponseJours, avantages, autresConditions, message);
            redirectAttributes.addFlashAttribute("succes", "Proposition d'embauche generee avec succes.");
        } catch (RessourceIntrouvableException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("erreur", ex.getMessage());
        }
        return "redirect:/candidatures/rh/" + idCandidature + "/proposition";
    }

    @GetMapping("/candidatures/proposition")
    public String maProposition(Model model, Authentication authentication) {
        Utilisateur utilisateur = utilisateurCourant(authentication);
        Optional<PropositionEmbauche> proposition = propositionService.trouverParCandidat(utilisateur);
        model.addAttribute("pageTitle", "Ma proposition d'embauche");
        model.addAttribute("proposition", proposition.orElse(null));
        model.addAttribute("demandeur", utilisateur);
        return "candidature/ma-proposition";
    }

    @PostMapping("/candidatures/proposition/{id}/accepter")
    public String accepterProposition(@PathVariable("id") Integer idProposition,
                                      Authentication authentication,
                                      RedirectAttributes redirectAttributes) {
        try {
            propositionService.accepterProposition(idProposition, utilisateurCourant(authentication));
            redirectAttributes.addFlashAttribute("succes", "Vous avez accepte la proposition d'embauche.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("erreur", ex.getMessage());
        }
        return "redirect:/candidatures/proposition";
    }

    @PostMapping("/candidatures/proposition/{id}/refuser")
    public String refuserProposition(@PathVariable("id") Integer idProposition,
                                     @RequestParam(value = "motifRefus", required = false) String motifRefus,
                                     Authentication authentication,
                                     RedirectAttributes redirectAttributes) {
        try {
            propositionService.refuserProposition(idProposition, utilisateurCourant(authentication), motifRefus);
            redirectAttributes.addFlashAttribute("succes", "Vous avez refuse la proposition d'embauche.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("erreur", ex.getMessage());
        }
        return "redirect:/candidatures/proposition";
    }

    private Utilisateur utilisateurCourant(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof UtilisateurDetails details) {
            return details.getUtilisateur();
        }
        throw new IllegalStateException("Aucun utilisateur authentifie");
    }
}
