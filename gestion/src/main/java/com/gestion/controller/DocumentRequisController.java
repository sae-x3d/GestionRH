package com.gestion.controller;

import com.gestion.model.DocumentRequis;
import com.gestion.repository.DocumentRequisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller @RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_ADMINISTRATEUR','ROLE_RH')")
public class DocumentRequisController {
    private final DocumentRequisRepository repository;

    @GetMapping("/employes/documents-requis")
    public String liste(Model model) {
        model.addAttribute("pageTitle", "Documents obligatoires");
        model.addAttribute("documents", repository.findAll());
        return "employe/documents-requis";
    }

    @PostMapping("/employes/documents-requis")
    public String ajouter(@RequestParam String typeDocument, RedirectAttributes flash) {
        String type = typeDocument == null ? "" : typeDocument.trim();
        if (type.isBlank() || type.length() > 100) flash.addFlashAttribute("erreur", "Type de document invalide.");
        else if (repository.existsByTypeDocumentIgnoreCase(type)) flash.addFlashAttribute("erreur", "Ce document est déjà configuré.");
        else { repository.save(DocumentRequis.builder().typeDocument(type).actif(true).build()); flash.addFlashAttribute("succes", "Document requis ajouté."); }
        return "redirect:/employes/documents-requis";
    }

    @PostMapping("/employes/documents-requis/{id}/actif")
    public String basculer(@PathVariable Integer id, RedirectAttributes flash) {
        DocumentRequis document = repository.findById(id).orElseThrow();
        document.setActif(!document.isActif()); repository.save(document);
        flash.addFlashAttribute("succes", "Configuration mise à jour.");
        return "redirect:/employes/documents-requis";
    }
}
