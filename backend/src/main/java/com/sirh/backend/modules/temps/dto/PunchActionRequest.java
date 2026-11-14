package com.sirh.backend.modules.temps.dto;

import jakarta.validation.constraints.NotBlank;

public record PunchActionRequest(@NotBlank String action, @NotBlank String idempotencyKey) {}
