package com.sirh.backend.modules.auth.repository;

import java.util.List;

import com.sirh.backend.modules.auth.entity.AuthUserPrincipal;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Repository;

@Repository
public class AuthUserRepository {
    private final JdbcTemplate jdbcTemplate;

    public AuthUserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public AuthUserPrincipal findByIdentifier(String email) {
        var users = jdbcTemplate.query(
            """
            SELECT u.id, u.username, u.email, u.password_hash, u.is_suspended, s.code AS account_status
            FROM users u
            JOIN statuts_validation_compte s ON s.id = u.administrative_validation_status_id
            WHERE LOWER(u.email) = LOWER(?) OR LOWER(u.username) = LOWER(?)
            """,
            (result, rowNum) -> new UserRow(
                result.getInt("id"), result.getString("username"), result.getString("email"),
                result.getString("password_hash"), result.getBoolean("is_suspended"),
                result.getString("account_status")),
            email, email);

        if (users.isEmpty()) {
            throw new UsernameNotFoundException("User not found");
        }

        var user = users.getFirst();
        List<String> roles = jdbcTemplate.queryForList(
            "SELECT r.role_name FROM roles r JOIN user_roles ur ON ur.role_id = r.id WHERE ur.user_id = ? ORDER BY r.id",
            String.class,
            user.id());

        List<String> departmentCodes = jdbcTemplate.queryForList("""
            SELECT DISTINCT UPPER(d.code)
            FROM employes e
            JOIN postes p ON p.id = e.poste_id AND p.actif = TRUE
            JOIN departements d ON d.id = p.departement_id AND d.actif = TRUE
            WHERE e.user_id = ?
            ORDER BY UPPER(d.code)
            """, String.class, user.id());

        return new AuthUserPrincipal(user.id(), user.username(), user.email(), user.passwordHash(),
            user.suspended(), user.accountStatus(), roles, departmentCodes);
    }

    public String findUsernameById(Integer userId) {
        return jdbcTemplate.queryForObject(
            "SELECT COALESCE(full_name, username) FROM users WHERE id = ?", String.class, userId);
    }

    public AuthUserPrincipal findById(Integer userId) {
        String email = jdbcTemplate.queryForObject("SELECT email FROM users WHERE id = ?", String.class, userId);
        return findByIdentifier(email);
    }

    public String findPhotoPathById(Integer userId) {
        return jdbcTemplate.query("SELECT photo_profil_path FROM users WHERE id = ?",
            result -> result.next() ? result.getString("photo_profil_path") : null, userId);
    }

    private record UserRow(Integer id, String username, String email, String passwordHash,
            boolean suspended, String accountStatus) { }
}
