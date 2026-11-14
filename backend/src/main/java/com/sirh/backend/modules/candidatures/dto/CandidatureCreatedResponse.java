package com.sirh.backend.modules.candidatures.dto;

import java.time.OffsetDateTime;

public record CandidatureCreatedResponse(Integer id, String statut, OffsetDateTime envoyeeAt) { }
