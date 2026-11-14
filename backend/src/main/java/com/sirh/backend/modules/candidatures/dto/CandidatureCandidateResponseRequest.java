package com.sirh.backend.modules.candidatures.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CandidatureCandidateResponseRequest(
    String action,
    Boolean reglesAcceptees,
    LocalDate dateNaissance,
    LocalDate dateDebut,
    BigDecimal salaireBase,
    String typeContrat,
    String commentaire
) { }
