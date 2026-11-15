package com.sirh.backend.modules.candidatures.controller;

import com.sirh.backend.modules.auth.entity.AuthUserPrincipal;
import com.sirh.backend.modules.candidatures.dto.CandidatureCreatedResponse;
import com.sirh.backend.modules.candidatures.dto.CandidatureOptionsResponse;
import com.sirh.backend.modules.candidatures.dto.CreateCandidatureRequest;
import com.sirh.backend.modules.candidatures.dto.CandidatureCandidateResponseRequest;
import com.sirh.backend.modules.candidatures.service.CandidatureService;
import com.sirh.backend.modules.candidatures.service.CandidatureWorkflowService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/candidatures")
public class CandidatureController {
    private final CandidatureService candidatures;
    private final CandidatureWorkflowService workflow;

    public CandidatureController(CandidatureService candidatures, CandidatureWorkflowService workflow) {
        this.candidatures = candidatures;
        this.workflow = workflow;
    }

    @GetMapping(value = "/me/photo", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> ownProfilePhoto(@AuthenticationPrincipal AuthUserPrincipal principal) {
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG)
            .header("X-Content-Type-Options", "nosniff")
            .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
            .body(candidatures.ownProfilePhoto(principal.getId()));
    }

    @GetMapping("/mes")
    public java.util.List<java.util.Map<String, Object>> mine(@AuthenticationPrincipal AuthUserPrincipal principal) {
        return workflow.candidateApplications(principal.getId());
    }

    @GetMapping("/{id}/suivi")
    public java.util.Map<String, Object> tracking(@AuthenticationPrincipal AuthUserPrincipal principal, @PathVariable Integer id) {
        return workflow.candidateDetail(principal.getId(), id);
    }

    @PostMapping(value = "/{id}/reponse", consumes = MediaType.APPLICATION_JSON_VALUE)
    public java.util.Map<String, Object> respond(@AuthenticationPrincipal AuthUserPrincipal principal, @PathVariable Integer id,
            @RequestBody CandidatureCandidateResponseRequest request) {
        return workflow.respondAsCandidate(principal.getId(), id, request);
    }

    @PostMapping(value = "/{id}/reponse", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public java.util.Map<String, Object> respondWithCin(@AuthenticationPrincipal AuthUserPrincipal principal, @PathVariable Integer id,
            @RequestPart("response") CandidatureCandidateResponseRequest request, @RequestPart(value = "cin", required = false) MultipartFile cin) {
        return workflow.respondAsCandidate(principal.getId(), id, request, cin);
    }

    @GetMapping("/options")
    public CandidatureOptionsResponse options(@AuthenticationPrincipal AuthUserPrincipal principal) {
        return candidatures.options(principal.getId());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CandidatureCreatedResponse submit(@AuthenticationPrincipal AuthUserPrincipal principal,
            @Valid @RequestPart("application") CreateCandidatureRequest request,
            @RequestPart("cv") MultipartFile cv,
            @RequestPart(value = "photo", required = false) MultipartFile photo) {
        return candidatures.submit(principal.getId(), request, cv, photo);
    }
}
