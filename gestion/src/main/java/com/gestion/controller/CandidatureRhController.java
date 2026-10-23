package com.gestion.controller;

import com.gestion.enums.TypeEntretien;
import com.gestion.exception.RessourceIntrouvableException;
import com.gestion.model.Candidature;
import com.gestion.model.Entretien;
import com.gestion.model.Utilisateur;
import com.gestion.service.CandidatureService;
import com.gestion.service.CandidatureStatusService;
import com.gestion.service.OffreEmploiService;
import com.gestion.service.PropositionEmbaucheService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Controller
@RequestMapping("/candidatures/rh")
@RequiredArgsConstructor
public class CandidatureRhController {

    private final CandidatureService candidatureService;
    private final OffreEmploiService offreEmploiService;
    private final CandidatureStatusService candidatureStatusService;
    private final PropositionEmbaucheService propositionEmbaucheService;

    @GetMapping
    public String liste(@RequestParam(name = "offreId", required = false) Integer offreId,
                        @RequestParam(name = "statut", required = false) String statut,
                        Model model,
                        Authentication authentication) {
        model.addAttribute("pageTitle", "Candidatures RH");
        model.addAttribute("candidatures", candidatureService.rechercherPourRh(offreId, statut));
        model.addAttribute("offres", offreEmploiService.listerToutes());
        model.addAttribute("statuts", candidatureStatusService.listerTous());
        model.addAttribute("offreId", offreId);
        model.addAttribute("statut", statut);
        model.addAttribute("demandeur", utilisateurCourant(authentication));
        return "candidature/rh-liste";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Integer id, Model model, Authentication authentication) {
        Candidature candidature = candidatureService.trouverParIdAvecDetails(id);
        List<Entretien> entretiens = candidature.getEntretiens().stream()
                .sorted(Comparator.comparing(Entretien::getDateHeure))
                .toList();
        model.addAttribute("pageTitle", "Dossier de candidature");
        model.addAttribute("candidature", candidature);
        model.addAttribute("entretiens", entretiens);
        model.addAttribute("typesEntretien", TypeEntretien.values());
        model.addAttribute("demandeur", utilisateurCourant(authentication));
        return "candidature/rh-detail";
    }

    @PostMapping("/{id}/examen")
    public String passerEnExamen(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        return executer(id, "La candidature passe en examen.", () -> candidatureService.marquerEnExamen(id),
                redirectAttributes);
    }

    @PostMapping("/{id}/retenir")
    public String retenir(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        return executer(id, "Le candidat est retenu pour l'entretien/les tests.",
                () -> candidatureService.retenir(id), redirectAttributes);
    }

    @PostMapping("/{id}/refuser")
    public String refuser(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        return executer(id, "La candidature est refusee.", () -> candidatureService.refuser(id),
                redirectAttributes);
    }

    @PostMapping("/{id}/entretiens")
    public String planifierEntretien(@PathVariable Integer id,
                                     @RequestParam("type") TypeEntretien type,
                                     @RequestParam("dateHeure")
                                     @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateHeure,
                                     @RequestParam(name = "lieu", required = false) String lieu,
                                     @RequestParam(name = "participants", required = false) String participants,
                                     RedirectAttributes redirectAttributes) {
        return executer(id, "Entretien/test planifie.", () -> candidatureService
                .planifierEntretien(id, type, dateHeure, lieu, participants), redirectAttributes);
    }

    @PostMapping("/{id}/entretiens/{idEntretien}/resultat")
    public String enregistrerResultat(@PathVariable Integer id,
                                      @PathVariable Integer idEntretien,
                                      @RequestParam("resultat") String resultat,
                                      @RequestParam(name = "evaluation", required = false) String evaluation,
                                      RedirectAttributes redirectAttributes) {
        return executer(id, "Resultat de l'entretien/test enregistre.",
                () -> candidatureService.enregistrerResultat(idEntretien, resultat, evaluation),
                redirectAttributes);
    }

    @PostMapping("/{id}/decision")
    public String decision(@PathVariable Integer id,
                           @RequestParam("decision") String decision,
                           RedirectAttributes redirectAttributes) {
        if ("ACCEPTER".equalsIgnoreCase(decision)) {
            return executer(id, "Candidature acceptee apres decision finale.",
                    () -> candidatureService.accepter(id), redirectAttributes);
        }
        return executer(id, "Candidature refusee apres decision finale.",
                () -> candidatureService.refuser(id), redirectAttributes);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMINISTRATEUR', 'ROLE_RH')")
    @GetMapping("/validation-embauche")
    public String listeValidationEmbauche(Model model, Authentication authentication) {
        model.addAttribute("pageTitle", "Validation finale embauche");
        model.addAttribute("candidatures", candidatureService.rechercherPourRh(null, "EN_ATTENTE_VALIDATION_RH_FINAL"));
        model.addAttribute("offres", offreEmploiService.listerToutes());
        model.addAttribute("statuts", candidatureStatusService.listerTous());
        model.addAttribute("demandeur", utilisateurCourant(authentication));
        return "candidature/rh-validation-embauche";
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMINISTRATEUR', 'ROLE_RH')")
    @PostMapping("/{id}/valider-embauche")
    public String validerEmbauche(@PathVariable Integer id, Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            propositionEmbaucheService.validerEmbaucheApresAcceptationCandidat(id, utilisateurCourant(authentication));
            redirectAttributes.addFlashAttribute("succes", "Embauche validee. Le dossier est finalise et l'employe a ete cree.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("erreur", ex.getMessage());
        }
        return "redirect:/candidatures/rh/validation-embauche";
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMINISTRATEUR', 'ROLE_RH')")
    @PostMapping("/{id}/refuser-embauche")
    public String refuserEmbauche(@PathVariable Integer id,
                                  @RequestParam(value = "motifRefus", required = false) String motifRefus,
                                  Authentication authentication,
                                  RedirectAttributes redirectAttributes) {
        try {
            propositionEmbaucheService.refuserEmbaucheApresAcceptationCandidat(id, utilisateurCourant(authentication), motifRefus);
            redirectAttributes.addFlashAttribute("succes", "Embauche refusee par la RH. Le dossier est cloture.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("erreur", ex.getMessage());
        }
        return "redirect:/candidatures/rh/validation-embauche";
    }

    private String executer(Integer id, String message, Runnable action,
                            RedirectAttributes redirectAttributes) {
        try {
            action.run();
            redirectAttributes.addFlashAttribute("succes", message);
        } catch (RessourceIntrouvableException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("erreur", ex.getMessage());
        }
        return "redirect:/candidatures/rh/" + id;
    }

    private Utilisateur utilisateurCourant(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof UtilisateurDetails details) {
            return details.getUtilisateur();
        }
        throw new IllegalStateException("Aucun utilisateur authentifie");
    }
}
