package com.sirh.backend.modules.dossier.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CorrectionRequest(
    @NotBlank @Size(max = 40) String champ,
    @NotBlank @Size(max = 5000) String nouvelleValeur,
    @NotBlank @Size(max = 2000) String motif
) { }
