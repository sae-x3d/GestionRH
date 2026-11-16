package com.sirh.backend.modules.discipline.dto;

import java.time.LocalDate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

public record DemandeExplicationRequest(
        @NotBlank @Size(max = 5000) String message,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateLimite) {
}
