package com.sirh.backend.modules.paie.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PayrollAdministrationService {
    private static final ZoneId HR_ZONE = ZoneId.of("Indian/Antananarivo");
    private final JdbcTemplate jdbc;

    public PayrollAdministrationService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> parameters(Integer userId) {
        requireAdministration(userId);
        return jdbc.queryForList("""
            SELECT p.id, p.code, p.libelle, p.valeur, p.unite,
                   r.libelle AS regime, p.regime_id AS "regimeId",
                   p.date_debut AS "dateDebut", p.date_fin AS "dateFin",
                   p.source_regle AS "sourceRegle", p.a_verifier AS "aVerifier",
                   p.motif_modification AS "motifModification",
                   (p.date_fin IS NULL) AS modifiable,
                   (CURRENT_DATE BETWEEN p.date_debut AND COALESCE(p.date_fin, DATE '9999-12-31')) AS "enVigueur"
            FROM parametres_paie p
            LEFT JOIN regimes_paie r ON r.id = p.regime_id
            ORDER BY p.code, r.code NULLS FIRST, p.date_debut DESC
            """);
    }

    @Transactional
    public List<Map<String, Object>> updateParameter(Integer userId, Integer id,
            BigDecimal value, LocalDate effectiveDate, String reason) {
        requireAdministration(userId);
        if (value == null || value.signum() < 0) throw bad("La valeur doit être positive ou nulle.");
        if (effectiveDate == null || effectiveDate.isBefore(LocalDate.now(HR_ZONE)))
            throw bad("La date d’effet doit être aujourd’hui ou dans le futur.");
        if (reason == null || reason.isBlank() || reason.trim().length() > 500)
            throw bad("Indiquez un motif de modification de 500 caractères maximum.");

        Map<String, Object> parameter = jdbc.query("""
            SELECT id, code, regime_id, date_debut AS date_debut, date_fin
            FROM parametres_paie WHERE id = ? FOR UPDATE
            """, rs -> {
                if (!rs.next()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Paramètre de paie introuvable.");
                Map<String, Object> row = new java.util.LinkedHashMap<>();
                row.put("id", rs.getInt("id"));
                row.put("code", rs.getString("code"));
                row.put("regimeId", rs.getObject("regime_id"));
                row.put("dateDebut", rs.getDate("date_debut").toLocalDate());
                row.put("dateFin", rs.getDate("date_fin") == null ? null : rs.getDate("date_fin").toLocalDate());
                return row;
            }, id);

        LocalDate start = (LocalDate) parameter.get("dateDebut");
        if (parameter.get("dateFin") != null) throw conflict("Seul le paramètre actuellement en vigueur peut être modifié.");
        if (!effectiveDate.isAfter(start)) throw bad("La nouvelle date d’effet doit suivre le début de la règle en cours.");
        LocalDate previousEnd = effectiveDate.minusDays(1);
        jdbc.update("UPDATE parametres_paie SET date_fin = ? WHERE id = ?", previousEnd, id);
        jdbc.update("""
            INSERT INTO parametres_paie
                (code, libelle, valeur, unite, regime_id, date_debut, source_regle, a_verifier, motif_modification, modifie_par_id)
            SELECT code, libelle, ?, unite, regime_id, ?, source_regle, a_verifier, ?, ?
            FROM parametres_paie WHERE id = ?
            """, value, effectiveDate, reason.trim(), userId, id);
        return parameters(userId);
    }

    private void requireAdministration(Integer userId) {
        Integer count = jdbc.queryForObject("""
            SELECT COUNT(*) FROM users u
            JOIN statuts_validation_compte s ON s.id = u.administrative_validation_status_id AND s.code = 'APPROVED'
            JOIN user_roles ur ON ur.user_id = u.id
            JOIN roles r ON r.id = ur.role_id AND LOWER(r.role_name) = 'administration'
            WHERE u.id = ? AND u.is_suspended = FALSE
            """, Integer.class, userId);
        if (count == null || count == 0) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Paramétrage réservé au rôle Administration.");
    }

    private static ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private static ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }
}
