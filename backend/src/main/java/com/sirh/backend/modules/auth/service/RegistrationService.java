package com.sirh.backend.modules.auth.service;

import java.util.Locale;

import com.sirh.backend.modules.auth.dto.RegistrationRequest;
import com.sirh.backend.modules.auth.dto.RegistrationResponse;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RegistrationService {
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    public RegistrationService(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public RegistrationResponse register(RegistrationRequest request) {
        String username = request.username().trim();
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        Integer duplicates = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM users WHERE LOWER(username) = LOWER(?) OR LOWER(email) = LOWER(?)",
            Integer.class, username, email);
        if (duplicates != null && duplicates > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Cet identifiant ou cette adresse e-mail est déjà utilisé.");
        }

        try {
            Integer userId = jdbcTemplate.queryForObject(
                """
                INSERT INTO users (username, email, full_name, password_hash, administrative_validation_status_id)
                SELECT ?, ?, ?, ?, status.id
                FROM statuts_validation_compte status
                WHERE status.code = 'NON_VERIFIE'
                RETURNING id
                """,
                Integer.class,
                username, email, request.fullName().trim(), passwordEncoder.encode(request.password()));
            if (userId == null) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Le statut d'inscription NON_VERIFIE est introuvable.");
            }
            int roleAssigned = jdbcTemplate.update(
                "INSERT INTO user_roles (user_id, role_id) SELECT ?, id FROM roles WHERE role_name = 'simple_user'",
                userId);
            if (roleAssigned != 1) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Le rôle simple_user est introuvable.");
            }
            return new RegistrationResponse(username, email, "NON_VERIFIE");
        } catch (DuplicateKeyException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Cet identifiant ou cette adresse e-mail est déjà utilisé.");
        }
    }
}
