package com.gestion.controller.api;

import com.gestion.dto.CreateDemandeDTO;
import com.gestion.dto.DemandeApiResponse;
import com.gestion.exception.AccesRefuseException;
import com.gestion.model.Demande;
import com.gestion.model.Utilisateur;
import com.gestion.service.DemandeService;
import com.gestion.service.UtilisateurDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/demandes")
@RequiredArgsConstructor
public class DemandeApiController {

    private final DemandeService demandeService;

    @PostMapping
    public ResponseEntity<DemandeApiResponse> creer(@Valid @RequestBody CreateDemandeDTO dto,
                                                    Authentication authentication) {
        Utilisateur demandeur = utilisateurCourant(authentication);
        if (!demandeService.peutCreer(demandeur)) {
            throw new AccesRefuseException("Seul un manager ou un administrateur peut creer une demande.");
        }
        if (dto.getDepartementId() != null
                && !demandeService.departementAutorise(dto.getDepartementId(), demandeur)) {
            throw new AccesRefuseException("Vous ne pouvez creer une demande que pour votre departement.");
        }
        Demande demande = demandeService.creer(dto, demandeur);
        return ResponseEntity.status(HttpStatus.CREATED).body(DemandeApiResponse.depuis(demande));
    }

    @GetMapping
    public ResponseEntity<List<DemandeApiResponse>> lister(Authentication authentication) {
        Utilisateur utilisateur = utilisateurCourant(authentication);
        List<DemandeApiResponse> demandes = demandeService.listerPour(utilisateur).stream()
                .map(DemandeApiResponse::depuis)
                .toList();
        return ResponseEntity.ok(demandes);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DemandeApiResponse> detail(@PathVariable Integer id, Authentication authentication) {
        Utilisateur utilisateur = utilisateurCourant(authentication);
        Demande demande = demandeService.trouverParId(id);
        if (!demandeService.peutConsulter(demande, utilisateur)) {
            throw new AccesRefuseException("Acces interdit a cette demande.");
        }
        return ResponseEntity.ok(DemandeApiResponse.depuis(demande));
    }

    private Utilisateur utilisateurCourant(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof UtilisateurDetails details) {
            return details.getUtilisateur();
        }
        return null;
    }
}
