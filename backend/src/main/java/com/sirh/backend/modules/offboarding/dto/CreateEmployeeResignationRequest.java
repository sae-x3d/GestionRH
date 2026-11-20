package com.sirh.backend.modules.offboarding.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CreateEmployeeResignationRequest(
        @NotNull Integer contractId,
        @NotNull LocalDate dateNotification,
        @NotNull LocalDate dernierJourTravaille,
        @Size(max = 2000) String commentaire) { }
