package com.sirh.backend.modules.recrutement.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;

public record CreateDemandeRequest(
    @NotNull Integer posteId,
    @NotNull Integer motifId,
    @NotNull @Min(1) Integer effectif,
    @NotNull @DecimalMin("0.01") BigDecimal budgetSalaireBase,
    @NotBlank @Size(max = 5000) String justification,
    @NotNull LocalDate dateSouhaitee
) { }
