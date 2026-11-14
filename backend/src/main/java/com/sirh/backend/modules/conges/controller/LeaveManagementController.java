package com.sirh.backend.modules.conges.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.sirh.backend.modules.auth.entity.AuthUserPrincipal;
import com.sirh.backend.modules.conges.service.LeaveManagementService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@RestController
public class LeaveManagementController {
    private final LeaveManagementService service;

    public LeaveManagementController(LeaveManagementService service) { this.service = service; }

    @GetMapping("/api/employe/conges")
    public Map<String, Object> employeeOverview(@AuthenticationPrincipal AuthUserPrincipal user) {
        return service.employeeOverview(user.getId());
    }

    @PostMapping("/api/employe/conges")
    public Map<String, Object> submit(@AuthenticationPrincipal AuthUserPrincipal user,
            @Valid @RequestBody LeaveRequest request) {
        return service.submit(user.getId(), request.typeCongeId(), request.dateDebut(), request.dateFin(), request.motif());
    }

    @PostMapping("/api/employe/conges/{id}/justificatif")
    public Map<String, Object> attachJustificatif(@AuthenticationPrincipal AuthUserPrincipal user,
            @PathVariable Integer id, @RequestParam("file") MultipartFile file) {
        return service.attachJustificatif(user.getId(), id, file);
    }

    @GetMapping("/api/manager/conges/demandes")
    public List<Map<String, Object>> managerQueue(@AuthenticationPrincipal AuthUserPrincipal user) {
        return service.managerQueue(user.getId());
    }

    @PostMapping("/api/manager/conges/demandes/{id}/decision")
    public Map<String, Object> managerDecision(@AuthenticationPrincipal AuthUserPrincipal user,
            @PathVariable Integer id, @Valid @RequestBody LeaveDecision request) {
        return service.managerDecision(user.getId(), id, request.decision(), request.commentaire());
    }

    @GetMapping("/api/rh/conges/demandes")
    public List<Map<String, Object>> rhQueue(@AuthenticationPrincipal AuthUserPrincipal user) {
        return service.rhQueue(user.getId());
    }

    @PostMapping("/api/rh/conges/demandes/{id}/decision")
    public Map<String, Object> rhDecision(@AuthenticationPrincipal AuthUserPrincipal user,
            @PathVariable Integer id, @Valid @RequestBody LeaveDecision request) {
        return service.rhDecision(user.getId(), id, request.decision(), request.commentaire());
    }

    @GetMapping("/api/rh/conges/absences")
    public List<Map<String, Object>> absences(@AuthenticationPrincipal AuthUserPrincipal user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        return service.absenceHistory(user.getId(), debut, fin);
    }

    @GetMapping("/api/rh/conges/types-absence")
    public List<Map<String, Object>> absenceTypes(@AuthenticationPrincipal AuthUserPrincipal user) {
        return service.absenceTypes(user.getId());
    }

    @PostMapping(value = "/api/rh/conges/absences", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> recordAbsence(@AuthenticationPrincipal AuthUserPrincipal user,
            @RequestParam Integer employeId, @RequestParam String typeCode,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
            @RequestParam String motif, @RequestParam(defaultValue = "false") boolean justifiee,
            @RequestParam(required = false) MultipartFile justificatif) {
        return service.recordAbsence(user.getId(), employeId, typeCode, dateDebut, dateFin, motif, justifiee, justificatif);
    }

    @GetMapping("/api/rh/conges/absences/{id}/justificatif")
    public ResponseEntity<byte[]> absenceJustification(@AuthenticationPrincipal AuthUserPrincipal user, @PathVariable Integer id) {
        return download(service.downloadAbsenceJustificatif(user.getId(), id));
    }

    @GetMapping("/api/rh/conges/demandes/{id}/justificatif")
    public ResponseEntity<byte[]> leaveJustification(@AuthenticationPrincipal AuthUserPrincipal user, @PathVariable Integer id) {
        return download(service.downloadLeaveJustificatif(user.getId(), id));
    }

    private ResponseEntity<byte[]> download(LeaveManagementService.LeaveDocument document) {
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(document.mimeType()))
            .header("X-Content-Type-Options", "nosniff").header(HttpHeaders.CACHE_CONTROL, "private, no-store")
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(document.fileName()).build().toString())
            .body(document.bytes());
    }

    public record LeaveRequest(@NotNull Integer typeCongeId,
            @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
            @Size(max = 2000) String motif) { }

    public record LeaveDecision(@NotBlank String decision, @Size(max = 2000) String commentaire) { }

}
