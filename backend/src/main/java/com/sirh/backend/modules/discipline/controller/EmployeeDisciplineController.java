package com.sirh.backend.modules.discipline.controller;

import java.util.List;
import java.util.Map;

import com.sirh.backend.modules.auth.entity.AuthUserPrincipal;
import com.sirh.backend.modules.discipline.service.DisciplineService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class EmployeeDisciplineController {
    private final DisciplineService service;

    public EmployeeDisciplineController(DisciplineService service) { this.service = service; }

    @GetMapping("/api/employe/discipline")
    public List<Map<String, Object>> myProcedures(@AuthenticationPrincipal AuthUserPrincipal user) {
        return service.employeeProcedures(user.getId());
    }

    @PostMapping("/api/employe/discipline/demandes/{id}/reponse")
    public Map<String, Object> respond(@AuthenticationPrincipal AuthUserPrincipal user,
            @PathVariable Integer id, @RequestParam String texte,
            @RequestParam(value = "pieces", required = false) List<MultipartFile> pieces) {
        return service.respondToExplanation(user.getId(), id, texte, pieces);
    }

    @GetMapping("/api/employe/discipline/pieces/{pieceId}")
    public ResponseEntity<byte[]> piece(@AuthenticationPrincipal AuthUserPrincipal user,
            @PathVariable Integer pieceId) {
        return download(service.downloadPiece(user.getId(), pieceId, false));
    }

    private ResponseEntity<byte[]> download(DisciplineService.Document document) {
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(document.mimeType()))
            .header("X-Content-Type-Options", "nosniff").header(HttpHeaders.CACHE_CONTROL, "private, no-store")
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(document.fileName()).build().toString())
            .body(document.bytes());
    }
}
