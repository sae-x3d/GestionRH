package com.sirh.backend.modules.offboarding.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CreateExitRequest(
        @NotNull Integer employeeId,
        @NotNull Integer contractId,
        @NotBlank @Size(max = 40) String motifCode,
        @NotNull LocalDate dateNotification,
        @NotNull LocalDate dernierJourTravaille,
        @Size(max = 2000) String commentaire) { }
