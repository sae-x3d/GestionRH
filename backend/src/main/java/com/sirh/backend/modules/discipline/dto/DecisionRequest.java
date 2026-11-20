package com.sirh.backend.modules.discipline.dto;

import java.time.LocalDate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

public record DecisionRequest(
        @NotBlank String decision,
        @Size(max = 5000) String motif,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {
}
