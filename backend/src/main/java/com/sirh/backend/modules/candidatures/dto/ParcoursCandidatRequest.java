package com.sirh.backend.modules.candidatures.dto;

import java.time.LocalDate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ParcoursCandidatRequest(
    @NotBlank @Size(max = 40) String typeCode,
    @Size(max = 30) String niveauEtudeCode,
    @NotBlank @Size(max = 200) String intitule,
    @NotBlank @Size(max = 200) String organisme,
    @Size(max = 5000) String description,
    @NotNull LocalDate dateDebut,
    LocalDate dateFin,
    @NotNull Boolean enCours
) { }
