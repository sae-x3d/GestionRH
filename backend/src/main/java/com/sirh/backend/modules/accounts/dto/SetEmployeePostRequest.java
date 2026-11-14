package com.sirh.backend.modules.accounts.dto;

import jakarta.validation.constraints.NotNull;

public record SetEmployeePostRequest(@NotNull Integer posteId) { }
