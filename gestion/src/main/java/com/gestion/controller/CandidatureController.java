package com.gestion.controller;

import com.gestion.dto.CreateCandidatureDTO;
import com.gestion.dto.LigneParcoursDTO;
import com.gestion.exception.RessourceIntrouvableException;
import com.gestion.model.Candidature;
import com.gestion.model.Utilisateur;
import com.gestion.service.CandidatureService;
import com.gestion.service.UtilisateurDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/candidatures")
@RequiredArgsConstructor
public class CandidatureController {

    private final CandidatureService candidatureService;

    @GetMapping
    public String mesCandidatures(Model model, Authentication authentication) {
        Utilisateur utilisateur = utilisateurCourant(authentication);
        model.addAttribute("pageTitle", "Mes candidatures");
        model.addAttribute("candidatures", candidatureService.listerParUtilisateur(utilisateur.getIdUser()));
        model.addAttribute("demandeur", utilisateur);
        return "candidature/liste";
    }

    @GetMapping("/postuler")
    public String formulairePostulation(@RequestParam(name = "offreId", required = false) Integer offreId,
                                        Model model,
                                        Authentication authentication) {
        CreateCandidatureDTO candidatureForm = new CreateCandidatureDTO();
        candidatureForm.setOffreId(offreId);
        candidatureForm.getParcours().add(new LigneParcoursDTO());
        model.addAttribute("candidatureForm", candidatureForm);
        preparerFormulaire(model, authentication);
        return "candidature/formulaire";
    }

    @PostMapping
    public String postuler(@Valid @ModelAttribute("candidatureForm") CreateCandidatureDTO candidatureForm,
                           BindingResult bindingResult,
                           @RequestParam(name = "cv", required = false) MultipartFile cv,
                           Authentication authentication,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        Utilisateur utilisateur = utilisateurCourant(authentication);

        if (bindingResult.hasErrors()) {
            preparerFormulaire(model, authentication);
            return "candidature/formulaire";
        }

        try {
            Candidature candidature = candidatureService.postuler(candidatureForm, cv, utilisateur);
            redirectAttributes.addFlashAttribute("succes",
                    "Candidature enregistree pour l'offre \"" + candidature.getOffre().getTitre()
                            + "\" avec le statut \"" + candidature.getStatut().getLibelle() + "\".");
            return "redirect:/candidatures";
        } catch (RessourceIntrouvableException | IllegalStateException ex) {
            model.addAttribute("erreur", ex.getMessage());
            preparerFormulaire(model, authentication);
            return "candidature/formulaire";
        }
    }

    private void preparerFormulaire(Model model, Authentication authentication) {
        model.addAttribute("pageTitle", "Postuler a une offre");
        model.addAttribute("offresOuvertes", candidatureService.listerOffresOuvertes());
        model.addAttribute("parcoursRef", candidatureService.listerParcours());
        model.addAttribute("niveauxRef", candidatureService.listerNiveaux());
        model.addAttribute("demandeur", utilisateurCourant(authentication));
    }

    private Utilisateur utilisateurCourant(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof UtilisateurDetails details) {
            return details.getUtilisateur();
        }
        throw new IllegalStateException("Aucun utilisateur authentifie");
    }
}
