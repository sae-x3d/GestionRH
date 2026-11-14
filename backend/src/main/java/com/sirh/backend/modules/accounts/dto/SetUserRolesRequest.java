package com.sirh.backend.modules.accounts.dto;

import java.util.List;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SetUserRolesRequest(@NotEmpty @Size(max = 10) List<@NotBlank String> roles) { }
