package com.sirh.backend.modules.recrutement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CreateOfferRequest(
    @NotBlank @Size(max = 150) String titre,
    @NotBlank @Size(max = 10000) String description,
    @Size(max = 5000) String competences,
    @NotNull LocalDate dateCloture
) { }
