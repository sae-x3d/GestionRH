package com.sirh.backend.modules.auth.dto;

import java.util.List;

public record AuthSessionResponse(
    Integer userId,
    String username,
    String email,
    List<String> roles,
    String accountValidationStatus,
    List<String> departmentCodes,
    String photoUrl
) { }
