package com.sirh.backend.modules.offboarding.controller;

import com.sirh.backend.modules.auth.entity.AuthUserPrincipal;
import com.sirh.backend.modules.offboarding.dto.CreateExitRequest;
import com.sirh.backend.modules.offboarding.service.ExitManagementService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.time.LocalDate;
import java.math.BigDecimal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ExitManagementController {
    private final ExitManagementService service;
    public ExitManagementController(ExitManagementService service) { this.service = service; }

    @GetMapping("/api/rh/sorties")
    public List<Map<String,Object>> list(@AuthenticationPrincipal AuthUserPrincipal actor) {
        return service.list(actor.getId(), false);
    }

    @GetMapping("/api/manager/sorties")
    public List<Map<String,Object>> managerList(@AuthenticationPrincipal AuthUserPrincipal actor) {
        return service.list(actor.getId(), true);
    }

    @GetMapping("/api/manager/sorties/options")
    public List<Map<String,Object>> options(@AuthenticationPrincipal AuthUserPrincipal actor) {
        return service.options(actor.getId());
    }

    @PostMapping("/api/manager/sorties")
    public Map<String,Object> request(@AuthenticationPrincipal AuthUserPrincipal actor,
            @Valid @RequestBody CreateExitRequest request) {
        return service.create(actor.getId(), request);
    }

    @PostMapping("/api/rh/sorties/{id}/preparer-stc")
    public Map<String,Object> prepare(@AuthenticationPrincipal AuthUserPrincipal actor, @PathVariable Integer id) {
        return service.prepareSettlement(actor.getId(), id);
    }

    @PostMapping("/api/rh/sorties/{id}/valider")
    public Map<String,Object> validate(@AuthenticationPrincipal AuthUserPrincipal actor, @PathVariable Integer id,
            @RequestBody Map<String,String> request) {
        BigDecimal net;
        try { net = new BigDecimal(request.getOrDefault("montantNet", "")); }
        catch (RuntimeException ex) { throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Le montant net validé doit être un nombre valide."); }
        return service.validate(actor.getId(), id, net, request.get("justificationNet"));
    }

    @PostMapping("/api/rh/sorties/{id}/paiement")
    public Map<String,Object> payment(@AuthenticationPrincipal AuthUserPrincipal actor, @PathVariable Integer id,
            @RequestBody Map<String,String> request) {
        String date = request.get("date");
        if (date == null || date.isBlank()) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "La date du paiement est obligatoire.");
        LocalDate paidAt;
        try { paidAt = LocalDate.parse(date); }
        catch (java.time.format.DateTimeParseException exception) { throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "La date du paiement est invalide."); }
        BigDecimal amount;
        try { amount = new BigDecimal(request.getOrDefault("montant", "")); }
        catch (RuntimeException ex) { throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Le montant payé doit être un nombre valide."); }
        return service.confirmPayment(actor.getId(), id, request.get("reference"), paidAt, amount);
    }

    @PostMapping("/api/rh/sorties/{id}/generer-documents")
    public Map<String,Object> generateDocuments(@AuthenticationPrincipal AuthUserPrincipal actor, @PathVariable Integer id) {
        return service.generateExitDocuments(actor.getId(), id);
    }

    @PostMapping("/api/rh/sorties/{id}/documents-remis")
    public Map<String,Object> documentsRemitted(@AuthenticationPrincipal AuthUserPrincipal actor, @PathVariable Integer id) {
        return service.markDocumentsRemitted(actor.getId(), id);
    }

    @PostMapping("/api/rh/sorties/{id}/finaliser")
    public Map<String,Object> finalizeExit(@AuthenticationPrincipal AuthUserPrincipal actor, @PathVariable Integer id) {
        return service.finalizeExit(actor.getId(), id);
    }

    @PostMapping("/api/rh/sorties/{id}/annuler")
    public Map<String,Object> cancel(@AuthenticationPrincipal AuthUserPrincipal actor, @PathVariable Integer id,
            @RequestBody Map<String,String> request) {
        return service.cancel(actor.getId(), id, request.get("motif"));
    }
}
