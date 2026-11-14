package com.sirh.backend.modules.recrutement.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PublicOfferResponse(
    Integer id,
    String titre,
    String description,
    String competences,
    String poste,
    String departement,
    BigDecimal budgetSalaireBase,
    Integer effectif,
    Integer effectifRestant,
    LocalDate datePublication,
    LocalDate dateCloture
) { }
