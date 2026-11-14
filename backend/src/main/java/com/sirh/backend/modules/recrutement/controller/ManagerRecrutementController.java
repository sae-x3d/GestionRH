package com.sirh.backend.modules.recrutement.controller;

import java.util.List;

import com.sirh.backend.modules.auth.entity.AuthUserPrincipal;
import com.sirh.backend.modules.recrutement.dto.CreateDemandeRequest;
import com.sirh.backend.modules.recrutement.dto.DemandeSummaryResponse;
import com.sirh.backend.modules.recrutement.dto.DemandeDetailResponse;
import com.sirh.backend.modules.recrutement.dto.ManagerOptionsResponse;
import com.sirh.backend.modules.recrutement.service.ManagerRecrutementService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/manager/recrutement")
public class ManagerRecrutementController {
    private final ManagerRecrutementService service;

    public ManagerRecrutementController(ManagerRecrutementService service) {
        this.service = service;
    }

    @GetMapping("/options")
    public ManagerOptionsResponse options(@AuthenticationPrincipal AuthUserPrincipal manager) {
        return service.getOptions(manager.getId());
    }

    @PostMapping("/demandes")
    public DemandeSummaryResponse submit(@AuthenticationPrincipal AuthUserPrincipal manager,
            @Valid @RequestBody CreateDemandeRequest request) {
        return service.submit(manager.getId(), request);
    }

    @GetMapping("/demandes")
    public List<DemandeSummaryResponse> departmentDemands(@AuthenticationPrincipal AuthUserPrincipal manager) {
        return service.getDepartmentDemands(manager.getId());
    }

    @GetMapping("/demandes/{id}")
    public DemandeDetailResponse departmentDemand(@AuthenticationPrincipal AuthUserPrincipal manager,
            @PathVariable Integer id) {
        return service.getDepartmentDemand(manager.getId(), id);
    }

    @PostMapping("/demandes/{id}/annuler")
    public void cancel(@AuthenticationPrincipal AuthUserPrincipal manager, @PathVariable Integer id) {
        service.cancel(manager.getId(), id);
    }
}
