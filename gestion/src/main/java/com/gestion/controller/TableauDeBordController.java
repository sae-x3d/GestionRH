package com.gestion.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class TableauDeBordController {

    @GetMapping("/tableau-de-bord")
    public String afficher(Model model) {
        model.addAttribute("pageTitle", "Tableau de bord");
        return "tableau-de-bord";
    }
}
