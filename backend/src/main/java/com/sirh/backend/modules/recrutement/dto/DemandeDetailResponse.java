package com.sirh.backend.modules.recrutement.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record DemandeDetailResponse(Integer id, String departement, String demandeur, String poste, String motif,
        Integer effectif, BigDecimal budgetSalaireBase, String justification, LocalDate dateSouhaitee,
        String statut, OffsetDateTime soumiseAt, boolean annulable, boolean actionnable,
        List<ValidationHistoryResponse> historique) { }
