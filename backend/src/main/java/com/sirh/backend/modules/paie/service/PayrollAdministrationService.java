package com.sirh.backend.modules.paie.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Comparator;
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

    @Transactional(readOnly = true)
    public List<Map<String, Object>> irsaBrackets(Integer userId) {
        requireAdministration(userId);
        return jdbc.queryForList("""
            SELECT id, date_debut AS "dateDebut", date_fin AS "dateFin", borne_inferieure AS "borneInferieure",
                   borne_superieure AS "borneSuperieure", taux, source_regle AS "sourceRegle",
                   motif_modification AS "motifModification", (date_fin IS NULL) AS modifiable
            FROM tranches_irsa ORDER BY date_debut DESC, borne_inferieure
            """);
    }

    @Transactional
    public List<Map<String, Object>> updateIrsaBrackets(Integer userId, LocalDate effectiveDate,
            List<IrsaBracket> brackets, String reason) {
        requireAdministration(userId);
        if (effectiveDate == null || effectiveDate.isBefore(LocalDate.now(HR_ZONE)))
            throw bad("La date d’effet doit être aujourd’hui ou dans le futur.");
        if (reason == null || reason.isBlank() || reason.trim().length() > 500)
            throw bad("Indiquez le motif de modification (500 caractères maximum).");
        if (brackets == null || brackets.isEmpty()) throw bad("Ajoutez au moins une tranche IRSA.");
        if (brackets.stream().anyMatch(row -> row == null || row.borneInferieure() == null || row.taux() == null))
            throw bad("Chaque tranche doit préciser sa borne inférieure et son taux.");
        List<IrsaBracket> ordered = new ArrayList<>(brackets);
        ordered.sort(Comparator.comparing(IrsaBracket::borneInferieure));
        if (ordered.get(0).borneInferieure().compareTo(BigDecimal.ZERO) != 0)
            throw bad("Le barème doit commencer à 0 Ar.");
        for (int i = 0; i < ordered.size(); i++) {
            IrsaBracket row = ordered.get(i);
            if (row.borneInferieure() == null || row.taux() == null || row.borneInferieure().signum() < 0
                    || row.taux().signum() < 0 || row.taux().compareTo(BigDecimal.ONE) > 0)
                throw bad("Les bornes doivent être positives et chaque taux compris entre 0 et 1.");
            boolean last = i == ordered.size() - 1;
            if (last != (row.borneSuperieure() == null))
                throw bad("Seule la dernière tranche peut ne pas avoir de borne supérieure.");
            if (!last && (row.borneSuperieure().compareTo(row.borneInferieure()) <= 0
                    || row.borneSuperieure().compareTo(ordered.get(i + 1).borneInferieure()) != 0))
                throw bad("Les tranches doivent être croissantes, contiguës et sans chevauchement.");
        }
        List<LocalDate> starts = jdbc.query("SELECT date_debut FROM tranches_irsa WHERE date_fin IS NULL ORDER BY date_debut LIMIT 1 FOR UPDATE",
                (rs, row) -> rs.getDate(1).toLocalDate());
        if (starts.isEmpty()) throw conflict("Aucun barème IRSA actif à versionner.");
        LocalDate currentStart = starts.get(0);
        if (!effectiveDate.isAfter(currentStart)) throw bad("La nouvelle date d’effet doit suivre le début du barème en cours.");
        jdbc.update("UPDATE tranches_irsa SET date_fin=?,motif_modification=?,modifie_par_id=? WHERE date_fin IS NULL",
                effectiveDate.minusDays(1), reason.trim(), userId);
        for (IrsaBracket row : ordered) {
            jdbc.update("""
                INSERT INTO tranches_irsa(date_debut,borne_inferieure,borne_superieure,taux,source_regle,motif_modification,modifie_par_id)
                VALUES(?,?,?,?,?,?,?)
                """, effectiveDate, row.borneInferieure(), row.borneSuperieure(), row.taux(),
                row.sourceRegle() == null || row.sourceRegle().isBlank() ? "Barème IRSA configuré par Administration" : row.sourceRegle().trim(),
                reason.trim(), userId);
        }
        return irsaBrackets(userId);
    }

    public record IrsaBracket(BigDecimal borneInferieure, BigDecimal borneSuperieure, BigDecimal taux, String sourceRegle) { }

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
