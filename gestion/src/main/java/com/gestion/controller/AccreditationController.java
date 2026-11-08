package com.gestion.controller;

import com.gestion.dto.AccreditationForm;
import com.gestion.exception.AccesRefuseException;
import com.gestion.exception.RessourceIntrouvableException;
import com.gestion.model.Accreditation;
import com.gestion.model.Utilisateur;
import com.gestion.service.AccreditationService;
import com.gestion.service.CandidatureService;
import com.gestion.service.UtilisateurDetails;
import com.gestion.service.ValidationWorkflowService;
import com.gestion.enums.TypeObjetValidation;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Controller
@RequiredArgsConstructor
public class AccreditationController {

    private final AccreditationService accreditationService;
    private final CandidatureService candidatureService;
    private final ValidationWorkflowService workflowService;

    @GetMapping("/mes-accreditations")
    @PreAuthorize("isAuthenticated()")
    public String mesAccreditations(Model model, Authentication authentication) {
        Utilisateur utilisateur = utilisateur(authentication);
        model.addAttribute("pageTitle", "Mes accreditations");
        model.addAttribute("accreditations", accreditationService.mesAccreditations(utilisateur.getIdUser()));
        model.addAttribute("demandeur", utilisateur);
        return "accreditation/liste";
    }

    @GetMapping("/accreditations")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMINISTRATEUR','ROLE_RH','ROLE_MANAGER')")
    public String liste(Model model, Authentication authentication) {
        Utilisateur utilisateur = utilisateur(authentication);
        model.addAttribute("pageTitle", "Accreditations");
        model.addAttribute("accreditations", accreditationService.listerPourGestionnaire(utilisateur));
        model.addAttribute("demandeur", utilisateur);
        return "accreditation/gestion";
    }

    @GetMapping("/accreditations/nouveau")
    @PreAuthorize("isAuthenticated()")
    public String nouveau(@RequestParam(name = "candidature", required = false) Integer idCandidature,
                          Model model, Authentication authentication) {
        Utilisateur utilisateur = utilisateur(authentication);
        AccreditationForm form = new AccreditationForm();
        form.setIdCandidature(idCandidature);
        form.setIdUtilisateur(utilisateur.getIdUser());
        Integer titulaireId = utilisateur.getIdUser();
        if (idCandidature != null
                && (workflowService.estAdministrateur(utilisateur) || workflowService.estRh(utilisateur))) {
            var candidature = candidatureService.trouverParIdAvecDetails(idCandidature);
            if (candidature.getUtilisateur() != null) {
                titulaireId = candidature.getUtilisateur().getIdUser();
                form.setIdUtilisateur(titulaireId);
            }
        }
        model.addAttribute("pageTitle", "Declarer une accreditation");
        model.addAttribute("form", form);
        model.addAttribute("candidatures", candidatureService.listerParUtilisateur(titulaireId));
        model.addAttribute("demandeur", utilisateur);
        return "accreditation/formulaire";
    }

    @PostMapping("/accreditations")
    @PreAuthorize("isAuthenticated()")
    public String soumettre(@ModelAttribute("form") AccreditationForm form,
                            @RequestParam(name = "document", required = false) MultipartFile document,
                            Model model, Authentication authentication, RedirectAttributes flash) {
        Utilisateur utilisateur = utilisateur(authentication);
        try {
            Accreditation accreditation = accreditationService.soumettre(form, document, utilisateur);
            flash.addFlashAttribute("succes",
                    "Accreditation deposee. Elle attend la validation Manager, RH puis Direction.");
            return "redirect:/accreditations/" + accreditation.getIdAccreditation();
        } catch (AccesRefuseException | RessourceIntrouvableException | IllegalStateException ex) {
            model.addAttribute("erreur", ex.getMessage());
            model.addAttribute("pageTitle", "Declarer une accreditation");
            model.addAttribute("candidatures", candidatureService.listerParUtilisateur(
                    idTitulairePourListe(form, utilisateur)));
            model.addAttribute("demandeur", utilisateur);
            return "accreditation/formulaire";
        }
    }

    @GetMapping("/accreditations/{id}")
    @PreAuthorize("isAuthenticated()")
    public String detail(@PathVariable Integer id, Model model, Authentication authentication) {
        Utilisateur utilisateur = utilisateur(authentication);
        Accreditation accreditation = accreditationService.trouver(id, utilisateur);
        model.addAttribute("pageTitle", "Accreditation");
        model.addAttribute("accreditation", accreditation);
        model.addAttribute("dossier", workflowService.dernierDossier(
                TypeObjetValidation.ACCREDITATION, id));
        model.addAttribute("historique", workflowService.historique(
                TypeObjetValidation.ACCREDITATION, id));
        model.addAttribute("demandeur", utilisateur);
        return "accreditation/detail";
    }

    @GetMapping("/accreditations/{id}/document")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ByteArrayResource> document(@PathVariable Integer id,
                                                      Authentication authentication) {
        Accreditation accreditation = accreditationService.trouver(id, utilisateur(authentication));
        Path chemin = accreditationService.cheminDocument(accreditation);
        try {
            byte[] contenu = Files.readAllBytes(chemin);
            String nom = accreditationService.nomDocument(accreditation);
            MediaType type = MediaTypeFactory.getMediaType(nom)
                    .orElse(MediaType.APPLICATION_OCTET_STREAM);
            return ResponseEntity.ok()
                    .contentType(type)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            ContentDisposition.attachment().filename(nom).build().toString())
                    .body(new ByteArrayResource(contenu));
        } catch (IOException ex) {
            throw new RessourceIntrouvableException("Document justificatif introuvable.");
        }
    }

    private Integer idTitulairePourListe(AccreditationForm form, Utilisateur utilisateur) {
        if (form.getIdUtilisateur() != null
                && (workflowService.estAdministrateur(utilisateur) || workflowService.estRh(utilisateur))) {
            return form.getIdUtilisateur();
        }
        return utilisateur.getIdUser();
    }

    private Utilisateur utilisateur(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof UtilisateurDetails details) {
            return details.getUtilisateur();
        }
        throw new AccesRefuseException("Vous devez etre connecte.");
    }
}
