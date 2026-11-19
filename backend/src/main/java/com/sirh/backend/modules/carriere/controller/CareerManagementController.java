package com.sirh.backend.modules.carriere.controller;

import com.sirh.backend.modules.auth.entity.AuthUserPrincipal;
import com.sirh.backend.modules.carriere.dto.ChangementPosteRequest;
import com.sirh.backend.modules.carriere.dto.ChangementSalaireRequest;
import com.sirh.backend.modules.carriere.dto.MutationRequest;
import com.sirh.backend.modules.carriere.dto.PromotionRequest;
import com.sirh.backend.modules.carriere.service.CareerManagementService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CareerManagementController {
    private final CareerManagementService careers;

    public CareerManagementController(CareerManagementService careers) {
        this.careers = careers;
    }

    @GetMapping("/api/rh/carriere/employes")
    public List<Map<String, Object>> rhDirectory(@AuthenticationPrincipal AuthUserPrincipal actor,
            @RequestParam(required = false) String q) {
        return careers.rhDirectory(actor.getId(), q);
    }

    @GetMapping("/api/rh/carriere/employes/{employeeId}")
    public Map<String, Object> rhCareer(@AuthenticationPrincipal AuthUserPrincipal actor, @PathVariable Integer employeeId) {
        return careers.rhCareer(actor.getId(), employeeId);
    }

    @GetMapping("/api/rh/carriere/employes/{employeeId}/situation")
    public Map<String, Object> rhSituation(@AuthenticationPrincipal AuthUserPrincipal actor,
            @PathVariable Integer employeeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return careers.rhSituation(actor.getId(), employeeId, date);
    }

    @PostMapping("/api/rh/carriere/promotions")
    public Map<String, Object> promotion(@AuthenticationPrincipal AuthUserPrincipal actor,
            @Valid @RequestBody PromotionRequest request) {
        return careers.promotion(actor.getId(), request);
    }

    @PostMapping("/api/rh/carriere/mutations")
    public Map<String, Object> mutation(@AuthenticationPrincipal AuthUserPrincipal actor,
            @Valid @RequestBody MutationRequest request) {
        return careers.mutation(actor.getId(), request);
    }

    @PostMapping("/api/rh/carriere/changements-poste")
    public Map<String, Object> changementPoste(@AuthenticationPrincipal AuthUserPrincipal actor,
            @Valid @RequestBody ChangementPosteRequest request) {
        return careers.changementPoste(actor.getId(), request);
    }

    @PostMapping("/api/rh/carriere/changements-salaire")
    public Map<String, Object> changementSalaire(@AuthenticationPrincipal AuthUserPrincipal actor,
            @Valid @RequestBody ChangementSalaireRequest request) {
        return careers.changementSalaire(actor.getId(), request);
    }

    @GetMapping("/api/manager/carriere/equipe")
    public List<Map<String, Object>> managerTeam(@AuthenticationPrincipal AuthUserPrincipal actor,
            @RequestParam(required = false) String q) {
        return careers.managerTeam(actor.getId(), q);
    }

    @GetMapping("/api/manager/carriere/equipe/{employeeId}")
    public Map<String, Object> managerCareer(@AuthenticationPrincipal AuthUserPrincipal actor, @PathVariable Integer employeeId) {
        return careers.managerCareer(actor.getId(), employeeId);
    }

    @GetMapping("/api/manager/carriere/equipe/{employeeId}/situation")
    public Map<String, Object> managerSituation(@AuthenticationPrincipal AuthUserPrincipal actor,
            @PathVariable Integer employeeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return careers.managerSituation(actor.getId(), employeeId, date);
    }

    @GetMapping("/api/carriere/me")
    public Map<String, Object> ownCareer(@AuthenticationPrincipal AuthUserPrincipal actor) {
        return careers.ownCareer(actor.getId());
    }

    @GetMapping("/api/carriere/me/situation")
    public Map<String, Object> ownSituation(@AuthenticationPrincipal AuthUserPrincipal actor,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return careers.ownSituation(actor.getId(), date);
    }
}
