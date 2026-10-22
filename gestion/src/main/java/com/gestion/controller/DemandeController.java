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
import com.gestion.service.PaginationUtils;
import com.gestion.service.PosteService;
import com.gestion.service.UtilisateurDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
<<<<<<< HEAD
import org.springframework.data.domain.Page;
=======
import org.springframework.security.access.AccessDeniedException;
>>>>>>> origin/Joh
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
<<<<<<< HEAD
        CreateDemandeDTO demandeForm = new CreateDemandeDTO();
        demandeForm.setDepartementId(demandeService.idDepartementUtilisateur(utilisateurCourant(authentication)));
        model.addAttribute("demandeForm", demandeForm);
=======
        verifierCreationDemande(authentication);
        model.addAttribute("demandeForm", new CreateDemandeDTO());
>>>>>>> origin/Joh
        preparerFormulaire(model, authentication);
        return "demande/formulaire";
    }

    @PostMapping
    public String creer(@Valid @ModelAttribute("demandeForm") CreateDemandeDTO demandeForm,
                        BindingResult bindingResult,
                        Authentication authentication,
                        Model model,
                        RedirectAttributes redirectAttributes) {
<<<<<<< HEAD
        Utilisateur utilisateur = utilisateurCourant(authentication);
        if (demandeForm.getDepartementId() != null
                && !demandeService.departementAutorise(demandeForm.getDepartementId(), utilisateur)) {
            bindingResult.rejectValue("departementId", "departement.nonAutorise",
                    "Vous ne pouvez créer une demande que pour votre département.");
        }

=======
        verifierCreationDemande(authentication);
>>>>>>> origin/Joh
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

        redirectAttributes.addFlashAttribute("succes",
                "Demande n " + demande.getIdDemande() + " enregistree avec le statut \""
                        + demandeStatusService.trouverStatutInitial().map(statut -> statut.getLibelleStatus()).orElse("")
<<<<<<< HEAD
                        + " ».");
        return "redirect:/demandes/" + demande.getIdDemande();
=======
                        + "\".");
        return "redirect:/demandes/nouvelle";
>>>>>>> origin/Joh
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
