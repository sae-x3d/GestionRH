package com.gestion.controller.api;

import com.gestion.model.Demande;
import com.gestion.model.Utilisateur;
import com.gestion.service.DemandeService;
import com.gestion.service.UtilisateurDetails;
import com.gestion.dto.DemandeCreateRequestDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/api/demandes")
public class DemandeApiController {

    private final DemandeService demandeService;

    public DemandeApiController(DemandeService demandeService) {
        this.demandeService = demandeService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> creer(@Valid @RequestBody DemandeCreateRequestDTO request,Authentication authentication) {

        UtilisateurDetails details = (UtilisateurDetails) authentication.getPrincipal();

        Utilisateur demandeur = details.getUtilisateur();

        Demande demande = demandeService.creer(request, demandeur);

        URI location = URI.create("/api/demandes/" + demande.getIdDemande());

        return ResponseEntity.created(location).body(Map.of(
                "idDemande", demande.getIdDemande(),
                "message", "Demande créée avec succès",
                "dateCreation", demande.getDateCreation(),
                "budget", demande.getBudget()
        ));
    }
}
