package com.gestion.controller;

import com.gestion.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    @GetMapping("/audit")
    @PreAuthorize("hasAuthority('ROLE_ADMINISTRATEUR')")
    public String journal(@RequestParam(name = "utilisateur", required = false) String utilisateur,
                          Model model) {
        model.addAttribute("pageTitle", "Journal d'audit");
        model.addAttribute("utilisateurFiltre", utilisateur);
        model.addAttribute("entrees", utilisateur == null || utilisateur.isBlank()
                ? auditService.lister()
                : auditService.listerParUtilisateur(utilisateur.trim()));
        return "audit/journal";
    }
}
