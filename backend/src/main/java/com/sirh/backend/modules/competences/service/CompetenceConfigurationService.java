package com.sirh.backend.modules.competences.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CompetenceConfigurationService {
    private final JdbcTemplate jdbc;

    public CompetenceConfigurationService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> competences(Integer rhId) {
        requireRole(rhId, "rh");
        return jdbc.queryForList("""
            SELECT c.id, c.code, c.libelle, c.description, c.actif,
                   COUNT(cp.id) AS "criteresUtilises",
                   c.created_at AS "createdAt", c.updated_at AS "updatedAt"
            FROM competences c
            LEFT JOIN criteres_poste cp ON cp.competence_id = c.id
            GROUP BY c.id
            ORDER BY c.libelle, c.code
            """);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> configuration(Integer rhId, Integer posteId) {
        requireRole(rhId, "rh");
        List<Map<String, Object>> postes = jdbc.queryForList("""
            SELECT p.id, p.intitule AS poste, p.code_matricule AS "codeMatricule",
                   d.nom AS departement, p.niveau_min AS "niveauMin", p.niveau_max AS "niveauMax",
                   p.actif
            FROM postes p
            JOIN departements d ON d.id = p.departement_id
            WHERE p.actif = TRUE
            ORDER BY d.nom, p.intitule
            """);
        Integer selectedPosteId = posteId;
        if (selectedPosteId == null && !postes.isEmpty()) {
            selectedPosteId = ((Number) postes.getFirst().get("id")).intValue();
        }
        Map<String, Object> selectedPoste = selectedPosteId == null ? null : poste(selectedPosteId);
        List<Map<String, Object>> criteres = selectedPosteId == null ? List.of() : jdbc.queryForList("""
            SELECT cp.id, cp.competence_id AS "competenceId", c.code AS "competenceCode",
                   c.libelle AS competence, cp.niveau_attendu AS "niveauAttendu",
                   cp.seuil_ecart AS "seuilEcart", cp.obligatoire, cp.commentaire
            FROM criteres_poste cp
            JOIN competences c ON c.id = cp.competence_id
            WHERE cp.poste_id = ?
            ORDER BY c.libelle, c.code
            """, selectedPosteId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("postes", postes);
        result.put("poste", selectedPoste);
        result.put("criteres", criteres);
        result.put("competences", competences(rhId));
        return result;
    }

    @Transactional
    public Map<String, Object> createCompetence(Integer rhId, String codeValue, String libelle, String description) {
        requireRole(rhId, "rh");
        String code = normalizeCode(codeValue);
        String label = required(libelle, "Le libelle de la competence est obligatoire.", 150);
        String details = optional(description, 1000);
        try {
            Integer id = jdbc.queryForObject("""
                INSERT INTO competences (code, libelle, description, cree_par_id)
                VALUES (?, ?, ?, ?)
                RETURNING id
                """, Integer.class, code, label, details, rhId);
            return Map.of("id", id, "message", "Competence creee.");
        } catch (DuplicateKeyException exception) {
            throw conflict("Une competence utilise deja ce code.");
        }
    }

    @Transactional
    public Map<String, Object> setCompetenceActive(Integer rhId, Integer competenceId, Boolean actif) {
        requireRole(rhId, "rh");
        if (actif == null) throw bad("Le statut de la competence est obligatoire.");
        int updated = jdbc.update("""
            UPDATE competences SET actif = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?
            """, actif, competenceId);
        if (updated == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Competence introuvable.");
        return Map.of("message", actif ? "Competence activee." : "Competence desactivee.");
    }

    @Transactional
    public Map<String, Object> saveCriterion(Integer rhId, Integer posteId, Integer competenceId,
            Integer niveauAttendu, Integer seuilEcart, Boolean obligatoire, String commentaire) {
        requireRole(rhId, "rh");
        Map<String, Object> poste = poste(posteId);
        int min = ((Number) poste.get("niveauMin")).intValue();
        int max = ((Number) poste.get("niveauMax")).intValue();
        int expected = requiredInt(niveauAttendu, "Le niveau attendu est obligatoire.");
        int threshold = seuilEcart == null ? 1 : seuilEcart;
        if (expected < min || expected > max) {
            throw bad("Le niveau attendu doit rester dans le bareme du poste (" + min + " a " + max + ").");
        }
        if (threshold < 0 || threshold > max - min) {
            throw bad("Le seuil d'ecart doit etre compris entre 0 et " + (max - min) + ".");
        }
        Boolean active = jdbc.query("SELECT actif FROM competences WHERE id = ?",
            rs -> rs.next() ? rs.getBoolean(1) : null, competenceId);
        if (active == null) throw bad("Competence introuvable.");
        if (!active) throw bad("La competence est desactivee. Reactivez-la avant de l'ajouter au bareme.");
        jdbc.update("""
            INSERT INTO criteres_poste (poste_id, competence_id, niveau_attendu, seuil_ecart, obligatoire, commentaire, cree_par_id)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (poste_id, competence_id) DO UPDATE
            SET niveau_attendu = EXCLUDED.niveau_attendu,
                seuil_ecart = EXCLUDED.seuil_ecart,
                obligatoire = EXCLUDED.obligatoire,
                commentaire = EXCLUDED.commentaire,
                updated_at = CURRENT_TIMESTAMP
            """, posteId, competenceId, expected, threshold, obligatoire == null || obligatoire,
            optional(commentaire, 1000), rhId);
        return configuration(rhId, posteId);
    }

    @Transactional
    public Map<String, Object> deleteCriterion(Integer rhId, Integer posteId, Integer critereId) {
        requireRole(rhId, "rh");
        Integer existingPoste = jdbc.query("SELECT poste_id FROM criteres_poste WHERE id = ?",
            rs -> rs.next() ? rs.getInt(1) : null, critereId);
        if (existingPoste == null || !existingPoste.equals(posteId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Critere introuvable pour ce poste.");
        }
        try {
            jdbc.update("DELETE FROM criteres_poste WHERE id = ?", critereId);
        } catch (DataIntegrityViolationException exception) {
            throw conflict("Ce critere possede deja un historique d'evaluation et ne peut plus etre supprime.");
        }
        return configuration(rhId, posteId);
    }

    @Transactional
    public Map<String, Object> updateBareme(Integer rhId, Integer posteId, Integer niveauMin, Integer niveauMax) {
        requireRole(rhId, "rh");
        poste(posteId);
        int min = requiredInt(niveauMin, "Le niveau minimum est obligatoire.");
        int max = requiredInt(niveauMax, "Le niveau maximum est obligatoire.");
        if (min < 0 || max <= min || max > 100) {
            throw bad("Le bareme doit verifier 0 <= minimum < maximum <= 100.");
        }
        String incompatible = jdbc.query("""
            SELECT c.libelle
            FROM criteres_poste cp
            JOIN competences c ON c.id = cp.competence_id
            WHERE cp.poste_id = ?
              AND (cp.niveau_attendu < ? OR cp.niveau_attendu > ? OR cp.seuil_ecart > ?)
            ORDER BY c.libelle
            LIMIT 1
            """, rs -> rs.next() ? rs.getString(1) : null, posteId, min, max, max - min);
        if (incompatible != null) {
            throw conflict("Le bareme est incompatible avec le critere deja configure : " + incompatible + ".");
        }
        jdbc.update("""
            UPDATE postes SET niveau_min = ?, niveau_max = ? WHERE id = ?
            """, min, max, posteId);
        return configuration(rhId, posteId);
    }

    private Map<String, Object> poste(Integer posteId) {
        if (posteId == null) throw bad("Le poste est obligatoire.");
        Map<String, Object> poste = jdbc.query("""
            SELECT p.id, p.intitule AS poste, p.code_matricule AS "codeMatricule",
                   d.nom AS departement, p.niveau_min AS "niveauMin", p.niveau_max AS "niveauMax",
                   p.actif
            FROM postes p
            JOIN departements d ON d.id = p.departement_id
            WHERE p.id = ?
            """, rs -> {
                if (!rs.next()) return null;
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", rs.getInt("id"));
                row.put("poste", rs.getString("poste"));
                row.put("codeMatricule", rs.getString("codeMatricule"));
                row.put("departement", rs.getString("departement"));
                row.put("niveauMin", rs.getInt("niveauMin"));
                row.put("niveauMax", rs.getInt("niveauMax"));
                row.put("actif", rs.getBoolean("actif"));
                return row;
            }, posteId);
        if (poste == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Poste introuvable.");
        if (!Boolean.TRUE.equals(poste.get("actif"))) throw conflict("Le poste est desactive.");
        return poste;
    }

    private void requireRole(Integer userId, String role) {
        boolean allowed = Boolean.TRUE.equals(jdbc.queryForObject("""
            SELECT EXISTS (
                SELECT 1 FROM users u
                JOIN statuts_validation_compte s ON s.id = u.administrative_validation_status_id
                JOIN user_roles ur ON ur.user_id = u.id
                JOIN roles r ON r.id = ur.role_id
                WHERE u.id = ? AND u.is_suspended = FALSE AND s.code = 'APPROVED'
                  AND LOWER(r.role_name) = LOWER(?)
            )
            """, Boolean.class, userId, role));
        if (!allowed) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Un compte RH valide est requis.");
    }

    private String normalizeCode(String value) {
        String code = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (!code.matches("^[A-Z0-9_]{2,40}$")) {
            throw bad("Le code doit contenir 2 a 40 caracteres : lettres majuscules, chiffres ou underscore.");
        }
        return code;
    }

    private String required(String value, String message, int maxLength) {
        if (value == null || value.isBlank()) throw bad(message);
        String cleaned = value.trim();
        if (cleaned.length() > maxLength) throw bad("Le texte depasse " + maxLength + " caracteres.");
        return cleaned;
    }

    private String optional(String value, int maxLength) {
        if (value == null || value.isBlank()) return null;
        String cleaned = value.trim();
        if (cleaned.length() > maxLength) throw bad("Le texte depasse " + maxLength + " caracteres.");
        return cleaned;
    }

    private int requiredInt(Integer value, String message) {
        if (value == null) throw bad(message);
        return value;
    }

    private ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }
}
