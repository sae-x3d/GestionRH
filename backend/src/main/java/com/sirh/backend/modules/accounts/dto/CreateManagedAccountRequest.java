package com.sirh.backend.modules.accounts.dto;

import java.time.LocalDate;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateManagedAccountRequest(
    @NotBlank @Pattern(regexp = "[A-Za-z0-9._-]{3,50}") String username,
    @NotBlank @Email @Size(max = 254) String email,
    @NotBlank @Size(max = 150) String fullName,
    @NotBlank @Size(min = 12, max = 72) String password,
    @NotBlank @Pattern(regexp = "(?i)(simple_user|employe|manager|rh|administration|gestionnaire)") String role,
    Integer posteId,
    LocalDate dateEmbauche,
    LocalDate dateNaissance
) { }
