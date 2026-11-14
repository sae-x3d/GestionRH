package com.sirh.backend.modules.candidatures.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record CandidatureWorkflowActionRequest(
    String action,
    LocalDateTime entretienAt,
    String typeEntretien,
    String commentaire,
    BigDecimal salaireBase,
    LocalDate dateDebut,
    String typeContrat
) { }
