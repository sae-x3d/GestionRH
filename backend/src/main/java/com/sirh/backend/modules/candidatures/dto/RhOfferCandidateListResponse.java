package com.sirh.backend.modules.candidatures.dto;

public record RhOfferCandidateListResponse(Integer id, String titre, String poste, String departement,
        Integer effectifRestant, Integer nombreCandidatures) { }
