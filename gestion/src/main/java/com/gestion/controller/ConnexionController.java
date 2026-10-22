package com.gestion.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ConnexionController {

    @GetMapping("/connexion")
    public String afficherPageConnexion() {
        return "connexion";
    }
}
