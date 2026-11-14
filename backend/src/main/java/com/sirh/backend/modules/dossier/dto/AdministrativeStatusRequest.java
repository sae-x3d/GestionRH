package com.sirh.backend.modules.dossier.dto;

import java.time.LocalDate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdministrativeStatusRequest(
    @NotBlank String statut,
    @NotNull LocalDate dateEffet,
    @NotBlank @Size(max = 2000) String motif
) { }
