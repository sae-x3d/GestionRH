package com.sirh.backend.modules.candidatures.controller;

import java.util.Map;
import com.sirh.backend.modules.auth.entity.AuthUserPrincipal;
import com.sirh.backend.modules.candidatures.service.CandidatureWorkflowService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/employe/contrat")
public class EmployeeContractController {
    private final CandidatureWorkflowService workflow;

    public EmployeeContractController(CandidatureWorkflowService workflow) { this.workflow = workflow; }

    @GetMapping
    public Map<String, Object> current(@AuthenticationPrincipal AuthUserPrincipal principal) {
        return workflow.employeeContract(principal.getId());
    }

    @GetMapping(value = "/document", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> document(@AuthenticationPrincipal AuthUserPrincipal principal) {
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=contrat.pdf")
            .contentType(MediaType.APPLICATION_PDF)
            .body(workflow.employeeContractDocument(principal.getId()));
    }
}
