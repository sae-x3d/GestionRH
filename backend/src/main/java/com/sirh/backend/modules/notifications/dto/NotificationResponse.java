package com.sirh.backend.modules.notifications.dto;

import java.time.OffsetDateTime;

public record NotificationResponse(Integer id, String type, String titre, String resume,
        String lien, boolean lue, OffsetDateTime createdAt) { }
