package com.gestion.controller;

import com.gestion.dto.CreerContratDTO;
import com.gestion.model.Utilisateur;
import com.gestion.service.ContratService;
import com.gestion.service.UtilisateurDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller @RequiredArgsConstructor
public class ContratController {
    private final ContratService contratService;

    @GetMapping("/candidatures/rh/{id}/contrat/nouveau")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMINISTRATEUR','ROLE_RH')")
    public String nouveau(@PathVariable Integer id, Model model) {
        var existing = contratService.contratCandidature(id);
        if (existing != null) return "redirect:/contrats/" + existing.getIdContrat();
        CreerContratDTO dto = contratService.preparerFormulaire(id);
        dto.setNomSalarie(valeurFlash(model, "nomSalarie", dto.getNomSalarie()));
        dto.setAdresseSalarie(valeurFlash(model, "adresseSalarie", dto.getAdresseSalarie()));
        dto.setDateFin(dateFlash(model, "dateFin", dto.getDateFin()));
        dto.setMotifCdd(valeurFlash(model, "motifCdd", dto.getMotifCdd()));
        model.addAttribute("candidature", id); model.addAttribute("form", dto); model.addAttribute("pageTitle", "Emettre le contrat");
        return "contrat/formulaire";
    }

    @PostMapping("/candidatures/rh/{id}/contrat")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMINISTRATEUR','ROLE_RH')")
    public String creer(@PathVariable Integer id, @Valid @ModelAttribute("form") CreerContratDTO dto,
                        BindingResult errors, Model model, RedirectAttributes flash) {
        if (errors.hasErrors()) { model.addAttribute("candidature", id); model.addAttribute("pageTitle", "Emettre le contrat"); return "contrat/formulaire"; }
        try { var c=contratService.generer(id,dto); flash.addFlashAttribute("succes","Contrat PDF genere. Il attend l'acceptation du candidat."); return "redirect:/contrats/"+c.getIdContrat(); }
        catch (IllegalArgumentException | IllegalStateException ex) { model.addAttribute("erreur",ex.getMessage()); model.addAttribute("candidature",id); model.addAttribute("pageTitle","Emettre le contrat"); return "contrat/formulaire"; }
    }

    @GetMapping("/contrats")
    public String mesContrats(Model model, Authentication auth) {
        Utilisateur u=utilisateur(auth); model.addAttribute("pageTitle","Mes contrats"); model.addAttribute("contrats",contratService.contratsCandidat(u.getIdUser())); return "contrat/liste";
    }

    @GetMapping("/contrats/{id}")
    public String voir(@PathVariable Integer id, Model model, Authentication auth) {
        var c=contratService.trouver(id);
        if (!rh(auth) && !c.getCandidature().getUtilisateur().getIdUser().equals(utilisateur(auth).getIdUser())) throw new org.springframework.security.access.AccessDeniedException("Acces refuse");
        model.addAttribute("pageTitle","Contrat de travail"); model.addAttribute("contrat",c); return "contrat/detail";
    }

    @GetMapping("/contrats/{id}/pdf")
    public ResponseEntity<ByteArrayResource> pdf(@PathVariable Integer id,
                                                  @RequestParam(name = "inline", defaultValue = "false") boolean inline,
                                                  Authentication auth) {
        var c=contratService.trouver(id);
        if (!rh(auth) && !c.getCandidature().getUtilisateur().getIdUser().equals(utilisateur(auth).getIdUser())) throw new org.springframework.security.access.AccessDeniedException("Acces refuse");
        String disposition = inline ? "inline" : "attachment";
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition + "; filename=contrat-" + id + ".pdf")
                .body(new ByteArrayResource(c.getPdf()));
    }

    @PostMapping("/contrats/{id}/reponse")
    public String repondre(@PathVariable Integer id, @RequestParam boolean accepte, Authentication auth, RedirectAttributes flash) {
        try { contratService.repondre(id,utilisateur(auth).getIdUser(),accepte); flash.addFlashAttribute("succes",accepte?"Acceptation enregistree. Votre fiche employe est creee.":"Refus enregistre."); }
        catch (IllegalArgumentException | IllegalStateException ex) { flash.addFlashAttribute("erreur",ex.getMessage()); }
        return "redirect:/contrats/"+id;
    }
    private Utilisateur utilisateur(Authentication a) { if(a!=null && a.getPrincipal() instanceof UtilisateurDetails d) return d.getUtilisateur(); throw new org.springframework.security.access.AccessDeniedException("Connexion requise"); }
    private boolean rh(Authentication a) { return a!=null && a.getAuthorities().stream().anyMatch(x -> x.getAuthority().equals("ROLE_RH")||x.getAuthority().equals("ROLE_ADMINISTRATEUR")); }
    private String valeurFlash(Model model, String cle, String defaut) {
        Object valeur = model.asMap().get(cle);
        return valeur instanceof String texte && !texte.isBlank() ? texte : defaut;
    }
    private java.time.LocalDate dateFlash(Model model, String cle, java.time.LocalDate defaut) {
        Object valeur = model.asMap().get(cle);
        return valeur instanceof java.time.LocalDate date ? date : defaut;
    }
}
