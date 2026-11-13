package com.gestion.controller;

import com.gestion.enums.TypeObjetValidation;
import com.gestion.exception.AccesRefuseException;
import com.gestion.exception.RessourceIntrouvableException;
import com.gestion.model.Document;
import com.gestion.model.Onboarding;
import com.gestion.model.Utilisateur;
import com.gestion.service.OnboardingService;
import com.gestion.service.UtilisateurDetails;
import com.gestion.service.ValidationWorkflowService;
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
public class OnboardingController {

    private final OnboardingService onboardingService;
    private final ValidationWorkflowService workflowService;

    @GetMapping("/mon-onboarding")
    @PreAuthorize("isAuthenticated()")
    public String monOnboarding(Model model, Authentication authentication) {
        Utilisateur utilisateur = utilisateur(authentication);
        model.addAttribute("pageTitle", "Mon integration");
        model.addAttribute("onboardings", onboardingService.mesOnboardings(utilisateur));
        model.addAttribute("demandeur", utilisateur);
        return "onboarding/mes-onboardings";
    }

    @GetMapping("/onboarding")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMINISTRATEUR','ROLE_RH','ROLE_MANAGER')")
    public String liste(Model model, Authentication authentication) {
        Utilisateur utilisateur = utilisateur(authentication);
        model.addAttribute("pageTitle", "Onboarding");
        model.addAttribute("onboardings", onboardingService.lister(utilisateur));
        model.addAttribute("demandeur", utilisateur);
        return "onboarding/liste";
    }

    @GetMapping("/onboarding/{id}")
    @PreAuthorize("isAuthenticated()")
    public String detail(@PathVariable Integer id, Model model, Authentication authentication) {
        Utilisateur utilisateur = utilisateur(authentication);
        Onboarding onboarding = onboardingService.trouver(id, utilisateur);
        model.addAttribute("pageTitle", "Parcours d'integration");
        model.addAttribute("onboarding", onboarding);
        model.addAttribute("taches", onboardingService.taches(onboarding));
        model.addAttribute("documents", onboardingService.documents(onboarding));
        model.addAttribute("dossier", workflowService.dernierDossier(
                TypeObjetValidation.ONBOARDING, id));
        model.addAttribute("historique", workflowService.historique(
                TypeObjetValidation.ONBOARDING, id));
        model.addAttribute("demandeur", utilisateur);
        return "onboarding/detail";
    }

    @PostMapping("/onboarding/{id}/taches/{idTache}")
    @PreAuthorize("isAuthenticated()")
    public String cocherTache(@PathVariable Integer id, @PathVariable Integer idTache,
                              @RequestParam(name = "fait", defaultValue = "true") boolean fait,
                              @RequestParam(name = "commentaire", required = false) String commentaire,
                              Authentication authentication, RedirectAttributes flash) {
        try {
            onboardingService.cocherTache(idTache, utilisateur(authentication), fait, commentaire);
            flash.addFlashAttribute("succes", fait ? "Tache terminee." : "Tache reouverte.");
        } catch (AccesRefuseException | RessourceIntrouvableException | IllegalStateException ex) {
            flash.addFlashAttribute("erreur", ex.getMessage());
        }
        return "redirect:/onboarding/" + id;
    }

    @PostMapping("/onboarding/{id}/documents")
    @PreAuthorize("isAuthenticated()")
    public String ajouterDocument(@PathVariable Integer id,
                                  @RequestParam(name = "typeDocument") String typeDocument,
                                  @RequestParam(name = "fichier") MultipartFile fichier,
                                  Authentication authentication, RedirectAttributes flash) {
        try {
            onboardingService.ajouterDocument(id, typeDocument, fichier, utilisateur(authentication));
            flash.addFlashAttribute("succes", "Document ajoute au parcours.");
        } catch (AccesRefuseException | RessourceIntrouvableException | IllegalStateException ex) {
            flash.addFlashAttribute("erreur", ex.getMessage());
        }
        return "redirect:/onboarding/" + id;
    }

    @PostMapping("/onboarding/{id}/soumettre")
    @PreAuthorize("isAuthenticated()")
    public String soumettre(@PathVariable Integer id,
                            @RequestParam(name = "decisionEssai", required = false) String decisionEssai,
                            @RequestParam(name = "commentaire", required = false) String commentaire,
                            Authentication authentication, RedirectAttributes flash) {
        try {
            onboardingService.soumettreCloture(id, utilisateur(authentication), decisionEssai, commentaire);
            flash.addFlashAttribute("succes",
                    "Onboarding soumis. Il attend la validation Manager, RH puis Direction.");
        } catch (AccesRefuseException | RessourceIntrouvableException | IllegalStateException ex) {
            flash.addFlashAttribute("erreur", ex.getMessage());
        }
        return "redirect:/onboarding/" + id;
    }

    @GetMapping("/onboarding/{id}/documents/{idDocument}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ByteArrayResource> document(@PathVariable Integer id,
                                                      @PathVariable Integer idDocument,
                                                      Authentication authentication) {
        Document document = onboardingService.document(idDocument, utilisateur(authentication));
        Path chemin = onboardingService.cheminDocument(document);
        try {
            byte[] contenu = Files.readAllBytes(chemin);
            MediaType type = MediaTypeFactory.getMediaType(document.getNomOriginal())
                    .orElse(MediaType.APPLICATION_OCTET_STREAM);
            return ResponseEntity.ok()
                    .contentType(type)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            ContentDisposition.attachment()
                                    .filename(document.getNomOriginal()).build().toString())
                    .body(new ByteArrayResource(contenu));
        } catch (IOException ex) {
            throw new RessourceIntrouvableException("Document introuvable.");
        }
    }

    private Utilisateur utilisateur(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof UtilisateurDetails details) {
            return details.getUtilisateur();
        }
        throw new AccesRefuseException("Vous devez etre connecte.");
    }
}
