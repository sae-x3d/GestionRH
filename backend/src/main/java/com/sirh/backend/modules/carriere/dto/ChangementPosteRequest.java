package com.sirh.backend.modules.carriere.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record ChangementPosteRequest(
    @NotNull Integer employeId,
    @NotNull Integer nouveauPosteId,
    @NotNull LocalDate dateEffet,
    @NotBlank @Size(max = 2000) String motif
) { }
