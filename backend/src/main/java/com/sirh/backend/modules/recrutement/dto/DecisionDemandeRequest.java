package com.sirh.backend.modules.recrutement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DecisionDemandeRequest(
    @NotBlank @Pattern(regexp = "APPROUVEE|REFUSEE") String decision,
    @Size(max = 2000) String commentaire
) { }
