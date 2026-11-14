package com.sirh.backend.modules.accounts.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegistrationDecisionRequest(
    @NotBlank @Pattern(regexp = "(?i)(APPROVED|REJECTED)") String status,
    @Size(max = 500) String comment
) { }
