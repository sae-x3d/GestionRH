package com.sirh.backend.modules.competences.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.sirh.backend.modules.notifications.service.NotificationCenterService;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FormationService {
    private static final String EVOLUTION_SELECT = """
        SELECT e.id AS "employeId", e.matricule,
               COALESCE(u.full_name, u.username) AS employe,
               d.nom AS departement, p.intitule AS poste,
               m.id AS "mesureId", f.id AS "formationId", f.titre AS formation,
               c.code AS "competenceCode", c.libelle AS competence,
               m.mesure_at AS "dateMesure",
               b.niveau_attendu AS "niveauAttendu",
               b.niveau_acquis_initial AS "niveauInitial",
               m.niveau_avant AS "niveauAvant", m.niveau_apres AS "niveauApres",
               m.manque, m.acquis, m.pourcentage, m.commentaire,
               ev.date_evaluation AS "dateEvaluation"
        FROM mesures_formation m
        JOIN participations_formation pf ON pf.id = m.participation_id
        JOIN formations f ON f.id = pf.formation_id
        JOIN competences c ON c.id = f.competence_id
        JOIN besoins_formation b ON b.id = pf.besoin_id
        JOIN employes e ON e.id = pf.employe_id
        JOIN users u ON u.id = e.user_id
        JOIN postes p ON p.id = b.poste_id
        JOIN departements d ON d.id = p.departement_id
        LEFT JOIN evaluation_lignes l ON l.id = b.evaluation_ligne_id
        LEFT JOIN evaluations_employe ev ON ev.id = l.evaluation_id
        """;

    private final JdbcTemplate jdbc;
    private final NotificationCenterService notifications;

    public FormationService(JdbcTemplate jdbc, NotificationCenterService notifications) {
        this.jdbc = jdbc;
        this.notifications = notifications;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> managerNeeds(Integer managerId) {
        Integer departmentId = managerDepartment(managerId);
        return jdbc.queryForList("""
            SELECT b.id, b.employe_id AS "employeId", e.matricule,
                   COALESCE(u.full_name, u.username) AS employe, d.nom AS departement,
                   p.intitule AS poste, c.code AS "competenceCode", c.libelle AS competence,
                   b.statut, b.niveau_attendu AS "niveauAttendu",
                   b.niveau_acquis_initial AS "niveauAcquisInitial",
                   b.ecart_initial AS "ecartInitial", b.seuil_ecart AS "seuilEcart",
                   b.created_at AS "createdAt"
            FROM besoins_formation b
            JOIN employes e ON e.id = b.employe_id
            JOIN users u ON u.id = e.user_id
            JOIN postes p ON p.id = b.poste_id
            JOIN departements d ON d.id = p.departement_id
            JOIN competences c ON c.id = b.competence_id
            WHERE p.departement_id = ?
            ORDER BY CASE b.statut WHEN 'OUVERT' THEN 0 WHEN 'PLANIFIE' THEN 1 ELSE 2 END,
                     b.created_at DESC, b.id DESC
            """, departmentId);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> rhNeeds(Integer rhId) {
        requireRole(rhId, "rh");
        return jdbc.queryForList("""
            SELECT b.id, b.employe_id AS "employeId", e.matricule,
                   COALESCE(u.full_name, u.username) AS employe, d.nom AS departement,
                   p.intitule AS poste, c.code AS "competenceCode", c.libelle AS competence,
                   b.statut, b.niveau_attendu AS "niveauAttendu",
                   b.niveau_acquis_initial AS "niveauAcquisInitial",
                   b.ecart_initial AS "ecartInitial", b.seuil_ecart AS "seuilEcart",
                   b.created_at AS "createdAt"
            FROM besoins_formation b
            JOIN employes e ON e.id = b.employe_id
            JOIN users u ON u.id = e.user_id
            JOIN postes p ON p.id = b.poste_id
            JOIN departements d ON d.id = p.departement_id
            JOIN competences c ON c.id = b.competence_id
            ORDER BY CASE b.statut WHEN 'OUVERT' THEN 0 WHEN 'PLANIFIE' THEN 1 ELSE 2 END,
                     b.created_at DESC, b.id DESC
            """);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> formations(Integer rhId) {
        requireRole(rhId, "rh");
        return jdbc.queryForList("""
            SELECT f.id, f.titre, f.competence_id AS "competenceId", c.code AS "competenceCode",
                   c.libelle AS competence, f.organisme, f.date_debut AS "dateDebut",
                   f.date_fin AS "dateFin", f.niveau_cible AS "niveauCible", f.statut,
                   (SELECT COUNT(*) FROM participations_formation pf WHERE pf.formation_id = f.id) AS "nbParticipants",
                   (SELECT COUNT(*) FROM participations_formation pf WHERE pf.formation_id = f.id AND pf.statut = 'TERMINE') AS "nbTermines"
            FROM formations f
            JOIN competences c ON c.id = f.competence_id
            ORDER BY f.date_debut DESC NULLS LAST, f.id DESC
            """);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> detail(Integer rhId, Integer formationId) {
        requireRole(rhId, "rh");
        return detailInternal(formationId);
    }

    @Transactional
    public Map<String, Object> createFormation(Integer rhId, Integer competenceId, String titre, String description,
            String organisme, LocalDate dateDebut, LocalDate dateFin, Integer niveauCible) {
        requireRole(rhId, "rh");
        Boolean competenceActive = jdbc.query("SELECT actif FROM competences WHERE id = ?",
            rs -> rs.next() ? rs.getBoolean(1) : null, competenceId);
        if (competenceActive == null) throw bad("Competence introuvable.");
        if (!competenceActive) throw conflict("La competence est desactivee.");
        String title = required(titre, "Le titre de la formation est obligatoire.", 200);
        if (dateFin != null && dateDebut != null && dateFin.isBefore(dateDebut)) {
            throw bad("La date de fin doit etre posterieure ou egale a la date de debut.");
        }
        if (niveauCible != null && (niveauCible < 0 || niveauCible > 100)) {
            throw bad("Le niveau cible doit rester positif et raisonnable.");
        }
        Integer id = jdbc.queryForObject("""
            INSERT INTO formations (
                competence_id, titre, description, organisme, date_debut, date_fin,
                niveau_cible, statut, cree_par_id
            ) VALUES (?, ?, ?, ?, ?, ?, ?, 'PLANIFIEE', ?)
            RETURNING id
            """, Integer.class, competenceId, title, optional(description, 1500),
            optional(organisme, 150), dateDebut, dateFin, niveauCible, rhId);
        Map<String, Object> result = detailInternal(id);
        result.put("message", "Formation creee.");
        return result;
    }

    @Transactional
    public Map<String, Object> enroll(Integer rhId, Integer formationId, Integer besoinId) {
        requireRole(rhId, "rh");
        Map<String, Object> formation = jdbc.query("""
            SELECT id, competence_id, statut FROM formations WHERE id = ? FOR UPDATE
            """, rs -> {
                if (!rs.next()) return null;
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", rs.getInt("id"));
                row.put("competenceId", rs.getInt("competence_id"));
                row.put("statut", rs.getString("statut"));
                return row;
            }, formationId);
        if (formation == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Formation introuvable.");
        if (List.of("TERMINEE", "ANNULEE").contains(formation.get("statut"))) {
            throw conflict("Cette formation n'accepte plus d'inscription.");
        }
        Map<String, Object> need = jdbc.query("""
            SELECT b.id, b.employe_id, b.competence_id, b.statut, e.user_id
            FROM besoins_formation b
            JOIN employes e ON e.id = b.employe_id
            WHERE b.id = ? FOR UPDATE OF b
            """, rs -> {
                if (!rs.next()) return null;
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", rs.getInt("id"));
                row.put("employeId", rs.getInt("employe_id"));
                row.put("competenceId", rs.getInt("competence_id"));
                row.put("statut", rs.getString("statut"));
                row.put("userId", rs.getInt("user_id"));
                return row;
            }, besoinId);
        if (need == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Besoin de formation introuvable.");
        if (!"OUVERT".equals(need.get("statut"))) throw conflict("Ce besoin n'est plus ouvert a la planification.");
        if (!need.get("competenceId").equals(formation.get("competenceId"))) {
            throw bad("La formation et le besoin doivent concerner la meme competence.");
        }
        boolean already = Boolean.TRUE.equals(jdbc.queryForObject("""
            SELECT EXISTS (
                SELECT 1 FROM participations_formation
                WHERE formation_id = ? AND employe_id = ?
            )
            """, Boolean.class, formationId, need.get("employeId")));
        if (already) throw conflict("Ce salarie est deja inscrit a cette formation.");

        jdbc.update("""
            INSERT INTO participations_formation (formation_id, employe_id, besoin_id, statut)
            VALUES (?, ?, ?, 'INSCRIT')
            """, formationId, need.get("employeId"), besoinId);
        jdbc.update("""
            UPDATE besoins_formation
            SET statut = 'PLANIFIE', updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """, besoinId);

        notifications.notifyUser(((Number) need.get("userId")).intValue(), "FORMATION_INSCRIT",
            "Inscription a une formation",
            "Vous etes inscrit a la formation : " + formationTitle(formationId) + ".",
            "/espace/employe/formations");

        Map<String, Object> result = detailInternal(formationId);
        result.put("message", "Salarie inscrit a la formation.");
        return result;
    }

    @Transactional
    public Map<String, Object> measure(Integer rhId, Integer formationId, Integer participationId,
            Integer niveauAvant, Integer niveauApres, String commentaire) {
        requireRole(rhId, "rh");
        if (participationId == null) throw bad("La participation est obligatoire.");
        if (niveauAvant == null || niveauApres == null) {
            throw bad("Les niveaux avant et apres formation sont obligatoires.");
        }
        Map<String, Object> participation = jdbc.query("""
            SELECT pf.id, pf.formation_id, pf.employe_id, pf.statut, b.id AS besoin_id,
                   b.niveau_attendu, b.poste_id, e.user_id
            FROM participations_formation pf
            JOIN besoins_formation b ON b.id = pf.besoin_id
            JOIN employes e ON e.id = pf.employe_id
            WHERE pf.id = ? AND pf.formation_id = ?
            FOR UPDATE OF pf
            """, rs -> {
                if (!rs.next()) return null;
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", rs.getInt("id"));
                row.put("formationId", rs.getInt("formation_id"));
                row.put("employeId", rs.getInt("employe_id"));
                row.put("statut", rs.getString("statut"));
                row.put("besoinId", rs.getInt("besoin_id"));
                row.put("niveauAttendu", rs.getInt("niveau_attendu"));
                row.put("posteId", rs.getInt("poste_id"));
                row.put("userId", rs.getInt("user_id"));
                return row;
            }, participationId, formationId);
        if (participation == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Participation introuvable pour cette formation.");
        }
        if (!"INSCRIT".equals(participation.get("statut"))) {
            throw conflict("Cette participation est deja terminee ou abandonnee.");
        }
        boolean alreadyMeasured = Boolean.TRUE.equals(jdbc.queryForObject("""
            SELECT EXISTS (SELECT 1 FROM mesures_formation WHERE participation_id = ?)
            """, Boolean.class, participationId));
        if (alreadyMeasured) throw conflict("Une mesure existe deja pour cette participation.");

        Map<String, Object> bareme = jdbc.query("""
            SELECT niveau_min, niveau_max FROM postes WHERE id = ?
            """, rs -> {
                if (!rs.next()) return null;
                return Map.of("min", rs.getInt("niveau_min"), "max", rs.getInt("niveau_max"));
            }, participation.get("posteId"));
        if (bareme == null) throw conflict("Le poste du besoin est introuvable.");
        int min = ((Number) bareme.get("min")).intValue();
        int max = ((Number) bareme.get("max")).intValue();
        if (niveauAvant < min || niveauAvant > max || niveauApres < min || niveauApres > max) {
            throw bad("Les niveaux mesures doivent rester dans le bareme du poste (" + min + " a " + max + ").");
        }

        int expected = ((Number) participation.get("niveauAttendu")).intValue();
        int manque = FormationMetrics.missing(expected, niveauAvant);
        int acquis = FormationMetrics.acquired(niveauAvant, niveauApres, manque);
        BigDecimal pourcentage = FormationMetrics.percentage(expected, niveauAvant, niveauApres);

        jdbc.update("""
            INSERT INTO mesures_formation (
                participation_id, niveau_avant, niveau_apres, manque, acquis,
                pourcentage, mesure_par_id, commentaire
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """, participationId, niveauAvant, niveauApres, manque, acquis, pourcentage, rhId,
            optional(commentaire, 1000));
        jdbc.update("""
            UPDATE participations_formation
            SET statut = 'TERMINE', termine_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """, participationId);
        jdbc.update("""
            UPDATE besoins_formation
            SET statut = 'CLOTURE', cloture_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """, participation.get("besoinId"));

        int remaining = jdbc.queryForObject("""
            SELECT COUNT(*) FROM participations_formation
            WHERE formation_id = ? AND statut = 'INSCRIT'
            """, Integer.class, formationId);
        if (remaining == 0) {
            jdbc.update("""
                UPDATE formations
                SET statut = 'TERMINEE', updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND statut <> 'ANNULEE'
                """, formationId);
        }

        notifications.notifyUser(((Number) participation.get("userId")).intValue(), "FORMATION_MESUREE",
            "Resultat de formation disponible",
            "La mesure de votre formation est disponible : " + pourcentage + " % du manque initial a ete acquis.",
            "/espace/employe/formations");

        Map<String, Object> result = detailInternal(formationId);
        result.put("message", "Mesure enregistree : " + pourcentage + " % du manque initial a ete acquis.");
        return result;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> myFormations(Integer userId) {
        Integer employeeId = employeeId(userId);
        requireRoleAny(userId, "employe", "manager");
        return jdbc.queryForList("""
            SELECT pf.id AS "participationId", f.id AS "formationId", f.titre,
                   c.code AS "competenceCode", c.libelle AS competence, f.organisme,
                   f.date_debut AS "dateDebut", f.date_fin AS "dateFin",
                   f.statut AS "statutFormation", pf.statut AS "statutParticipation",
                   b.niveau_attendu AS "niveauAttendu", b.niveau_acquis_initial AS "niveauAcquisInitial",
                   b.ecart_initial AS "ecartInitial", m.niveau_avant AS "niveauAvant",
                   m.niveau_apres AS "niveauApres", m.pourcentage, m.mesure_at AS "mesureAt",
                   m.commentaire AS "commentaireMesure"
            FROM participations_formation pf
            JOIN formations f ON f.id = pf.formation_id
            JOIN competences c ON c.id = f.competence_id
            JOIN besoins_formation b ON b.id = pf.besoin_id
            LEFT JOIN mesures_formation m ON m.participation_id = pf.id
            WHERE pf.employe_id = ?
            ORDER BY f.date_debut DESC NULLS LAST, pf.id DESC
            """, employeeId);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> evolutionRh(Integer rhId) {
        requireRole(rhId, "rh");
        return evolution(null);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> evolutionEmploye(Integer userId) {
        Integer employeeId = employeeId(userId);
        requireRoleAny(userId, "employe", "manager");
        return evolution(employeeId);
    }

    private List<Map<String, Object>> evolution(Integer employeeId) {
        String sql = EVOLUTION_SELECT
            + (employeeId == null ? "" : " WHERE pf.employe_id = ? ")
            + " ORDER BY employe, m.mesure_at, m.id";
        return employeeId == null
            ? jdbc.queryForList(sql)
            : jdbc.queryForList(sql, employeeId);
    }

    private Map<String, Object> detailInternal(Integer formationId) {
        Map<String, Object> formation = jdbc.query("""
            SELECT f.id, f.titre, f.competence_id AS "competenceId", c.code AS "competenceCode",
                   c.libelle AS competence, f.description, f.organisme,
                   f.date_debut AS "dateDebut", f.date_fin AS "dateFin",
                   f.niveau_cible AS "niveauCible", f.statut,
                   COALESCE(u.full_name, u.username) AS "creePar", f.created_at AS "createdAt"
            FROM formations f
            JOIN competences c ON c.id = f.competence_id
            JOIN users u ON u.id = f.cree_par_id
            WHERE f.id = ?
            """, rs -> {
                if (!rs.next()) return null;
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", rs.getInt("id"));
                row.put("titre", rs.getString("titre"));
                row.put("competenceId", rs.getInt("competenceId"));
                row.put("competenceCode", rs.getString("competenceCode"));
                row.put("competence", rs.getString("competence"));
                row.put("description", rs.getString("description"));
                row.put("organisme", rs.getString("organisme"));
                row.put("dateDebut", rs.getObject("dateDebut", LocalDate.class));
                row.put("dateFin", rs.getObject("dateFin", LocalDate.class));
                row.put("niveauCible", rs.getObject("niveauCible"));
                row.put("statut", rs.getString("statut"));
                row.put("creePar", rs.getString("creePar"));
                row.put("createdAt", rs.getObject("createdAt"));
                return row;
            }, formationId);
        if (formation == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Formation introuvable.");

        List<Map<String, Object>> participants = jdbc.queryForList("""
            SELECT pf.id AS "participationId", pf.employe_id AS "employeId", e.matricule,
                   COALESCE(u.full_name, u.username) AS employe, pf.statut,
                   pf.inscrit_at AS "inscritAt", pf.termine_at AS "termineAt",
                   b.id AS "besoinId", b.niveau_attendu AS "niveauAttendu",
                   b.niveau_acquis_initial AS "niveauAcquisInitial",
                   m.id AS "mesureId", m.niveau_avant AS "niveauAvant",
                   m.niveau_apres AS "niveauApres", m.manque, m.acquis,
                   m.pourcentage, m.mesure_at AS "mesureAt", m.commentaire
            FROM participations_formation pf
            JOIN employes e ON e.id = pf.employe_id
            JOIN users u ON u.id = e.user_id
            JOIN besoins_formation b ON b.id = pf.besoin_id
            LEFT JOIN mesures_formation m ON m.participation_id = pf.id
            WHERE pf.formation_id = ?
            ORDER BY employe, e.matricule
            """, formationId);

        List<Map<String, Object>> eligibleNeeds = jdbc.queryForList("""
            SELECT b.id, b.employe_id AS "employeId", e.matricule,
                   COALESCE(u.full_name, u.username) AS employe,
                   b.niveau_attendu AS "niveauAttendu",
                   b.niveau_acquis_initial AS "niveauAcquisInitial",
                   b.ecart_initial AS "ecartInitial"
            FROM besoins_formation b
            JOIN employes e ON e.id = b.employe_id
            JOIN users u ON u.id = e.user_id
            WHERE b.competence_id = ? AND b.statut = 'OUVERT'
              AND NOT EXISTS (
                  SELECT 1 FROM participations_formation pf
                  WHERE pf.formation_id = ? AND pf.employe_id = b.employe_id
              )
            ORDER BY employe, e.matricule
            """, formation.get("competenceId"), formationId);

        List<Map<String, Object>> competences = jdbc.queryForList("""
            SELECT id, code, libelle FROM competences WHERE actif = TRUE ORDER BY libelle, code
            """);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("formation", formation);
        result.put("participants", participants);
        result.put("besoinsEligibles", eligibleNeeds);
        result.put("competences", competences);
        return result;
    }

    private String formationTitle(Integer formationId) {
        return jdbc.query("SELECT titre FROM formations WHERE id = ?",
            rs -> rs.next() ? rs.getString(1) : "formation", formationId);
    }

    private Integer employeeId(Integer userId) {
        Integer id = jdbc.query("SELECT id FROM employes WHERE user_id = ?",
            rs -> rs.next() ? rs.getInt(1) : null, userId);
        if (id == null) throw new ResponseStatusException(HttpStatus.FORBIDDEN,
            "Aucun dossier salarie n'est lie a ce compte.");
        return id;
    }

    private Integer managerDepartment(Integer managerId) {
        requireRole(managerId, "manager");
        List<Integer> ids = jdbc.queryForList("""
            SELECT DISTINCT p.departement_id
            FROM employes e
            JOIN postes p ON p.id = e.poste_id
            WHERE e.user_id = ? AND p.actif = TRUE
            """, Integer.class, managerId);
        if (ids.size() != 1) {
            throw conflict("Le compte manager doit etre rattache a un seul departement actif.");
        }
        return ids.getFirst();
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
        if (!allowed) throw new ResponseStatusException(HttpStatus.FORBIDDEN,
            "Un compte " + role + " valide est requis.");
    }

    private void requireRoleAny(Integer userId, String... roles) {
        for (String role : roles) {
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
            if (allowed) return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ce compte n'a pas le role requis.");
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

    private ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }
}
