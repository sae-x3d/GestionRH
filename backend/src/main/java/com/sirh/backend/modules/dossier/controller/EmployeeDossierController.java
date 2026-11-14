package com.sirh.backend.modules.dossier.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import com.sirh.backend.modules.auth.entity.AuthUserPrincipal;
import com.sirh.backend.modules.dossier.dto.CorrectionDecisionRequest;
import com.sirh.backend.modules.dossier.dto.CorrectionRequest;
import com.sirh.backend.modules.dossier.dto.AdministrativeStatusRequest;
import com.sirh.backend.modules.dossier.service.EmployeeDossierService;
import com.sirh.backend.modules.dossier.service.EmployeeDossierService.Download;
import com.sirh.backend.modules.candidatures.service.CandidatureWorkflowService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class EmployeeDossierController {
    private final EmployeeDossierService dossiers;
    private final CandidatureWorkflowService contracts;
    public EmployeeDossierController(EmployeeDossierService dossiers, CandidatureWorkflowService contracts) {
        this.dossiers = dossiers; this.contracts = contracts;
    }

    @GetMapping("/api/rh/employes")
    public Map<String, Object> rhDirectory(@AuthenticationPrincipal AuthUserPrincipal actor,
            @RequestParam(required = false) String q, @RequestParam(required = false) Integer departementId,
            @RequestParam(required = false) Integer posteId, @RequestParam(required = false) String statut,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "nom") String sortBy, @RequestParam(defaultValue = "asc") String sortDirection) {
        return dossiers.directory(actor.getId(), false, q, departementId, posteId, statut, page, size, sortBy, sortDirection);
    }

    @GetMapping("/api/manager/equipe/effectif")
    public Map<String, Object> managerTeam(@AuthenticationPrincipal AuthUserPrincipal actor,
            @RequestParam(required = false) String q, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size, @RequestParam(defaultValue = "nom") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection) {
        return dossiers.directory(actor.getId(), true, q, null, null, null, page, size, sortBy, sortDirection);
    }

    @GetMapping("/api/dossiers/me")
    public Map<String, Object> ownDossier(@AuthenticationPrincipal AuthUserPrincipal actor) {
        return dossiers.ownDossier(actor.getId());
    }

    @GetMapping("/api/dossiers/employes/{employeeId}")
    public Map<String, Object> dossier(@AuthenticationPrincipal AuthUserPrincipal actor, @PathVariable Integer employeeId) {
        return dossiers.dossier(actor.getId(), employeeId);
    }

    @GetMapping("/api/dossiers/employes/{employeeId}/documents/types")
    public List<Map<String, Object>> documentTypes(@AuthenticationPrincipal AuthUserPrincipal actor, @PathVariable Integer employeeId) {
        return dossiers.documentTypes(actor.getId(), employeeId);
    }

    @GetMapping("/api/dossiers/employes/{employeeId}/documents/{documentId}/versions")
    public List<Map<String, Object>> documentVersions(@AuthenticationPrincipal AuthUserPrincipal actor,
            @PathVariable Integer employeeId, @PathVariable Integer documentId) {
        return dossiers.documentVersions(actor.getId(), employeeId, documentId);
    }

    @PostMapping(value = "/api/dossiers/employes/{employeeId}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> uploadDocument(@AuthenticationPrincipal AuthUserPrincipal actor, @PathVariable Integer employeeId,
            @RequestParam(required = false) Integer typeId, @RequestParam(required = false) Integer documentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateEmission,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateExpiration,
            @RequestParam("file") MultipartFile file) {
        return dossiers.uploadDocument(actor.getId(), employeeId, typeId, documentId, dateEmission, dateExpiration, file);
    }

    @GetMapping("/api/dossiers/employes/{employeeId}/documents/{documentId}/telecharger")
    public ResponseEntity<byte[]> downloadDocument(@AuthenticationPrincipal AuthUserPrincipal actor,
            @PathVariable Integer employeeId, @PathVariable Integer documentId) {
        Download download = dossiers.downloadDocument(actor.getId(), employeeId, documentId);
        MediaType mime = MediaType.parseMediaType(download.mimeType());
        return ResponseEntity.ok().contentType(mime)
            .header("X-Content-Type-Options", "nosniff")
            .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(download.fileName()).build().toString())
            .body(download.bytes());
    }

    @GetMapping("/api/dossiers/employes/{employeeId}/documents/{documentId}/versions/{versionId}/telecharger")
    public ResponseEntity<byte[]> downloadVersion(@AuthenticationPrincipal AuthUserPrincipal actor,
            @PathVariable Integer employeeId, @PathVariable Integer documentId, @PathVariable Integer versionId) {
        Download download = dossiers.downloadDocumentVersion(actor.getId(), employeeId, documentId, versionId);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(download.mimeType()))
            .header("X-Content-Type-Options", "nosniff").header(HttpHeaders.CACHE_CONTROL, "private, no-store")
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(download.fileName()).build().toString())
            .body(download.bytes());
    }

    @PostMapping("/api/rh/dossiers/employes/{employeeId}/documents/{documentId}/archiver")
    public void archiveDocument(@AuthenticationPrincipal AuthUserPrincipal actor, @PathVariable Integer employeeId, @PathVariable Integer documentId) {
        dossiers.archiveDocument(actor.getId(), employeeId, documentId);
    }

    @GetMapping("/api/rh/dossiers/demandes")
    public List<Map<String, Object>> pendingCorrections(@AuthenticationPrincipal AuthUserPrincipal actor) {
        return dossiers.corrections(actor.getId(), true);
    }

    @GetMapping("/api/dossiers/me/demandes")
    public List<Map<String, Object>> ownCorrections(@AuthenticationPrincipal AuthUserPrincipal actor) {
        return dossiers.corrections(actor.getId(), false);
    }

    @PostMapping("/api/dossiers/me/demandes")
    public Map<String, Object> submitCorrection(@AuthenticationPrincipal AuthUserPrincipal actor, @Valid @RequestBody CorrectionRequest request) {
        return dossiers.submitCorrection(actor.getId(), request);
    }

    @PostMapping(value = "/api/dossiers/me/demandes/avec-piece-jointe", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> submitCorrectionWithAttachment(@AuthenticationPrincipal AuthUserPrincipal actor,
            @RequestParam String champ, @RequestParam String nouvelleValeur, @RequestParam String motif,
            @RequestParam(required = false) MultipartFile pieceJointe) {
        return dossiers.submitCorrection(actor.getId(), new CorrectionRequest(champ, nouvelleValeur, motif), pieceJointe);
    }

    @GetMapping("/api/dossiers/me/demandes/{requestId}/piece-jointe")
    public ResponseEntity<byte[]> correctionAttachment(@AuthenticationPrincipal AuthUserPrincipal actor, @PathVariable Integer requestId) {
        Download download = dossiers.downloadCorrectionAttachment(actor.getId(), requestId);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(download.mimeType()))
            .header("X-Content-Type-Options", "nosniff").header(HttpHeaders.CACHE_CONTROL, "private, no-store")
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(download.fileName()).build().toString())
            .body(download.bytes());
    }

    @DeleteMapping("/api/dossiers/me/demandes/{requestId}")
    public Map<String, Object> cancelCorrection(@AuthenticationPrincipal AuthUserPrincipal actor, @PathVariable Integer requestId) {
        return dossiers.cancelCorrection(actor.getId(), requestId);
    }

    @PostMapping("/api/rh/dossiers/demandes/{requestId}/decision")
    public Map<String, Object> decideCorrection(@AuthenticationPrincipal AuthUserPrincipal actor, @PathVariable Integer requestId,
            @Valid @RequestBody CorrectionDecisionRequest request) {
        return dossiers.decideCorrection(actor.getId(), requestId, request.decision(), request.commentaire());
    }

    @PatchMapping("/api/rh/dossiers/employes/{employeeId}/statut")
    public Map<String, Object> changeStatus(@AuthenticationPrincipal AuthUserPrincipal actor, @PathVariable Integer employeeId,
            @Valid @RequestBody AdministrativeStatusRequest request) {
        return dossiers.changeAdministrativeStatus(actor.getId(), employeeId, request.statut(), request.dateEffet(), request.motif());
    }

    @GetMapping("/api/rh/dossiers/alertes")
    public Map<String, Object> administrativeAlerts(@AuthenticationPrincipal AuthUserPrincipal actor) {
        return dossiers.administrativeAlerts(actor.getId());
    }

    @GetMapping(value = "/api/dossiers/employes/{employeeId}/photo", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> photo(@AuthenticationPrincipal AuthUserPrincipal actor, @PathVariable Integer employeeId) {
        byte[] image = dossiers.profilePhoto(actor.getId(), employeeId);
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).header("X-Content-Type-Options", "nosniff")
            .header(HttpHeaders.CACHE_CONTROL, "private, no-store").body(image);
    }

    @GetMapping("/api/dossiers/employes/{employeeId}/cv-recrutement")
    public ResponseEntity<byte[]> recruitmentCv(@AuthenticationPrincipal AuthUserPrincipal actor, @PathVariable Integer employeeId) {
        EmployeeDossierService.Download download = dossiers.recruitmentCv(actor.getId(), employeeId);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
            .header("X-Content-Type-Options", "nosniff").header(HttpHeaders.CACHE_CONTROL, "private, no-store")
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(download.fileName()).build().toString())
            .body(download.bytes());
    }

    @GetMapping(value = "/api/dossiers/employes/{employeeId}/contrats/{contractId}/document", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> contractDocument(@AuthenticationPrincipal AuthUserPrincipal actor,
            @PathVariable Integer employeeId, @PathVariable Integer contractId) {
        byte[] pdf = contracts.employeeContractDocumentForDossier(actor.getId(), employeeId, contractId);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
            .header("X-Content-Type-Options", "nosniff").header(HttpHeaders.CACHE_CONTROL, "private, no-store")
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename("contrat-" + contractId + ".pdf").build().toString())
            .body(pdf);
    }
}
