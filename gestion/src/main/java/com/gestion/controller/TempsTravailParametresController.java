package com.gestion.controller;

import com.gestion.model.ParametreTempsTravail;
import com.gestion.service.TempsTravailService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.math.BigDecimal;
import java.time.LocalTime;

@Controller @RequiredArgsConstructor
@RequestMapping("/temps-travail/parametres")
@PreAuthorize("hasAnyAuthority('ROLE_ADMINISTRATEUR','ROLE_RH')")
public class TempsTravailParametresController {
    private final TempsTravailService service;
    @GetMapping public String page(Model model) {
        model.addAttribute("pageTitle", "Paramètres du temps de travail");
        model.addAttribute("p", service.parametre()); return "planning/parametres";
    }
    @PostMapping public String sauver(@RequestParam BigDecimal heuresHebdomadaires,
            @RequestParam @DateTimeFormat(pattern="HH:mm") LocalTime heureDebut,
            @RequestParam @DateTimeFormat(pattern="HH:mm") LocalTime heureFin,
            @RequestParam Integer pauseMinutes, @RequestParam Integer toleranceRetardMinutes,
            @RequestParam Integer seuilTranche25Minutes, @RequestParam Integer majorationNuit,
            @RequestParam Integer majorationDimanche, @RequestParam Integer majorationFerie,
            RedirectAttributes flash) {
        try { service.enregistrerParametre(heuresHebdomadaires, heureDebut, heureFin, pauseMinutes,
                toleranceRetardMinutes, seuilTranche25Minutes, majorationNuit, majorationDimanche, majorationFerie);
            flash.addFlashAttribute("succes", "Paramètres enregistrés.");
        } catch (IllegalStateException e) { flash.addFlashAttribute("erreur", e.getMessage()); }
        return "redirect:/temps-travail/parametres";
    }
}
