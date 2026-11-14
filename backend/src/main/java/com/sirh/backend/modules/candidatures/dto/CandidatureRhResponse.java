package com.sirh.backend.modules.candidatures.dto;

import java.time.OffsetDateTime;

public record CandidatureRhResponse(Integer id, Integer offreId, String candidat, String email, String photoUrl,
        String titreOffre, String poste, String departement, String statutCode, String statut, OffsetDateTime envoyeeAt) { }
