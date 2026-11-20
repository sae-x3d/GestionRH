package com.sirh.backend.modules.competences.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.sirh.backend.modules.auth.entity.AuthUserPrincipal;
import com.sirh.backend.modules.competences.service.FormationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FormationController {
    private final FormationService service;

    public FormationController(FormationService service) {
        this.service = service;
    }

    @GetMapping("/api/manager/formations/besoins")
    public List<Map<String, Object>> managerNeeds(@AuthenticationPrincipal AuthUserPrincipal user) {
        return service.managerNeeds(user.getId());
    }

    @GetMapping("/api/rh/formations/besoins")
    public List<Map<String, Object>> rhNeeds(@AuthenticationPrincipal AuthUserPrincipal user) {
        return service.rhNeeds(user.getId());
    }

    @GetMapping("/api/rh/formations")
    public List<Map<String, Object>> formations(@AuthenticationPrincipal AuthUserPrincipal user) {
        return service.formations(user.getId());
    }

    @PostMapping("/api/rh/formations")
    public Map<String, Object> createFormation(@AuthenticationPrincipal AuthUserPrincipal user,
            @Valid @RequestBody FormationRequest request) {
        return service.createFormation(user.getId(), request.competenceId(), request.titre(),
            request.description(), request.organisme(), request.dateDebut(), request.dateFin(),
            request.niveauCible());
    }

    @GetMapping("/api/rh/formations/evolution")
    public List<Map<String, Object>> evolutionRh(@AuthenticationPrincipal AuthUserPrincipal user) {
        return service.evolutionRh(user.getId());
    }

    @GetMapping("/api/rh/formations/{formationId}")
    public Map<String, Object> detail(@AuthenticationPrincipal AuthUserPrincipal user,
            @PathVariable Integer formationId) {
        return service.detail(user.getId(), formationId);
    }

    @PostMapping("/api/rh/formations/{formationId}/participants")
    public Map<String, Object> enroll(@AuthenticationPrincipal AuthUserPrincipal user,
            @PathVariable Integer formationId, @Valid @RequestBody EnrollmentRequest request) {
        return service.enroll(user.getId(), formationId, request.besoinId());
    }

    @PostMapping("/api/rh/formations/{formationId}/mesures")
    public Map<String, Object> measure(@AuthenticationPrincipal AuthUserPrincipal user,
            @PathVariable Integer formationId, @Valid @RequestBody MeasureRequest request) {
        return service.measure(user.getId(), formationId, request.participationId(),
            request.niveauAvant(), request.niveauApres(), request.commentaire());
    }

    @GetMapping("/api/employe/formations")
    public List<Map<String, Object>> myFormations(@AuthenticationPrincipal AuthUserPrincipal user) {
        return service.myFormations(user.getId());
    }

    @GetMapping("/api/employe/formations/evolution")
    public List<Map<String, Object>> evolutionEmploye(@AuthenticationPrincipal AuthUserPrincipal user) {
        return service.evolutionEmploye(user.getId());
    }

    public record FormationRequest(
        @NotNull Integer competenceId,
        @NotBlank @Size(max = 200) String titre,
        @Size(max = 1500) String description,
        @Size(max = 150) String organisme,
        LocalDate dateDebut,
        LocalDate dateFin,
        Integer niveauCible
    ) {
    }

    public record EnrollmentRequest(@NotNull Integer besoinId) {
    }

    public record MeasureRequest(
        @NotNull Integer participationId,
        @NotNull Integer niveauAvant,
        @NotNull Integer niveauApres,
        @Size(max = 1000) String commentaire
    ) {
    }
}
