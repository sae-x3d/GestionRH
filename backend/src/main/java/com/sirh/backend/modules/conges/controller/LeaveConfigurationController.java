package com.sirh.backend.modules.conges.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import com.sirh.backend.modules.auth.entity.AuthUserPrincipal;
import com.sirh.backend.modules.conges.service.LeaveConfigurationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rh/conges/parametres")
public class LeaveConfigurationController {
    private final LeaveConfigurationService service;

    public LeaveConfigurationController(LeaveConfigurationService service) { this.service = service; }

    @GetMapping
    public Map<String, Object> configuration(@AuthenticationPrincipal AuthUserPrincipal user) {
        return service.configuration(user.getId());
    }

    @PostMapping("/effectifs/{posteId}")
    public Map<String, Object> setMinimum(@AuthenticationPrincipal AuthUserPrincipal user,
            @PathVariable Integer posteId, @Valid @RequestBody MinimumRequest request) {
        return service.setMinimum(user.getId(), posteId, request.minimumPresence(), request.dateDebut(), request.motif());
    }

    @PostMapping("/acquisition/{typeId}")
    public Map<String, Object> setAccrual(@AuthenticationPrincipal AuthUserPrincipal user,
            @PathVariable Integer typeId, @Valid @RequestBody AccrualRequest request) {
        return service.setAccrual(user.getId(), typeId, request.joursParMois(), request.dateDebut());
    }

    @PostMapping("/types/{typeId}")
    public Map<String, Object> setTypePolicy(@AuthenticationPrincipal AuthUserPrincipal user,
            @PathVariable Integer typeId, @Valid @RequestBody TypePolicyRequest request) {
        return service.setTypePolicy(user.getId(), typeId, request.actif(), request.justificatifRequis());
    }

    public record MinimumRequest(@NotNull @Min(0) Integer minimumPresence,
            @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @Size(min = 1, max = 500) String motif) { }

    public record AccrualRequest(@NotNull @DecimalMin("0.000") BigDecimal joursParMois,
            @NotNull LocalDate dateDebut) { }
    public record TypePolicyRequest(@NotNull Boolean actif, @NotNull Boolean justificatifRequis) { }
}
