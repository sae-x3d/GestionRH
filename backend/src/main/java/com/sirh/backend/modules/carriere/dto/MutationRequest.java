package com.sirh.backend.modules.carriere.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record MutationRequest(
    @NotNull Integer employeId,
    @NotNull Integer nouveauPosteId,
    @DecimalMin("0.01") BigDecimal nouveauSalaireBase,
    @NotNull LocalDate dateEffet,
    @NotBlank @Size(max = 2000) String motif
) { }
