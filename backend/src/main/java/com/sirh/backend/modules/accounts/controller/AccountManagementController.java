package com.sirh.backend.modules.accounts.controller;

import java.util.List;
import java.util.Map;
import com.sirh.backend.modules.accounts.dto.CreateManagedAccountRequest;
import com.sirh.backend.modules.accounts.dto.RegistrationDecisionRequest;
import com.sirh.backend.modules.accounts.dto.SetUserRolesRequest;
import com.sirh.backend.modules.accounts.dto.SuspendAccountRequest;
import com.sirh.backend.modules.accounts.dto.SetEmployeePostRequest;
import com.sirh.backend.modules.accounts.service.AccountManagementService;
import com.sirh.backend.modules.auth.entity.AuthUserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/gestionnaire/comptes")
public class AccountManagementController {
    private final AccountManagementService accounts;

    public AccountManagementController(AccountManagementService accounts) { this.accounts = accounts; }

    @GetMapping
    public List<Map<String, Object>> users(@AuthenticationPrincipal AuthUserPrincipal principal) {
        return accounts.users(principal.getId());
    }

    @GetMapping("/employes")
    public List<Map<String, Object>> employees(@AuthenticationPrincipal AuthUserPrincipal principal) {
        return accounts.employees(principal.getId());
    }

    @GetMapping("/inscriptions")
    public List<Map<String, Object>> pending(@AuthenticationPrincipal AuthUserPrincipal principal) {
        return accounts.pendingRegistrations(principal.getId());
    }

    @GetMapping(value = "/{id}/photo", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> verificationPhoto(@AuthenticationPrincipal AuthUserPrincipal principal, @PathVariable Integer id) {
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG)
            .header("X-Content-Type-Options", "nosniff")
            .header("Cache-Control", "private, no-store")
            .body(accounts.verificationPhoto(principal.getId(), id));
    }

    @GetMapping("/options")
    public Map<String, Object> options(@AuthenticationPrincipal AuthUserPrincipal principal) {
        return accounts.options(principal.getId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> create(@AuthenticationPrincipal AuthUserPrincipal principal,
            @Valid @RequestBody CreateManagedAccountRequest request) {
        return accounts.create(principal.getId(), request);
    }

    @PatchMapping("/{id}/suspension")
    public Map<String, Object> suspend(@AuthenticationPrincipal AuthUserPrincipal principal, @PathVariable Integer id,
            @Valid @RequestBody SuspendAccountRequest request) {
        return accounts.setSuspended(principal.getId(), id, request);
    }

    @PutMapping("/{id}/roles")
    public Map<String, Object> roles(@AuthenticationPrincipal AuthUserPrincipal principal, @PathVariable Integer id,
            @Valid @RequestBody SetUserRolesRequest request) {
        return accounts.setRoles(principal.getId(), id, request);
    }

    @PatchMapping("/{id}/affectation")
    public Map<String, Object> assignPost(@AuthenticationPrincipal AuthUserPrincipal principal, @PathVariable Integer id,
            @Valid @RequestBody SetEmployeePostRequest request) {
        return accounts.setEmployeePost(principal.getId(), id, request);
    }

    @PostMapping("/{id}/inscription-decision")
    public Map<String, Object> decide(@AuthenticationPrincipal AuthUserPrincipal principal, @PathVariable Integer id,
            @Valid @RequestBody RegistrationDecisionRequest request) {
        return accounts.decideRegistration(principal.getId(), id, request);
    }
}
