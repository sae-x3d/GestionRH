package com.gestion.controller;

import com.gestion.dto.CreateOffreDTO;
import com.gestion.exception.RessourceIntrouvableException;
import com.gestion.model.OffreEmploi;
import com.gestion.model.Utilisateur;
import com.gestion.service.OffreEmploiService;
import com.gestion.service.QuestionService;
import com.gestion.service.UtilisateurDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/offres")
@RequiredArgsConstructor
public class OffreEmploiController {

    private final OffreEmploiService offreEmploiService;
    private final QuestionService questionService;

    @GetMapping
    public String filActualite(Model model, Authentication authentication) {
        model.addAttribute("pageTitle", "Offres d'emploi");
        model.addAttribute("offres", offreEmploiService.listerPubliees());
        model.addAttribute("demandeur", utilisateurCourant(authentication));
        return "offre/liste";
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMINISTRATEUR', 'ROLE_RH')")
    @GetMapping("/gestion")
    public String gestion(Model model, Authentication authentication) {
        model.addAttribute("pageTitle", "Gestion des offres");
        model.addAttribute("offres", offreEmploiService.listerToutes());
        model.addAttribute("demandesEligibles", offreEmploiService.listerDemandesEligibles());
        model.addAttribute("demandeur", utilisateurCourant(authentication));
        return "offre/gestion";
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMINISTRATEUR', 'ROLE_RH')")
    @GetMapping("/nouvelle")
    public String formulaireCreation(@RequestParam(name = "demandeId", required = false) Integer demandeId,
                                     Model model,
                                     Authentication authentication) {
        CreateOffreDTO offreForm;
        try {
            offreForm = offreEmploiService.preparerFormulaire(demandeId);
        } catch (RessourceIntrouvableException | IllegalStateException ex) {
            offreForm = new CreateOffreDTO();
            model.addAttribute("erreur", ex.getMessage());
        }
        model.addAttribute("offreForm", offreForm);
        preparerFormulaire(model, authentication);
        return "offre/formulaire";
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMINISTRATEUR', 'ROLE_RH')")
    @PostMapping
    public String creer(@Valid @ModelAttribute("offreForm") CreateOffreDTO offreForm,
                        BindingResult bindingResult,
                        Authentication authentication,
                        Model model,
                        RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            preparerFormulaire(model, authentication);
            return "offre/formulaire";
        }

        try {
            OffreEmploi offre = offreEmploiService.creer(offreForm, utilisateurCourant(authentication));
            redirectAttributes.addFlashAttribute("succes",
                    "Offre \"" + offre.getTitre() + "\" creee en brouillon.");
            return "redirect:/offres/gestion";
        } catch (RessourceIntrouvableException | IllegalStateException ex) {
            model.addAttribute("erreur", ex.getMessage());
            preparerFormulaire(model, authentication);
            return "offre/formulaire";
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Integer id, Model model, Authentication authentication) {
        OffreEmploi offre = offreEmploiService.trouverParId(id);
        if (!offreEmploiService.estPubliee(offre) && !estGestionnaire(authentication)) {
            return "redirect:/offres";
        }
        model.addAttribute("pageTitle", offre.getTitre());
        model.addAttribute("offre", offre);
        model.addAttribute("demandeur", utilisateurCourant(authentication));
        return "offre/detail";
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMINISTRATEUR', 'ROLE_RH')")
    @GetMapping("/{id}/modifier")
    public String formulaireModification(@PathVariable Integer id, Model model, Authentication authentication) {
        OffreEmploi offre = offreEmploiService.trouverParId(id);
        model.addAttribute("offre", offre);
        model.addAttribute("offreForm", offreEmploiService.versDto(offre));
        preparerFormulaire(model, authentication);
        return "offre/formulaire";
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMINISTRATEUR', 'ROLE_RH')")
    @PostMapping("/{id}")
    public String modifier(@PathVariable Integer id,
                           @Valid @ModelAttribute("offreForm") CreateOffreDTO offreForm,
                           BindingResult bindingResult,
                           Authentication authentication,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("offre", offreEmploiService.trouverParId(id));
            preparerFormulaire(model, authentication);
            return "offre/formulaire";
        }

        try {
            OffreEmploi offre = offreEmploiService.modifier(id, offreForm);
            redirectAttributes.addFlashAttribute("succes",
                    "Offre \"" + offre.getTitre() + "\" mise a jour.");
            return "redirect:/offres/gestion";
        } catch (RessourceIntrouvableException | IllegalStateException ex) {
            model.addAttribute("erreur", ex.getMessage());
            model.addAttribute("offre", offreEmploiService.trouverParId(id));
            preparerFormulaire(model, authentication);
            return "offre/formulaire";
        }
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMINISTRATEUR', 'ROLE_RH')")
    @GetMapping("/{id}/questions")
    public String questionnaire(@PathVariable Integer id, Model model, Authentication authentication) {
        model.addAttribute("pageTitle", "Questionnaire de l'offre");
        model.addAttribute("offre", offreEmploiService.trouverParId(id));
        model.addAttribute("questions", questionService.listerParOffre(id));
        model.addAttribute("demandeur", utilisateurCourant(authentication));
        return "offre/questions";
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMINISTRATEUR', 'ROLE_RH')")
    @PostMapping("/{id}/questions")
    public String ajouterQuestion(@PathVariable Integer id,
                                  @RequestParam("libelle") String libelle,
                                  @RequestParam(name = "ordre", required = false) Integer ordre,
                                  RedirectAttributes redirectAttributes) {
        try {
            questionService.ajouter(id, libelle, ordre);
            redirectAttributes.addFlashAttribute("succes", "Question ajoutee au questionnaire.");
        } catch (RessourceIntrouvableException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("erreur", ex.getMessage());
        }
        return "redirect:/offres/" + id + "/questions";
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMINISTRATEUR', 'ROLE_RH')")
    @PostMapping("/{id}/questions/{idQuestion}/supprimer")
    public String supprimerQuestion(@PathVariable Integer id, @PathVariable Integer idQuestion,
                                    RedirectAttributes redirectAttributes) {
        try {
            questionService.supprimer(idQuestion);
            redirectAttributes.addFlashAttribute("succes", "Question supprimee.");
        } catch (RessourceIntrouvableException ex) {
            redirectAttributes.addFlashAttribute("erreur", ex.getMessage());
        }
        return "redirect:/offres/" + id + "/questions";
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMINISTRATEUR', 'ROLE_RH')")
    @PostMapping("/{id}/publier")
    public String publier(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            OffreEmploi offre = offreEmploiService.publier(id);
            redirectAttributes.addFlashAttribute("succes",
                    "Offre \"" + offre.getTitre() + "\" publiee dans le fil d'actualite.");
        } catch (RessourceIntrouvableException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("erreur", ex.getMessage());
        }
        return "redirect:/offres/gestion";
    }

    private void preparerFormulaire(Model model, Authentication authentication) {
        model.addAttribute("pageTitle", model.containsAttribute("offre")
                ? "Modifier l'offre d'emploi"
                : "Nouvelle offre d'emploi");
        model.addAttribute("demandesEligibles", offreEmploiService.listerDemandesEligibles());
        model.addAttribute("demandeur", utilisateurCourant(authentication));
    }

    private boolean estGestionnaire(Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if ("ROLE_ADMINISTRATEUR".equals(authority.getAuthority())
                    || "ROLE_RH".equals(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    private Utilisateur utilisateurCourant(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof UtilisateurDetails details) {
            return details.getUtilisateur();
        }
        return null;
    }
}
