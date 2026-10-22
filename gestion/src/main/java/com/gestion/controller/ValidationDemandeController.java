package com.gestion.controller;

import com.gestion.model.Demande;
import com.gestion.model.Utilisateur;
import com.gestion.service.DemandeService;
import com.gestion.service.UtilisateurDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/demandes/validation")
@RequiredArgsConstructor
public class ValidationDemandeController {

    private final DemandeService demandeService;

    @GetMapping("/rh")
    public String attenteRh(Model model, Authentication authentication) {
        verifierRh(authentication);
        return afficherFile(model, "rh", "Validation RH",
                "Demandes en attente de validation RH",
                DemandeService.STATUT_ATTENTE_RH);
    }

    @GetMapping("/finance")
    public String attenteFinance(Model model, Authentication authentication) {
        verifierFinance(authentication);
        java.util.List<Demande> demandes = demandeService.listerParStatut(DemandeService.STATUT_ATTENTE_BUDGET);
        boolean estRhOuAdmin = isRh(authentication) || isAdmin(authentication);
        if (!estRhOuAdmin) {
            demandes = demandes.stream()
                    .filter(d -> memeDepartement(authentication, d))
                    .toList();
        }
        model.addAttribute("pageTitle", "Validation Finance");
        model.addAttribute("titre", "Demandes en attente de validation du budget");
        model.addAttribute("etape", "finance");
        model.addAttribute("demandes", demandes);
        return "demande/validation";
    }

    @GetMapping("/direction")
    public String attenteDirection(Model model, Authentication authentication) {
        verifierDirection(authentication);
        return afficherFile(model, "direction", "Validation Direction",
                "Demandes en attente de validation de la direction",
                DemandeService.STATUT_ATTENTE_DIRECTION);
    }

    @PostMapping("/{id}/rh/accepter")
    public String accepterRh(@PathVariable Integer id, Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        verifierRh(authentication);
        return executer(id, "rh", "Demande validee par la RH, transmise a la finance.",
                () -> demandeService.validerRh(id), redirectAttributes);
    }

    @PostMapping("/{id}/rh/refuser")
    public String refuserRh(@PathVariable Integer id, Authentication authentication,
                            RedirectAttributes redirectAttributes) {
        verifierRh(authentication);
        return executer(id, "rh", "Demande refusee par la RH.",
                () -> demandeService.refuserRh(id), redirectAttributes);
    }

    @PostMapping("/{id}/finance/accepter")
    public String accepterFinance(@PathVariable Integer id, Authentication authentication,
                                  RedirectAttributes redirectAttributes) {
        verifierFinance(authentication);
        return executer(id, "finance", "Budget valide, demande transmise a la direction.",
                () -> demandeService.validerFinance(id), redirectAttributes);
    }

    @PostMapping("/{id}/finance/refuser")
    public String refuserFinance(@PathVariable Integer id, Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        verifierFinance(authentication);
        return executer(id, "finance", "Budget refuse par la finance.",
                () -> demandeService.refuserFinance(id), redirectAttributes);
    }

    @PostMapping("/{id}/direction/accepter")
    public String accepterDirection(@PathVariable Integer id, Authentication authentication,
                                    RedirectAttributes redirectAttributes) {
        verifierDirection(authentication);
        return executer(id, "direction", "Demande approuvee par la direction.",
                () -> demandeService.validerDirection(id), redirectAttributes);
    }

    @PostMapping("/{id}/direction/refuser")
    public String refuserDirection(@PathVariable Integer id, Authentication authentication,
                                   RedirectAttributes redirectAttributes) {
        verifierDirection(authentication);
        return executer(id, "direction", "Demande refusee par la direction.",
                () -> demandeService.refuserDirection(id), redirectAttributes);
    }

    private String afficherFile(Model model, String etape, String pageTitle,
                                String titre, String statut) {
        model.addAttribute("pageTitle", pageTitle);
        model.addAttribute("titre", titre);
        model.addAttribute("etape", etape);
        model.addAttribute("demandes", demandeService.listerParStatut(statut));
        return "demande/validation";
    }

    private String executer(Integer id, String etape, String message,
                            Runnable action, RedirectAttributes redirectAttributes) {
        try {
            action.run();
            redirectAttributes.addFlashAttribute("succes", message);
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("erreur", ex.getMessage());
        }
        return "redirect:/demandes/validation/" + etape;
    }

    private void verifierFinance(Authentication authentication) {
        if (authentication == null) {
            throw new AccessDeniedException("Utilisateur non authentifie.");
        }
        boolean autorise = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMINISTRATEUR".equals(authority.getAuthority())
                        || "ROLE_RH".equals(authority.getAuthority()));
        if (autorise) {
            return;
        }
        if (authentication.getPrincipal() instanceof UtilisateurDetails details) {
            Utilisateur utilisateur = details.getUtilisateur();
            if (utilisateur.getEmploye() != null
                    && utilisateur.getEmploye().getDepartement() != null
                    && "Finance".equalsIgnoreCase(utilisateur.getEmploye().getDepartement().getNomDept())) {
                return;
            }
        }
        throw new AccessDeniedException("Seuls les membres du departement Finance peuvent valider un budget.");
    }

    private void verifierRh(Authentication authentication) {
        if (authentication == null) {
            throw new AccessDeniedException("Utilisateur non authentifie.");
        }
        if (isRh(authentication) || isAdmin(authentication)) {
            return;
        }
        throw new AccessDeniedException("Acces reserve a la RH.");
    }

    private void verifierDirection(Authentication authentication) {
        if (authentication == null) {
            throw new AccessDeniedException("Utilisateur non authentifie.");
        }
        if (isAdmin(authentication)) {
            return;
        }
        throw new AccessDeniedException("Acces reserve a l'administration.");
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMINISTRATEUR".equals(authority.getAuthority()));
    }

    private boolean isRh(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_RH".equals(authority.getAuthority()));
    }

    private boolean memeDepartement(Authentication authentication, Demande demande) {
        if (!(authentication.getPrincipal() instanceof UtilisateurDetails details)) {
            return false;
        }
        Utilisateur utilisateur = details.getUtilisateur();
        if (utilisateur.getEmploye() == null || utilisateur.getEmploye().getDepartement() == null
                || demande.getDepartement() == null) {
            return false;
        }
        return utilisateur.getEmploye().getDepartement().getIdDept() != null
                && utilisateur.getEmploye().getDepartement().getIdDept()
                        .equals(demande.getDepartement().getIdDept());
    }
}
