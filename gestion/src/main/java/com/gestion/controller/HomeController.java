package com.gestion.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String accueil() {
<<<<<<< HEAD
        return "redirect:/demandes";
=======
        return "redirect:/offres";
>>>>>>> origin/Joh
    }
}
