package com.sirh.backend.modules.paie.controller;

import com.sirh.backend.modules.auth.entity.AuthUserPrincipal;
import com.sirh.backend.modules.paie.service.PayrollProcessingService;
import com.sirh.backend.modules.paie.service.PayrollPayslipService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@RestController
public class PayrollProcessingController {
    private final PayrollProcessingService service;
    private final PayrollPayslipService payslipService;

    public PayrollProcessingController(PayrollProcessingService service, PayrollPayslipService payslipService) { this.service = service; this.payslipService=payslipService; }

    @GetMapping("/api/rh/paie/periodes")
    public List<Map<String,Object>> periods(@AuthenticationPrincipal AuthUserPrincipal user) {
        return service.periods(user.getId());
    }

    @PostMapping("/api/rh/paie/periodes")
    public Map<String,Object> create(@AuthenticationPrincipal AuthUserPrincipal user,
            @Valid @RequestBody CreatePayrollPeriod request) {
        return service.createPeriod(user.getId(), request.dateDebut(), request.dateFin());
    }

    @PostMapping("/api/rh/paie/periodes/{id}/calculer")
    public Map<String,Object> calculate(@AuthenticationPrincipal AuthUserPrincipal user,@PathVariable Integer id) {
        return service.calculate(user.getId(),id);
    }

    @PostMapping("/api/rh/paie/periodes/{id}/valider")
    public Map<String,Object> validate(@AuthenticationPrincipal AuthUserPrincipal user,@PathVariable Integer id) {
        return service.validate(user.getId(),id);
    }

    @PostMapping("/api/rh/paie/periodes/{id}/payer")
    public Map<String,Object> paid(@AuthenticationPrincipal AuthUserPrincipal user,@PathVariable Integer id,
            @Valid @RequestBody MarkPayrollPaid request) {
        return service.markPaid(user.getId(),id,request.reference());
    }

    @PostMapping("/api/rh/paie/periodes/{id}/cloturer")
    public Map<String,Object> close(@AuthenticationPrincipal AuthUserPrincipal user,@PathVariable Integer id) {
        return service.close(user.getId(),id);
    }

    @GetMapping("/api/rh/paie/periodes/{id}/bulletins")
    public List<Map<String,Object>> bulletins(@AuthenticationPrincipal AuthUserPrincipal user,@PathVariable Integer id) {
        return service.periodBulletins(user.getId(),id);
    }

    @GetMapping("/api/rh/paie/periodes/{id}/etat-excel")
    public ResponseEntity<byte[]> payrollExcel(@AuthenticationPrincipal AuthUserPrincipal user,@PathVariable Integer id) {
        byte[] file=payslipService.rhMonthlyPayrollExcel(user.getId(),id);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\"etat-de-paie-"+id+".xlsx\"")
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")).body(file);
    }

    @GetMapping("/api/rh/paie/periodes/{id}/variables")
    public Map<String,Object> variables(@AuthenticationPrincipal AuthUserPrincipal user,@PathVariable Integer id) {
        return service.variableOptions(user.getId(),id);
    }

    @PostMapping("/api/rh/paie/periodes/{id}/variables")
    public Map<String,Object> createVariable(@AuthenticationPrincipal AuthUserPrincipal user,@PathVariable Integer id,
            @Valid @RequestBody CreatePayrollVariable request) {
        return service.createVariable(user.getId(),id,request.employeId(),request.rubriqueId(),request.montant(),request.justification());
    }

    @PostMapping("/api/rh/paie/periodes/{periodId}/variables/{variableId}/decision")
    public Map<String,Object> decideVariable(@AuthenticationPrincipal AuthUserPrincipal user,@PathVariable Integer periodId,
            @PathVariable Integer variableId,@Valid @RequestBody DecidePayrollVariable request) {
        return service.decideVariable(user.getId(),periodId,variableId,request.approuver(),request.motif());
    }

    @GetMapping("/api/rh/paie/periodes/{id}/avances")
    public Map<String,Object> advances(@AuthenticationPrincipal AuthUserPrincipal user,@PathVariable Integer id) {
        return service.advanceOptions(user.getId(),id);
    }

    @PostMapping("/api/rh/paie/periodes/{id}/avances")
    public Map<String,Object> createAdvance(@AuthenticationPrincipal AuthUserPrincipal user,@PathVariable Integer id,
            @Valid @RequestBody CreatePayrollAdvance request) {
        return service.createAdvance(user.getId(),id,request.employeId(),request.dateAvance(),request.montant(),request.motif());
    }

    @PostMapping("/api/rh/paie/periodes/{periodId}/avances/{advanceId}/decision")
    public Map<String,Object> decideAdvance(@AuthenticationPrincipal AuthUserPrincipal user,@PathVariable Integer periodId,
            @PathVariable Integer advanceId,@Valid @RequestBody DecidePayrollVariable request) {
        return service.decideAdvance(user.getId(),periodId,advanceId,request.approuver(),request.motif());
    }

    public record CreatePayrollPeriod(
        @NotNull @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate dateDebut,
        @NotNull @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate dateFin) { }
    public record CreatePayrollVariable(@NotNull Integer employeId,@NotNull Integer rubriqueId,
        @NotNull @Positive BigDecimal montant,@NotBlank String justification) { }
    public record DecidePayrollVariable(@NotNull Boolean approuver,@NotBlank String motif) { }
    public record CreatePayrollAdvance(@NotNull Integer employeId,
        @NotNull @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate dateAvance,
        @NotNull @Positive BigDecimal montant,@NotBlank String motif) { }
    public record MarkPayrollPaid(@NotBlank String reference) { }
}
