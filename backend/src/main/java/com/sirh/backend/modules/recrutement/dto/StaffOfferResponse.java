package com.sirh.backend.modules.recrutement.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record StaffOfferResponse(
    Integer id,
    String titre,
    String description,
    String competences,
    String poste,
    String departement,
    BigDecimal budgetSalaireBase,
    Integer effectif,
    Integer effectifRecrute,
    Integer effectifRestant,
    Integer nombreCandidatures,
    String statutCode,
    String statut,
    LocalDate datePublication,
    LocalDate dateCloture
) { }
