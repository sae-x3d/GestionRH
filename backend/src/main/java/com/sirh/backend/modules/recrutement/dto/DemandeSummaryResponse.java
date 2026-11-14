package com.sirh.backend.modules.recrutement.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record DemandeSummaryResponse(Integer id, String poste, String motif, Integer effectif,
        BigDecimal budgetSalaireBase, LocalDate dateSouhaitee, String statut, OffsetDateTime soumiseAt,
        String departement, String demandeur) { }
