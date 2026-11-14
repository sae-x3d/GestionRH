package com.sirh.backend.modules.temps.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record BulkPlanningAssignmentRequest(
        @NotEmpty List<Integer> employeIds,
        @NotEmpty List<@NotNull LocalDate> dates,
        @NotNull Integer typeJourId,
        LocalTime heureDebut,
        LocalTime heureFin,
        LocalTime pauseDebut,
        LocalTime pauseFin,
        String observation) {}
