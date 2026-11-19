package com.sirh.backend.modules.discipline.controller;

import java.util.List;
import java.util.Map;

import com.sirh.backend.modules.auth.entity.AuthUserPrincipal;
import com.sirh.backend.modules.discipline.dto.DeclareIncidentRequest;
import com.sirh.backend.modules.discipline.service.DisciplineService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ManagerDisciplineController {
    private final DisciplineService service;

    public ManagerDisciplineController(DisciplineService service) { this.service = service; }

    @PostMapping("/api/manager/discipline/incidents")
    public Map<String, Object> declareIncident(@AuthenticationPrincipal AuthUserPrincipal user,
            @Valid @RequestBody DeclareIncidentRequest request) {
        return service.declareIncident(user.getId(), request, true);
    }

    @GetMapping("/api/manager/discipline/incidents")
    public List<Map<String, Object>> myIncidents(@AuthenticationPrincipal AuthUserPrincipal user) {
        return service.managerIncidents(user.getId());
    }
}
