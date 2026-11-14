package com.sirh.backend.modules.temps.controller;

import java.time.LocalDate;
import java.util.Map;
import com.sirh.backend.modules.auth.entity.AuthUserPrincipal;
import com.sirh.backend.modules.temps.dto.BulkPlanningAssignmentRequest;
import com.sirh.backend.modules.temps.dto.PunchActionRequest;
import com.sirh.backend.modules.temps.service.TimeManagementService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TimeManagementController {
    private final TimeManagementService service;

    public TimeManagementController(TimeManagementService service) { this.service = service; }

    @GetMapping("/api/planning/mon")
    public Map<String, Object> myPlanning(@AuthenticationPrincipal AuthUserPrincipal user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate semaine) {
        return service.employeeWeek(user.getId(), semaine);
    }

    @GetMapping("/api/planning/pointage")
    public Map<String, Object> myPunch(@AuthenticationPrincipal AuthUserPrincipal user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.punchDay(user.getId(), date);
    }

    @PostMapping("/api/planning/pointage")
    public Map<String, Object> punch(@AuthenticationPrincipal AuthUserPrincipal user,
            @Valid @RequestBody PunchActionRequest request) {
        return service.recordPunch(user.getId(), request);
    }

    @GetMapping("/api/manager/planning/equipe")
    public Map<String, Object> managerWeek(@AuthenticationPrincipal AuthUserPrincipal user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate semaine) {
        return service.managerWeek(user.getId(), semaine);
    }

    @GetMapping("/api/manager/presence/equipe")
    public Map<String, Object> managerPresence(@AuthenticationPrincipal AuthUserPrincipal user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.presence(user.getId(), "manager", date);
    }

    @PostMapping("/api/manager/planning/affectations")
    public Map<String, Object> assign(@AuthenticationPrincipal AuthUserPrincipal user,
            @Valid @RequestBody BulkPlanningAssignmentRequest request) {
        return service.assignBulk(user.getId(), request);
    }

    @PostMapping("/api/manager/planning/{planningId}/publier")
    public Map<String, Object> publish(@AuthenticationPrincipal AuthUserPrincipal user, @PathVariable Integer planningId) {
        return service.publish(user.getId(), planningId);
    }

    @PostMapping("/api/manager/planning/{planningId}/rouvrir")
    public Map<String, Object> reopen(@AuthenticationPrincipal AuthUserPrincipal user, @PathVariable Integer planningId) {
        return service.reopen(user.getId(), planningId);
    }

    @GetMapping("/api/rh/planning/equipe")
    public Map<String, Object> rhWeek(@AuthenticationPrincipal AuthUserPrincipal user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate semaine) {
        return service.globalWeek(user.getId(), "rh", semaine);
    }

    @GetMapping("/api/rh/presence/equipe")
    public Map<String, Object> rhPresence(@AuthenticationPrincipal AuthUserPrincipal user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.presence(user.getId(), "rh", date);
    }

    @GetMapping("/api/administration/planning/equipe")
    public Map<String, Object> administrationWeek(@AuthenticationPrincipal AuthUserPrincipal user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate semaine) {
        return service.globalWeek(user.getId(), "administration", semaine);
    }

    @GetMapping("/api/administration/presence/equipe")
    public Map<String, Object> administrationPresence(@AuthenticationPrincipal AuthUserPrincipal user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.presence(user.getId(), "administration", date);
    }
}
