package com.gestion.controller;

import com.gestion.dto.CreerContratDTO;
import com.gestion.enums.TypeObjetValidation;
import com.gestion.model.Utilisateur;
import com.gestion.service.ContratService;
import com.gestion.service.UtilisateurDetails;
import com.gestion.service.ValidationWorkflowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class ContratController {

    private final ContratService contratService;
    private final ValidationWorkflowService workflowService;

    @GetMapping("/candidatures/rh/{id}/contrat/nouveau")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMINISTRATEUR','ROLE_RH')")
    public String nouveau(@PathVariable Integer id, Model model) {
        var existing = contratService.contratActifCandidature(id);
        if (existing != null) {
            return "redirect:/contrats/" + existing.getIdContrat();
        }
        CreerContratDTO dto = contratService.preparerFormulaire(id);
        dto.setNomSalarie(valeurFlash(model, "nomSalarie", dto.getNomSalarie()));
        dto.setAdresseSalarie(valeurFlash(model, "adresseSalarie", dto.getAdresseSalarie()));
        dto.setDateFin(dateFlash(model, "dateFin", dto.getDateFin()));
        dto.setMotifCdd(valeurFlash(model, "motifCdd", dto.getMotifCdd()));
        model.addAttribute("candidature", id);
        model.addAttribute("form", dto);
        model.addAttribute("pageTitle", "Emettre le contrat");
        return "contrat/formulaire";
    }

    @PostMapping("/candidatures/rh/{id}/contrat")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMINISTRATEUR','ROLE_RH')")
    public String creer(@PathVariable Integer id, @Valid @ModelAttribute("form") CreerContratDTO dto,
                        BindingResult errors, Model model, Authentication authentication,
                        RedirectAttributes flash) {
        if (errors.hasErrors()) {
            model.addAttribute("candidature", id);
            model.addAttribute("pageTitle", "Emettre le contrat");
            return "contrat/formulaire";
        }
        try {
            var contrat = contratService.generer(id, dto, utilisateur(authentication));
            flash.addFlashAttribute("succes",
                    "Contrat cree. Il attend la validation Manager, RH puis Direction avant d'etre transmis au candidat.");
            return "redirect:/contrats/" + contrat.getIdContrat();
        } catch (IllegalArgumentException | IllegalStateException ex) {
            model.addAttribute("erreur", ex.getMessage());
            model.addAttribute("candidature", id);
            model.addAttribute("pageTitle", "Emettre le contrat");
            return "contrat/formulaire";
        }
    }

    @GetMapping("/contrats")
    public String mesContrats(Model model, Authentication authentication) {
        Utilisateur utilisateur = utilisateur(authentication);
        model.addAttribute("pageTitle", "Mes contrats");
        model.addAttribute("contrats", contratService.contratsCandidat(utilisateur.getIdUser()));
        model.addAttribute("demandeur", utilisateur);
        return "contrat/liste";
    }

    @GetMapping("/contrats/{id}")
    public String voir(@PathVariable Integer id, Model model, Authentication authentication) {
        Utilisateur utilisateur = utilisateur(authentication);
        var contrat = contratService.trouver(id);
        if (!contratService.peutVoir(contrat, utilisateur)) {
            throw new AccessDeniedException("Acces refuse");
        }
        model.addAttribute("pageTitle", "Contrat de travail");
        model.addAttribute("contrat", contrat);
        model.addAttribute("dossier", workflowService.dernierDossier(
                TypeObjetValidation.CONTRAT, id));
        model.addAttribute("historique", workflowService.historique(
                TypeObjetValidation.CONTRAT, id));
        model.addAttribute("demandeur", utilisateur);
        return "contrat/detail";
    }

    @GetMapping("/contrats/{id}/pdf")
    public ResponseEntity<ByteArrayResource> pdf(@PathVariable Integer id,
                                                 @RequestParam(name = "inline", defaultValue = "false") boolean inline,
                                                 Authentication authentication) {
        var contrat = contratService.trouver(id);
        if (!contratService.peutVoir(contrat, utilisateur(authentication))) {
            throw new AccessDeniedException("Acces refuse");
        }
        if (contrat.getPdf() == null) {
            throw new IllegalStateException("Le PDF sera disponible apres la validation Direction.");
        }
        String disposition = inline ? "inline" : "attachment";
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        disposition + "; filename=contrat-" + id + ".pdf")
                .body(new ByteArrayResource(contrat.getPdf()));
    }

    @PostMapping("/contrats/{id}/reponse")
    public String repondre(@PathVariable Integer id, @RequestParam boolean accepte,
                           Authentication authentication, RedirectAttributes flash) {
        try {
            contratService.repondre(id, utilisateur(authentication).getIdUser(), accepte);
            flash.addFlashAttribute("succes", accepte
                    ? "Acceptation enregistree. Votre parcours d'integration est ouvert."
                    : "Refus enregistre.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            flash.addFlashAttribute("erreur", ex.getMessage());
        }
        return "redirect:/contrats/" + id;
    }

    @PostMapping("/contrats/{id}/renouveler")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMINISTRATEUR','ROLE_RH')")
    public String renouveler(@PathVariable Integer id,
                             @RequestParam(name = "typeCible", defaultValue = "CDD") String typeCible,
                             @RequestParam(name = "dateFin", required = false)
                             @org.springframework.format.annotation.DateTimeFormat(
                                     iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
                             java.time.LocalDate dateFin,
                             @RequestParam(name = "salaire", required = false) Integer salaire,
                             @RequestParam(name = "motif", required = false) String motif,
                             Authentication authentication, RedirectAttributes flash) {
        try {
            var contrat = contratService.renouveler(id, typeCible, dateFin, salaire, motif,
                    utilisateur(authentication));
            flash.addFlashAttribute("succes", "Nouveau contrat enregistre.");
            return "redirect:/contrats/" + contrat.getIdContrat();
        } catch (IllegalArgumentException | IllegalStateException ex) {
            flash.addFlashAttribute("erreur", ex.getMessage());
        }
        return "redirect:/contrats/" + id;
    }

    @PostMapping("/contrats/{id}/convertir-cdi")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMINISTRATEUR','ROLE_RH')")
    public String convertirEnCdi(@PathVariable Integer id,
                                 @RequestParam(name = "salaire", required = false) Integer salaire,
                                 Authentication authentication, RedirectAttributes flash) {
        try {
            var contrat = contratService.convertirEnCdi(id, salaire, utilisateur(authentication));
            flash.addFlashAttribute("succes", "Contrat converti en CDI.");
            return "redirect:/contrats/" + contrat.getIdContrat();
        } catch (IllegalArgumentException | IllegalStateException ex) {
            flash.addFlashAttribute("erreur", ex.getMessage());
        }
        return "redirect:/contrats/" + id;
    }

    @PostMapping("/contrats/{id}/avenant")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMINISTRATEUR','ROLE_RH')")
    public String avenant(@PathVariable Integer id,
                          @RequestParam(name = "salaire", required = false) Integer salaire,
                          @RequestParam(name = "lieuTravail", required = false) String lieuTravail,
                          @RequestParam(name = "commentaire", required = false) String commentaire,
                          Authentication authentication, RedirectAttributes flash) {
        try {
            var contrat = contratService.avenant(id, salaire, lieuTravail, commentaire,
                    utilisateur(authentication));
            flash.addFlashAttribute("succes", "Avenant enregistre.");
            return "redirect:/contrats/" + contrat.getIdContrat();
        } catch (IllegalArgumentException | IllegalStateException ex) {
            flash.addFlashAttribute("erreur", ex.getMessage());
        }
        return "redirect:/contrats/" + id;
    }

    private Utilisateur utilisateur(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof UtilisateurDetails details) {
            return details.getUtilisateur();
        }
        throw new AccessDeniedException("Connexion requise");
    }

    private String valeurFlash(Model model, String cle, String defaut) {
        Object valeur = model.asMap().get(cle);
        return valeur instanceof String texte && !texte.isBlank() ? texte : defaut;
    }

    private java.time.LocalDate dateFlash(Model model, String cle, java.time.LocalDate defaut) {
        Object valeur = model.asMap().get(cle);
        return valeur instanceof java.time.LocalDate date ? date : defaut;
    }
}
