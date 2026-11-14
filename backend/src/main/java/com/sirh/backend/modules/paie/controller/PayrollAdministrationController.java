package com.sirh.backend.modules.paie.controller;

import com.sirh.backend.modules.auth.entity.AuthUserPrincipal;
import com.sirh.backend.modules.paie.service.PayrollAdministrationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PayrollAdministrationController {
    private final PayrollAdministrationService service;

    public PayrollAdministrationController(PayrollAdministrationService service) {
        this.service = service;
    }

    @GetMapping("/api/administration/paie/parametres")
    public List<Map<String, Object>> parameters(@AuthenticationPrincipal AuthUserPrincipal user) {
        return service.parameters(user.getId());
    }

    @PutMapping("/api/administration/paie/parametres/{id}")
    public List<Map<String, Object>> update(@AuthenticationPrincipal AuthUserPrincipal user,
            @PathVariable Integer id, @Valid @RequestBody UpdatePayrollParameter request) {
        return service.updateParameter(user.getId(), id, request.valeur(), request.dateEffet(), request.motif());
    }

    public record UpdatePayrollParameter(@NotNull @PositiveOrZero BigDecimal valeur,
            @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateEffet,
            @NotBlank String motif) { }
}
