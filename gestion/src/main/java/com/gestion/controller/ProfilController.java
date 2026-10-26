package com.gestion.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ProfilController {

    @GetMapping("/profil")
    public String afficher(Model model) {
        model.addAttribute("pageTitle", "Mon profil");
        return "profil";
    }
}
