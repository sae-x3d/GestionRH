package com.sirh.backend.modules.competences.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.sirh.backend.modules.auth.entity.AuthUserPrincipal;
import com.sirh.backend.modules.competences.service.EvaluationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EvaluationController {
    private final EvaluationService service;

    public EvaluationController(EvaluationService service) {
        this.service = service;
    }

    @GetMapping("/api/manager/evaluations")
    public Map<String, Object> managerOverview(@AuthenticationPrincipal AuthUserPrincipal user) {
        return service.managerOverview(user.getId());
    }

    @GetMapping("/api/manager/evaluations/formulaire")
    public Map<String, Object> managerForm(@AuthenticationPrincipal AuthUserPrincipal user,
            @RequestParam Integer employeId) {
        return service.evaluationForm(user.getId(), employeId);
    }

    @PostMapping("/api/manager/evaluations")
    public Map<String, Object> createEvaluation(@AuthenticationPrincipal AuthUserPrincipal user,
            @Valid @RequestBody EvaluationRequest request) {
        return service.createEvaluation(user.getId(), request.employeId(), request.dateEvaluation(),
            request.commentaire(), request.notes());
    }

    @GetMapping("/api/manager/evaluations/{evaluationId}")
    public Map<String, Object> managerDetail(@AuthenticationPrincipal AuthUserPrincipal user,
            @PathVariable Integer evaluationId) {
        return service.detail(user.getId(), evaluationId, "manager");
    }

    @GetMapping("/api/rh/evaluations")
    public List<Map<String, Object>> rhOverview(@AuthenticationPrincipal AuthUserPrincipal user) {
        return service.rhOverview(user.getId());
    }

    @GetMapping("/api/rh/evaluations/{evaluationId}")
    public Map<String, Object> rhDetail(@AuthenticationPrincipal AuthUserPrincipal user,
            @PathVariable Integer evaluationId) {
        return service.detail(user.getId(), evaluationId, "rh");
    }

    @PostMapping("/api/rh/evaluations/{evaluationId}/valider")
    public Map<String, Object> validate(@AuthenticationPrincipal AuthUserPrincipal user,
            @PathVariable Integer evaluationId) {
        return service.validate(user.getId(), evaluationId);
    }

    @GetMapping("/api/employe/evaluations")
    public List<Map<String, Object>> employeeEvaluations(@AuthenticationPrincipal AuthUserPrincipal user) {
        return service.employeeEvaluations(user.getId());
    }

    @GetMapping("/api/employe/evaluations/{evaluationId}")
    public Map<String, Object> employeeDetail(@AuthenticationPrincipal AuthUserPrincipal user,
            @PathVariable Integer evaluationId) {
        return service.detail(user.getId(), evaluationId, "employee");
    }

    public record EvaluationRequest(
        @NotNull Integer employeId,
        @NotNull LocalDate dateEvaluation,
        @Size(max = 1500) String commentaire,
        @NotEmpty Map<Integer, Integer> notes
    ) {
    }
}
