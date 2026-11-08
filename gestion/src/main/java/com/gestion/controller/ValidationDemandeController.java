package com.gestion.controller;

import com.gestion.exception.AccesRefuseException;
import com.gestion.model.Demande;
import com.gestion.model.Utilisateur;
import com.gestion.service.DemandeService;
import com.gestion.service.UtilisateurDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/demandes")
@RequiredArgsConstructor
public class ValidationDemandeController {

    private final DemandeService demandeService;

    @PreAuthorize("hasAnyAuthority('ROLE_ADMINISTRATEUR', 'ROLE_FINANCE', 'ROLE_MANAGER', 'ROLE_EMPLOYE')")
    @GetMapping("/validation/finance")
    public String attenteFinance(Model model, Authentication authentication) {
        verifierFinance(authentication);
        java.util.List<Demande> demandes = demandeService.listerParStatut(DemandeService.STATUT_ATTENTE_BUDGET);
        model.addAttribute("pageTitle", "Validation Finance");
        model.addAttribute("titre", "Demandes en attente de validation du budget");
        model.addAttribute("etape", "finance");
        model.addAttribute("demandes", demandes);
        return "demande/validation";
    }

    @PreAuthorize("hasAuthority('ROLE_ADMINISTRATEUR')")
    @GetMapping("/validation/direction")
    public String attenteDirection(Model model, Authentication authentication) {
        verifierDirection(authentication);
        return afficherFile(model, "direction", "Validation Direction",
                "Demandes en attente de validation de la direction",
                DemandeService.STATUT_ATTENTE_DIRECTION);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMINISTRATEUR', 'ROLE_FINANCE', 'ROLE_MANAGER', 'ROLE_EMPLOYE')")
    @PostMapping("/{id}/validation/finance/accepter")
    public String accepterFinance(@PathVariable Integer id, Authentication authentication,
                                  RedirectAttributes redirectAttributes) {
        verifierFinance(authentication);
        Utilisateur utilisateur = utilisateurCourant(authentication);
        return executer(id, "finance", "Budget valide, demande transmise a la direction.",
                () -> demandeService.validerFinance(id, utilisateur), redirectAttributes);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMINISTRATEUR', 'ROLE_FINANCE', 'ROLE_MANAGER', 'ROLE_EMPLOYE')")
    @PostMapping("/{id}/validation/finance/refuser")
    public String refuserFinance(@PathVariable Integer id, Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        verifierFinance(authentication);
        Utilisateur utilisateur = utilisateurCourant(authentication);
        return executer(id, "finance", "Budget refuse par la finance.",
                () -> demandeService.refuserFinance(id, utilisateur), redirectAttributes);
    }

    @PreAuthorize("hasAuthority('ROLE_ADMINISTRATEUR')")
    @PostMapping("/{id}/validation/direction/accepter")
    public String accepterDirection(@PathVariable Integer id, Authentication authentication,
                                    RedirectAttributes redirectAttributes) {
        verifierDirection(authentication);
        Utilisateur utilisateur = utilisateurCourant(authentication);
        return executer(id, "direction", "Demande approuvee par la direction.",
                () -> demandeService.validerDirection(id, utilisateur), redirectAttributes);
    }

    @PreAuthorize("hasAuthority('ROLE_ADMINISTRATEUR')")
    @PostMapping("/{id}/validation/direction/refuser")
    public String refuserDirection(@PathVariable Integer id, Authentication authentication,
                                   RedirectAttributes redirectAttributes) {
        verifierDirection(authentication);
        Utilisateur utilisateur = utilisateurCourant(authentication);
        return executer(id, "direction", "Demande refusee par la direction.",
                () -> demandeService.refuserDirection(id, utilisateur), redirectAttributes);
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
        } catch (AccesRefuseException ex) {
            throw new AccessDeniedException(ex.getMessage(), ex);
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("erreur", ex.getMessage());
        }
        return "redirect:/demandes/validation/" + etape;
    }

    private void verifierFinance(Authentication authentication) {
        if (authentication == null) {
            throw new AccessDeniedException("Utilisateur non authentifie.");
        }
        Utilisateur utilisateur = utilisateurCourant(authentication);
        if (demandeService.estAdministrateur(utilisateur) || demandeService.estFinance(utilisateur)) {
            return;
        }
        throw new AccessDeniedException("Seuls les profils Finance peuvent valider un budget.");
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

    private Utilisateur utilisateurCourant(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof UtilisateurDetails details) {
            return details.getUtilisateur();
        }
        throw new AccessDeniedException("Aucun utilisateur authentifie.");
    }
}
