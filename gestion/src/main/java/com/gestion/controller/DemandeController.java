package com.gestion.controller;

import com.gestion.dto.CreateDemandeDTO;
import com.gestion.exception.DecisionRhImpossibleException;
import com.gestion.exception.RessourceIntrouvableException;
import com.gestion.model.Demande;
import com.gestion.model.Utilisateur;
import com.gestion.service.DemandeService;
import com.gestion.service.DemandeStatusService;
import com.gestion.service.DepartementService;
import com.gestion.service.EmployeService;
import com.gestion.service.MotifService;
import com.gestion.service.PaginationUtils;
import com.gestion.service.PosteService;
import com.gestion.service.UtilisateurDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    @GetMapping
    public String lister(@RequestParam(defaultValue = "0") int page,
                         @RequestParam(defaultValue = "10") int size,
                         @RequestParam(required = false) Integer statutId,
                         @RequestParam(required = false) Integer departementId,
                         Model model,
                         Authentication authentication) {
        Utilisateur utilisateur = utilisateurCourant(authentication);
        List<Demande> demandes = demandeService.listerPour(utilisateur);

        if (statutId != null) {
            demandes = demandes.stream()
                    .filter(demande -> demande.getDemandeStatus() != null
                            && statutId.equals(demande.getDemandeStatus().getIdDemandeStatus()))
                    .toList();
        }
        if (departementId != null) {
            demandes = demandes.stream()
                    .filter(demande -> demande.getDepartement() != null
                            && departementId.equals(demande.getDepartement().getIdDept()))
                    .toList();
        }

        Page<Demande> pageDemandes = PaginationUtils.paginerListe(demandes, page, size);

        Map<String, String> filtres = new LinkedHashMap<>();
        if (statutId != null) {
            filtres.put("statutId", statutId.toString());
        }
        if (departementId != null) {
            filtres.put("departementId", departementId.toString());
        }

        model.addAttribute("demandes", pageDemandes.getContent());
        model.addAttribute("currentPage", pageDemandes.getNumber());
        model.addAttribute("totalPages", pageDemandes.getTotalPages());
        model.addAttribute("size", pageDemandes.getSize());
        model.addAttribute("filtres", filtres);
        model.addAttribute("statutId", statutId);
        model.addAttribute("departementId", departementId);
        model.addAttribute("statuts", demandeStatusService.listerTous());
        model.addAttribute("departements", demandeService.peutFiltrerParDepartement(utilisateur)
                ? departementService.listerTous()
                : List.of());
        model.addAttribute("pageTitle", "Liste des demandes");
        return "demande/liste";
    }

    @PreAuthorize("hasAuthority('ROLE_RH')")
    @GetMapping("/validation-rh")
    public String validationRh(Model model) {
        model.addAttribute("demandes", demandeService.listerEnAttenteValidationRh());
        model.addAttribute("pageTitle", "Validation RH");
        model.addAttribute("titre", "Demandes en attente de validation RH");
        model.addAttribute("etape", "rh");
        return "demande/validation";
    }

    @PreAuthorize("hasAuthority('ROLE_RH')")
    @PostMapping("/{id}/validation-rh/accepter")
    public String accepterRh(@PathVariable Integer id,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        return traiterDecisionRh(id, true, authentication, redirectAttributes);
    }

    @PreAuthorize("hasAuthority('ROLE_RH')")
    @PostMapping("/{id}/validation-rh/refuser")
    public String refuserRh(@PathVariable Integer id,
                            Authentication authentication,
                            RedirectAttributes redirectAttributes) {
        return traiterDecisionRh(id, false, authentication, redirectAttributes);
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Integer id,
                         Model model,
                         Authentication authentication,
                         RedirectAttributes redirectAttributes) {
        try {
            Demande demande = demandeService.trouverPourUtilisateur(id, utilisateurCourant(authentication));
            model.addAttribute("demande", demande);
            model.addAttribute("pageTitle", "Demande n° " + demande.getIdDemande());
            return "demande/detail";
        } catch (RessourceIntrouvableException ex) {
            redirectAttributes.addFlashAttribute("erreur", ex.getMessage());
            return "redirect:/demandes";
        }
    }

    @GetMapping("/nouvelle")
    public String formulaireCreation(Model model, Authentication authentication) {
        verifierCreationDemande(authentication);
        CreateDemandeDTO demandeForm = new CreateDemandeDTO();
        demandeForm.setDepartementId(demandeService.idDepartementUtilisateur(utilisateurCourant(authentication)));
        model.addAttribute("demandeForm", demandeForm);
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
        Utilisateur utilisateur = utilisateurCourant(authentication);
        if (demandeForm.getDepartementId() != null
                && !demandeService.departementAutorise(demandeForm.getDepartementId(), utilisateur)) {
            bindingResult.rejectValue("departementId", "departement.nonAutorise",
                    "Vous ne pouvez créer une demande que pour votre département.");
        }

        if (bindingResult.hasErrors()) {
            preparerFormulaire(model, authentication);
            return "demande/formulaire";
        }

        Demande demande;
        try {
            demande = demandeService.creer(demandeForm, utilisateur);
        } catch (RessourceIntrouvableException ex) {
            model.addAttribute("erreur", ex.getMessage());
            preparerFormulaire(model, authentication);
            return "demande/formulaire";
        }

        String libelleStatut = demandeStatusService.trouverStatutInitial()
                .map(statut -> demandeStatusService.libelleAffichage(statut.getLibelleStatus()))
                .orElse("");
        redirectAttributes.addFlashAttribute("succes",
                "Demande n° " + demande.getIdDemande() + " enregistrée avec le statut « "
                        + demandeStatusService.trouverStatutInitial()
                                .map(statut -> demandeStatusService.libelleAffichage(statut.getLibelleStatus()))
                                .orElse("")
                        + " ».");
        return "redirect:/demandes/" + demande.getIdDemande();
    }

    private String traiterDecisionRh(Integer id, boolean accepter, Authentication authentication,
                                     RedirectAttributes redirectAttributes) {
        try {
            Demande demande = demandeService.deciderRh(id, utilisateurCourant(authentication), accepter);
            String message = accepter
                    ? "Demande n° " + demande.getIdDemande()
                            + " acceptée : en attente de validation budgétaire."
                    : "Demande n° " + demande.getIdDemande() + " refusée.";
            redirectAttributes.addFlashAttribute("succes", message);
        } catch (RessourceIntrouvableException | DecisionRhImpossibleException ex) {
            redirectAttributes.addFlashAttribute("erreur", ex.getMessage());
        }
        return "redirect:/demandes/validation-rh";
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
                    return "ROLE_ADMINISTRATEUR".equals(a) || "ROLE_MANAGER".equals(a) || "ROLE_RH".equals(a);
                });
        if (!autorise) {
            throw new AccessDeniedException("Seuls les managers, la RH et l'administration peuvent creer une demande de recrutement.");
        }
    }
}
