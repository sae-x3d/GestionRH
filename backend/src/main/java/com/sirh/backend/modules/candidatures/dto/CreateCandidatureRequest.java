package com.sirh.backend.modules.candidatures.dto;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateCandidatureRequest(
    @NotNull Integer offreId,
    @NotEmpty @Size(max = 30) List<@Valid ParcoursCandidatRequest> parcours
) { }
