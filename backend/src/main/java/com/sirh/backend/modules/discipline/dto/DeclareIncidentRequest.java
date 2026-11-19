package com.sirh.backend.modules.discipline.dto;

import java.time.LocalDate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

public record DeclareIncidentRequest(
        @NotNull Integer employeId,
        @NotBlank @Size(max = 30) String graviteCode,
        @NotBlank @Size(max = 200) String titre,
        @NotBlank @Size(max = 5000) String description,
        @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFaits) {
}
