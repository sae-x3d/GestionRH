package com.gestion.controller;

import com.gestion.enums.TypeAbsence;
import com.gestion.exception.AccesRefuseException;
import com.gestion.model.Utilisateur;
import com.gestion.service.AbsenceService;
import com.gestion.service.UtilisateurDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.core.io.ByteArrayResource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDate;

@Controller @RequiredArgsConstructor
public class AbsenceController {
    private final AbsenceService absenceService;

    @GetMapping("/absences")
    public String index(Model model, Authentication authentication) {
        Utilisateur u = utilisateur(authentication);
        model.addAttribute("pageTitle", "Absences");
        model.addAttribute("absences", absenceService.lister(u.getEmploye(), u));
        model.addAttribute("types", TypeAbsence.values());
        model.addAttribute("estRh", u.getRole() != null && (u.getRole().getNomRole().contains("RH")
                || u.getRole().getNomRole().contains("ADMINISTRATEUR")));
        return "absences/index";
    }

    @PostMapping("/absences")
    public String soumettre(@RequestParam TypeAbsence type,
                            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
                            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin,
                            @RequestParam MultipartFile justificatif, Authentication authentication,
                            RedirectAttributes flash) {
        try { absenceService.soumettre(utilisateur(authentication).getEmploye(), type, debut, fin, justificatif);
            flash.addFlashAttribute("succes", "Absence transmise a la RH avec justificatif.");
        } catch (IllegalStateException e) { flash.addFlashAttribute("erreur", e.getMessage()); }
        return "redirect:/absences";
    }

    @PostMapping("/absences/{id}/decision")
    @PreAuthorize("hasAnyAuthority('ROLE_RH','ROLE_ADMINISTRATEUR')")
    public String decision(@PathVariable Integer id, @RequestParam boolean accepter,
                           Authentication authentication, RedirectAttributes flash) {
        try { absenceService.decider(id, accepter, utilisateur(authentication));
            flash.addFlashAttribute("succes", "Decision d'absence enregistree.");
        } catch (IllegalStateException | AccesRefuseException e) { flash.addFlashAttribute("erreur", e.getMessage()); }
        return "redirect:/absences";
    }

    @GetMapping("/absences/{id}/justificatif")
    public ResponseEntity<ByteArrayResource> justificatif(@PathVariable Integer id, Authentication auth) {
        Path path = absenceService.justificatif(id, utilisateur(auth));
        try {
            return ResponseEntity.ok().contentType(MediaTypeFactory.getMediaType(path.getFileName().toString())
                    .orElse(MediaType.APPLICATION_OCTET_STREAM))
                    .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                            .filename(path.getFileName().toString()).build().toString())
                    .body(new ByteArrayResource(Files.readAllBytes(path)));
        } catch (IOException e) { throw new com.gestion.exception.RessourceIntrouvableException("Justificatif absent."); }
    }

    private Utilisateur utilisateur(Authentication a) {
        if (a != null && a.getPrincipal() instanceof UtilisateurDetails d) return d.getUtilisateur();
        throw new AccesRefuseException("Vous devez etre connecte.");
    }
}
