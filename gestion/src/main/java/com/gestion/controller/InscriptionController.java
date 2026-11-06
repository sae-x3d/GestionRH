package com.gestion.controller;

import com.gestion.dto.InscriptionDTO;
import com.gestion.service.InscriptionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class InscriptionController {

    private final InscriptionService inscriptionService;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;

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
                           HttpServletRequest request,
                           HttpServletResponse response,
                           Model model,
                           RedirectAttributes flash) {
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

        boolean connecte = authentifier(form.getUsername(), form.getPassword(), request, response);
        if (!connecte) {
            flash.addFlashAttribute("succes",
                    "Votre compte a ete cree. Connectez-vous pour continuer.");
            return redirectionConnexion(form.getOffreId());
        }

        flash.addFlashAttribute("succes", "Votre compte a ete cree. Bienvenue !");
        if (form.getOffreId() != null) {
            return "redirect:/candidatures/postuler?offreId=" + form.getOffreId();
        }
        return "redirect:/offres";
    }

    private boolean authentifier(String username, String password,
                                 HttpServletRequest request, HttpServletResponse response) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password));
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            securityContextRepository.saveContext(context, request, response);
            return true;
        } catch (AuthenticationException ex) {
            return false;
        }
    }

    private String redirectionConnexion(Integer offreId) {
        if (offreId == null) {
            return "redirect:/connexion";
        }
        return "redirect:/connexion?offreId=" + offreId;
    }

    private void preparerFormulaire(Model model, InscriptionDTO form) {
        model.addAttribute("pageTitle", "Creer un compte");
        model.addAttribute("form", form);
    }
}
