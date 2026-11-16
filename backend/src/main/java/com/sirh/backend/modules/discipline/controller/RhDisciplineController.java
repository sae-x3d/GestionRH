package com.sirh.backend.modules.discipline.controller;

import java.util.List;
import java.util.Map;

import com.sirh.backend.modules.auth.entity.AuthUserPrincipal;
import com.sirh.backend.modules.discipline.dto.DecisionRequest;
import com.sirh.backend.modules.discipline.dto.DeclareIncidentRequest;
import com.sirh.backend.modules.discipline.dto.DemandeExplicationRequest;
import com.sirh.backend.modules.discipline.service.DisciplineService;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RhDisciplineController {
    private final DisciplineService service;

    public RhDisciplineController(DisciplineService service) { this.service = service; }

    @PostMapping("/api/rh/discipline/incidents")
    public Map<String, Object> declareIncident(@AuthenticationPrincipal AuthUserPrincipal user,
            @Valid @RequestBody DeclareIncidentRequest request) {
        return service.declareIncident(user.getId(), request, false);
    }

    @GetMapping("/api/rh/discipline/incidents")
    public List<Map<String, Object>> incidents(@AuthenticationPrincipal AuthUserPrincipal user,
            @RequestParam(defaultValue = "false") boolean sansProcedure) {
        return service.rhIncidents(user.getId(), sansProcedure);
    }

    @PostMapping("/api/rh/discipline/incidents/{id}/procedure")
    public Map<String, Object> openProcedure(@AuthenticationPrincipal AuthUserPrincipal user,
            @PathVariable Integer id) {
        return service.openProcedure(user.getId(), id);
    }

    @PostMapping("/api/rh/discipline/procedures/{id}/demande-explication")
    public Map<String, Object> requestExplanation(@AuthenticationPrincipal AuthUserPrincipal user,
            @PathVariable Integer id, @Valid @RequestBody DemandeExplicationRequest request) {
        return service.requestExplanation(user.getId(), id, request);
    }

    @PostMapping("/api/rh/discipline/procedures/{id}/decision")
    public Map<String, Object> decide(@AuthenticationPrincipal AuthUserPrincipal user,
            @PathVariable Integer id, @Valid @RequestBody DecisionRequest request) {
        return service.decide(user.getId(), id, request);
    }

    @GetMapping("/api/rh/discipline/procedures")
    public List<Map<String, Object>> procedures(@AuthenticationPrincipal AuthUserPrincipal user,
            @RequestParam(required = false) String statut) {
        return service.listProcedures(user.getId(), statut);
    }

    @GetMapping("/api/rh/discipline/procedures/{id}")
    public Map<String, Object> procedureDetail(@AuthenticationPrincipal AuthUserPrincipal user,
            @PathVariable Integer id) {
        return service.procedureDetail(user.getId(), id);
    }

    @GetMapping("/api/rh/discipline/pieces/{pieceId}")
    public ResponseEntity<byte[]> piece(@AuthenticationPrincipal AuthUserPrincipal user,
            @PathVariable Integer pieceId) {
        return download(service.downloadPiece(user.getId(), pieceId, true));
    }

    private ResponseEntity<byte[]> download(DisciplineService.Document document) {
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(document.mimeType()))
            .header("X-Content-Type-Options", "nosniff").header(HttpHeaders.CACHE_CONTROL, "private, no-store")
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(document.fileName()).build().toString())
            .body(document.bytes());
    }
}
