package com.sirh.backend.modules.competences.controller;

import java.util.List;
import java.util.Map;

import com.sirh.backend.modules.auth.entity.AuthUserPrincipal;
import com.sirh.backend.modules.competences.service.CompetenceConfigurationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CompetenceConfigurationController {
    private final CompetenceConfigurationService service;

    public CompetenceConfigurationController(CompetenceConfigurationService service) {
        this.service = service;
    }

    @GetMapping("/api/rh/competences")
    public List<Map<String, Object>> competences(@AuthenticationPrincipal AuthUserPrincipal user) {
        return service.competences(user.getId());
    }

    @PostMapping("/api/rh/competences")
    public Map<String, Object> createCompetence(@AuthenticationPrincipal AuthUserPrincipal user,
            @Valid @RequestBody CompetenceRequest request) {
        return service.createCompetence(user.getId(), request.code(), request.libelle(), request.description());
    }

    @PatchMapping("/api/rh/competences/{competenceId}/statut")
    public Map<String, Object> setCompetenceActive(@AuthenticationPrincipal AuthUserPrincipal user,
            @PathVariable Integer competenceId, @Valid @RequestBody ActivationRequest request) {
        return service.setCompetenceActive(user.getId(), competenceId, request.actif());
    }

    @GetMapping("/api/rh/criteres-poste")
    public Map<String, Object> configuration(@AuthenticationPrincipal AuthUserPrincipal user,
            @RequestParam(required = false) Integer posteId) {
        return service.configuration(user.getId(), posteId);
    }

    @PostMapping("/api/rh/criteres-poste")
    public Map<String, Object> saveCriterion(@AuthenticationPrincipal AuthUserPrincipal user,
            @Valid @RequestBody CriterionRequest request) {
        return service.saveCriterion(user.getId(), request.posteId(), request.competenceId(),
            request.niveauAttendu(), request.seuilEcart(), request.obligatoire(), request.commentaire());
    }

    @DeleteMapping("/api/rh/criteres-poste/{critereId}")
    public Map<String, Object> deleteCriterion(@AuthenticationPrincipal AuthUserPrincipal user,
            @PathVariable Integer critereId, @RequestParam Integer posteId) {
        return service.deleteCriterion(user.getId(), posteId, critereId);
    }

    @PutMapping("/api/rh/postes/{posteId}/bareme")
    public Map<String, Object> updateBareme(@AuthenticationPrincipal AuthUserPrincipal user,
            @PathVariable Integer posteId, @Valid @RequestBody BaremeRequest request) {
        return service.updateBareme(user.getId(), posteId, request.niveauMin(), request.niveauMax());
    }

    public record CompetenceRequest(
        @NotBlank @Size(max = 40) String code,
        @NotBlank @Size(max = 150) String libelle,
        @Size(max = 1000) String description
    ) {
    }

    public record ActivationRequest(@NotNull Boolean actif) {
    }

    public record CriterionRequest(
        @NotNull Integer posteId,
        @NotNull Integer competenceId,
        @NotNull Integer niveauAttendu,
        @NotNull Integer seuilEcart,
        Boolean obligatoire,
        @Size(max = 1000) String commentaire
    ) {
    }

    public record BaremeRequest(@NotNull Integer niveauMin, @NotNull Integer niveauMax) {
    }
}
