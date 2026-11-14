package com.sirh.backend.modules.candidatures.dto;

public record RhOfferCandidateListResponse(Integer id, String titre, String poste, String departement,
        Integer effectif, Integer effectifRecrute, Integer effectifRestant, Integer nombreCandidatures,
        String statutCode, String statut, java.time.LocalDate dateCloture) { }
