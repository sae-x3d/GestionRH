package com.sirh.backend.modules.recrutement.controller;

import java.util.List;
import com.sirh.backend.modules.auth.entity.AuthUserPrincipal;
import com.sirh.backend.modules.recrutement.dto.CreateOfferRequest;
import com.sirh.backend.modules.recrutement.dto.CreatedOfferResponse;
import com.sirh.backend.modules.recrutement.dto.DecisionDemandeRequest;
import com.sirh.backend.modules.recrutement.dto.DemandeDetailResponse;
import com.sirh.backend.modules.recrutement.dto.DemandeSummaryResponse;
import com.sirh.backend.modules.recrutement.service.RecruitmentWorkflowService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/{area}/recrutement/demandes")
public class RecruitmentWorkflowController {
    private final RecruitmentWorkflowService workflow;

    public RecruitmentWorkflowController(RecruitmentWorkflowService workflow) { this.workflow = workflow; }

    @GetMapping
    public List<DemandeSummaryResponse> pending(@PathVariable String area,
            @AuthenticationPrincipal AuthUserPrincipal user) {
        return workflow.pending(role(area), user.getId());
    }

    @GetMapping("/{id}")
    public DemandeDetailResponse detail(@PathVariable String area, @PathVariable Integer id,
            @AuthenticationPrincipal AuthUserPrincipal user) {
        return workflow.detail(role(area), user.getId(), id);
    }

    @PostMapping("/{id}/decision")
    public DemandeDetailResponse decide(@PathVariable String area, @PathVariable Integer id,
            @AuthenticationPrincipal AuthUserPrincipal user, @Valid @RequestBody DecisionDemandeRequest request) {
        return workflow.decide(role(area), user.getId(), id, request);
    }

    @PostMapping("/{id}/offre")
    public CreatedOfferResponse createOffer(@PathVariable String area, @PathVariable Integer id,
            @AuthenticationPrincipal AuthUserPrincipal user, @Valid @RequestBody CreateOfferRequest request) {
        return workflow.createOffer(role(area), user.getId(), id, request);
    }

    private String role(String area) {
        return switch (area.toLowerCase()) {
            case "rh" -> "RH";
            case "finance" -> "FINANCE";
            case "administration" -> "ADMINISTRATION";
            default -> "INCONNU";
        };
    }
}
