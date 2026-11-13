package com.gestion.controller;

import com.gestion.dto.InscriptionDTO;
import com.gestion.service.InscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class InscriptionController {

    private final InscriptionService inscriptionService;

    @GetMapping("/inscription")
    public String afficherFormulaire(@RequestParam(name = "offreId", required = false) Integer offreId,
                                     Model model) {
        InscriptionDTO form = new InscriptionDTO();
        form.setOffreId(offreId);
        preparerFormulaire(model, form);
        return "inscription";
    }

    @PostMapping("/inscription")
    public String inscrire(@Valid @ModelAttribute("form") InscriptionDTO form,
                           BindingResult errors,
                           Model model) {
        if (!errors.hasFieldErrors("confirmation") && !form.motsDePasseIdentiques()) {
            errors.rejectValue("confirmation", "confirmation",
                    "Les mots de passe ne correspondent pas.");
        }
        if (errors.hasErrors()) {
            preparerFormulaire(model, form);
            return "inscription";
        }

        try {
            inscriptionService.inscrire(form);
        } catch (IllegalStateException ex) {
            errors.reject("inscription", ex.getMessage());
            preparerFormulaire(model, form);
            return "inscription";
        }

        if (form.getOffreId() == null) {
            return "redirect:/connexion?inscription";
        }
        return "redirect:/connexion?inscription&offreId=" + form.getOffreId();
    }

    private void preparerFormulaire(Model model, InscriptionDTO form) {
        model.addAttribute("pageTitle", "Creer un compte");
        model.addAttribute("form", form);
    }
}
