package com.gestion.controller;

import com.gestion.exception.RessourceIntrouvableException;
import com.gestion.service.QuestionContratService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/contrats/questionnaire")
@PreAuthorize("hasAnyAuthority('ROLE_ADMINISTRATEUR', 'ROLE_RH')")
@RequiredArgsConstructor
public class QuestionContratController {

    private final QuestionContratService questionContratService;

    @GetMapping
    public String liste(Model model) {
        model.addAttribute("pageTitle", "Questionnaire du contrat");
        model.addAttribute("questions", questionContratService.listerToutes());
        return "contrat/questionnaire";
    }

    @PostMapping
    public String ajouter(@RequestParam("libelle") String libelle,
                          @RequestParam(name = "ordre", required = false) Integer ordre,
                          RedirectAttributes redirectAttributes) {
        try {
            questionContratService.ajouter(libelle, ordre);
            redirectAttributes.addFlashAttribute("succes", "Question ajoutee.");
        } catch (RessourceIntrouvableException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("erreur", ex.getMessage());
        }
        return "redirect:/contrats/questionnaire";
    }

    @PostMapping("/{id}/modifier")
    public String modifier(@PathVariable Integer id,
                           @RequestParam("libelle") String libelle,
                           @RequestParam(name = "ordre", required = false) Integer ordre,
                           @RequestParam(name = "actif", defaultValue = "false") boolean actif,
                           RedirectAttributes redirectAttributes) {
        try {
            questionContratService.modifier(id, libelle, ordre, actif);
            redirectAttributes.addFlashAttribute("succes", "Question mise a jour.");
        } catch (RessourceIntrouvableException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("erreur", ex.getMessage());
        }
        return "redirect:/contrats/questionnaire";
    }

    @PostMapping("/{id}/desactiver")
    public String desactiver(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            questionContratService.desactiver(id);
            redirectAttributes.addFlashAttribute("succes", "Question desactivee.");
        } catch (RessourceIntrouvableException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("erreur", ex.getMessage());
        }
        return "redirect:/contrats/questionnaire";
    }

    @PostMapping("/{id}/reactiver")
    public String reactiver(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            questionContratService.reactiver(id);
            redirectAttributes.addFlashAttribute("succes", "Question reactivee.");
        } catch (RessourceIntrouvableException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("erreur", ex.getMessage());
        }
        return "redirect:/contrats/questionnaire";
    }
}
