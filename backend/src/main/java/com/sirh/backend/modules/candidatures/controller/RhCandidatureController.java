package com.sirh.backend.modules.candidatures.controller;

import java.util.List;
import java.util.Map;
import com.sirh.backend.modules.auth.entity.AuthUserPrincipal;
import com.sirh.backend.modules.candidatures.dto.CandidatureWorkflowActionRequest;
import com.sirh.backend.modules.candidatures.dto.RhOfferCandidateListResponse;
import com.sirh.backend.modules.candidatures.dto.CandidatureRhResponse;
import com.sirh.backend.modules.candidatures.service.CandidatureService;
import com.sirh.backend.modules.candidatures.service.CandidatureWorkflowService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rh/candidatures")
public class RhCandidatureController {
    private final CandidatureService candidatures;
    private final CandidatureWorkflowService workflow;

    public RhCandidatureController(CandidatureService candidatures, CandidatureWorkflowService workflow) {
        this.candidatures = candidatures;
        this.workflow = workflow;
    }

    @GetMapping("/offres")
    public List<RhOfferCandidateListResponse> offers(@AuthenticationPrincipal AuthUserPrincipal principal) {
        return workflow.offersForRh(principal.getId());
    }

    @GetMapping
    public List<CandidatureRhResponse> received(@AuthenticationPrincipal AuthUserPrincipal principal) {
        return candidatures.receivedByRh(principal.getId());
    }

    @GetMapping("/{id}")
    public Map<String, Object> detail(@AuthenticationPrincipal AuthUserPrincipal principal, @PathVariable Integer id) {
        return workflow.rhDetail(principal.getId(), id);
    }

    @PostMapping("/{id}/actions")
    public Map<String, Object> action(@AuthenticationPrincipal AuthUserPrincipal principal, @PathVariable Integer id,
            @RequestBody CandidatureWorkflowActionRequest request) {
        return workflow.actAsRh(principal.getId(), id, request);
    }

    @GetMapping("/{id}/cv")
    public ResponseEntity<byte[]> downloadCv(@AuthenticationPrincipal AuthUserPrincipal principal, @PathVariable Integer id) {
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                .filename("cv-candidature-" + id + ".pdf").build().toString())
            .header("X-Content-Type-Options", "nosniff")
            .header(HttpHeaders.CACHE_CONTROL, "no-store")
            .body(candidatures.cvForRh(principal.getId(), id));
    }

    @GetMapping(value = "/{id}/photo", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> photo(@AuthenticationPrincipal AuthUserPrincipal principal, @PathVariable Integer id) {
        return ResponseEntity.ok()
            .contentType(MediaType.IMAGE_PNG)
            .header("X-Content-Type-Options", "nosniff")
            .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
            .body(candidatures.photoForRh(principal.getId(), id));
    }
}
