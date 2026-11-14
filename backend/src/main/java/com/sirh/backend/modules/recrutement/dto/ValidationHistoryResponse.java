package com.sirh.backend.modules.recrutement.dto;

import java.time.OffsetDateTime;

public record ValidationHistoryResponse(String etape, String decision, String validateur,
        String commentaire, OffsetDateTime decideeAt) { }
