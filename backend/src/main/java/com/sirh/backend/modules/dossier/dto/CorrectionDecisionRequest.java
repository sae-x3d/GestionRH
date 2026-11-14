package com.sirh.backend.modules.dossier.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CorrectionDecisionRequest(
    @NotBlank String decision,
    @Size(max = 2000) String commentaire
) { }
