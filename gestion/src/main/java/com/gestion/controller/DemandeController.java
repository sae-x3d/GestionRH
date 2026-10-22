package com.gestion.controller;

import com.gestion.dto.CreateDemandeDTO;
import com.gestion.exception.RessourceIntrouvableException;
import com.gestion.model.Demande;
import com.gestion.model.Utilisateur;
import com.gestion.service.DemandeService;
import com.gestion.service.DemandeStatusService;
import com.gestion.service.DepartementService;
import com.gestion.service.EmployeService;
import com.gestion.service.MotifService;
import com.gestion.service.PosteService;
import com.gestion.service.UtilisateurDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/demandes")
@RequiredArgsConstructor
public class DemandeController {

    private final DemandeService demandeService;
    private final PosteService posteService;
    private final DepartementService departementService;
    private final MotifService motifService;
    private final EmployeService employeService;
    private final DemandeStatusService demandeStatusService;

    @GetMapping("/nouvelle")
    public String formulaireCreation(Model model, Authentication authentication) {
        verifierCreationDemande(authentication);
        model.addAttribute("demandeForm", new CreateDemandeDTO());
        preparerFormulaire(model, authentication);
        return "demande/formulaire";
    }

    @PostMapping
    public String creer(@Valid @ModelAttribute("demandeForm") CreateDemandeDTO demandeForm,
                        BindingResult bindingResult,
                        Authentication authentication,
                        Model model,
                        RedirectAttributes redirectAttributes) {
        verifierCreationDemande(authentication);
        if (bindingResult.hasErrors()) {
            preparerFormulaire(model, authentication);
            return "demande/formulaire";
        }

        Demande demande;
        try {
            demande = demandeService.creer(demandeForm, utilisateurCourant(authentication));
        } catch (RessourceIntrouvableException ex) {
            model.addAttribute("erreur", ex.getMessage());
            preparerFormulaire(model, authentication);
            return "demande/formulaire";
        }

        redirectAttributes.addFlashAttribute("succes",
                "Demande n " + demande.getIdDemande() + " enregistree avec le statut \""
                        + demandeStatusService.trouverStatutInitial().map(statut -> statut.getLibelleStatus()).orElse("")
                        + "\".");
        return "redirect:/demandes/nouvelle";
    }

    private void preparerFormulaire(Model model, Authentication authentication) {
        model.addAttribute("pageTitle", "Nouvelle demande de recrutement");
        model.addAttribute("postes", posteService.listerTous());
        model.addAttribute("departements", departementService.listerTous());
        model.addAttribute("motifs", motifService.listerTous());
        model.addAttribute("employes", employeService.listerActifs());
        model.addAttribute("statutInitial", demandeStatusService.trouverStatutInitial().orElse(null));
        model.addAttribute("demandeur", utilisateurCourant(authentication));
    }

    private Utilisateur utilisateurCourant(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof UtilisateurDetails details) {
            return details.getUtilisateur();
        }
        throw new IllegalStateException("Aucun utilisateur authentifie");
    }

    private void verifierCreationDemande(Authentication authentication) {
        if (authentication == null) {
            throw new AccessDeniedException("Utilisateur non authentifie.");
        }
        boolean autorise = authentication.getAuthorities().stream()
                .anyMatch(authority -> {
                    String a = authority.getAuthority();
                    return "ROLE_ADMINISTRATEUR".equals(a) || "ROLE_MANAGER".equals(a);
                });
        if (!autorise) {
            throw new AccessDeniedException("Seuls les managers et l'administration peuvent creer une demande de recrutement.");
        }
    }
}
