package com.sirh.backend.modules.competences.service;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sirh.backend.modules.notifications.service.NotificationCenterService;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EvaluationService {
    private final JdbcTemplate jdbc;
    private final NotificationCenterService notifications;

    public EvaluationService(JdbcTemplate jdbc, NotificationCenterService notifications) {
        this.jdbc = jdbc;
        this.notifications = notifications;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> managerOverview(Integer managerId) {
        Integer departmentId = managerDepartment(managerId);
        List<Map<String, Object>> employees = jdbc.queryForList("""
            SELECT e.id AS "employeId", e.matricule, COALESCE(u.full_name, u.username) AS employe,
                   u.username, p.intitule AS poste, p.niveau_min AS "niveauMin", p.niveau_max AS "niveauMax",
                   (SELECT MAX(ev.date_evaluation) FROM evaluations_employe ev WHERE ev.employe_id = e.id) AS "derniereEvaluation",
                   (SELECT COUNT(*) FROM criteres_poste cp WHERE cp.poste_id = e.poste_id) AS "nbCriteres"
            FROM employes e
            JOIN users u ON u.id = e.user_id
            JOIN postes p ON p.id = e.poste_id
            LEFT JOIN situation_administrative_employe sa ON sa.employe_id = e.id
            LEFT JOIN statuts_administratifs_employe st ON st.id = sa.statut_id
            WHERE p.departement_id = ? AND p.actif = TRUE AND e.user_id <> ?
              AND COALESCE(st.code, 'ACTIF') <> 'SORTI'
            ORDER BY employe, e.matricule
            """, departmentId, managerId);
        List<Map<String, Object>> evaluations = jdbc.queryForList("""
            SELECT ev.id, ev.employe_id AS "employeId", COALESCE(u.full_name, u.username) AS employe,
                   p.intitule AS poste, ev.date_evaluation AS "dateEvaluation", ev.statut,
                   ev.soumise_at AS "soumiseAt", ev.validee_at AS "valideeAt",
                   (SELECT COUNT(*) FROM evaluation_lignes l WHERE l.evaluation_id = ev.id) AS "nbCriteres",
                   (SELECT COUNT(*) FROM evaluation_lignes l WHERE l.evaluation_id = ev.id AND l.ecart >= l.seuil_ecart) AS "nbEcarts"
            FROM evaluations_employe ev
            JOIN employes e ON e.id = ev.employe_id
            JOIN users u ON u.id = e.user_id
            JOIN postes p ON p.id = ev.poste_id
            WHERE p.departement_id = ?
            ORDER BY ev.date_evaluation DESC, ev.id DESC
            """, departmentId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("employes", employees);
        result.put("evaluations", evaluations);
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> evaluationForm(Integer managerId, Integer employeeId) {
        Integer departmentId = managerDepartment(managerId);
        Map<String, Object> employee = targetEmployee(employeeId, managerId);
        if (!departmentId.equals(employee.get("departementId"))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Salarie introuvable dans votre equipe.");
        }
        List<Map<String, Object>> criteres = criteriaForPoste(((Number) employee.get("posteId")).intValue());
        if (criteres.isEmpty()) {
            throw conflict("Aucun critere n'est configure pour ce poste. Les RH doivent definir le bareme.");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("employe", employee);
        result.put("criteres", criteres);
        result.put("dateEvaluation", LocalDate.now());
        return result;
    }

    @Transactional
    public Map<String, Object> createEvaluation(Integer managerId, Integer employeeId, LocalDate dateEvaluation,
            String commentaire, Map<Integer, Integer> notes) {
        Integer departmentId = managerDepartment(managerId);
        Map<String, Object> employee = targetEmployee(employeeId, managerId);
        if (!departmentId.equals(employee.get("departementId"))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Salarie introuvable dans votre equipe.");
        }
        if (dateEvaluation == null) throw bad("La date d'evaluation est obligatoire.");
        if (dateEvaluation.isAfter(LocalDate.now())) throw bad("La date d'evaluation ne peut pas etre future.");
        if (employee.get("dateEmbauche") instanceof LocalDate hireDate && dateEvaluation.isBefore(hireDate)) {
            throw bad("La date d'evaluation ne peut pas preceder la date d'embauche.");
        }
        List<Map<String, Object>> criteres = criteriaForPoste(((Number) employee.get("posteId")).intValue());
        if (criteres.isEmpty()) throw conflict("Aucun critere n'est configure pour ce poste.");
        if (notes == null) throw bad("Renseignez le niveau acquis pour chaque critere.");
        Set<Integer> expectedIds = new HashSet<>();
        for (Map<String, Object> critere : criteres) {
            expectedIds.add(((Number) critere.get("id")).intValue());
        }
        if (!notes.keySet().equals(expectedIds)) {
            throw bad("Tous les criteres du poste doivent etre notes, sans critere supplementaire.");
        }

        int min = ((Number) employee.get("niveauMin")).intValue();
        int max = ((Number) employee.get("niveauMax")).intValue();
        for (Map<String, Object> critere : criteres) {
            Integer acquired = notes.get(((Number) critere.get("id")).intValue());
            if (acquired == null || acquired < min || acquired > max) {
                throw bad("Chaque note doit rester dans le bareme du poste (" + min + " a " + max + ").");
            }
        }

        Integer evaluationId = jdbc.queryForObject("""
            INSERT INTO evaluations_employe (
                employe_id, poste_id, evaluateur_id, date_evaluation, statut, commentaire,
                bareme_min, bareme_max
            ) VALUES (?, ?, ?, ?, 'SOUMISE', ?, ?, ?)
            RETURNING id
            """, Integer.class, employeeId, employee.get("posteId"), managerId, dateEvaluation,
            clean(commentaire), min, max);

        for (Map<String, Object> critere : criteres) {
            int critereId = ((Number) critere.get("id")).intValue();
            int expected = ((Number) critere.get("niveauAttendu")).intValue();
            int acquired = notes.get(critereId);
            int threshold = ((Number) critere.get("seuilEcart")).intValue();
            jdbc.update("""
                INSERT INTO evaluation_lignes (
                    evaluation_id, critere_id, competence_id, niveau_attendu, niveau_acquis,
                    seuil_ecart, ecart, obligatoire
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, evaluationId, critereId, critere.get("competenceId"), expected, acquired,
                threshold, FormationMetrics.gap(expected, acquired), critere.get("obligatoire"));
        }

        notifications.notifyRole("rh", "EVALUATION_A_VALIDER", "Evaluation a valider",
            "L'evaluation de " + employee.get("employe") + " est soumise pour validation.",
            "/espace/rh/evaluations/" + evaluationId);
        Map<String, Object> result = detail(managerId, evaluationId, "manager");
        result.put("message", "Evaluation soumise. Les RH doivent maintenant la valider.");
        return result;
    }

    @Transactional
    public Map<String, Object> validate(Integer rhId, Integer evaluationId) {
        requireRole(rhId, "rh");
        Map<String, Object> evaluation = jdbc.query("""
            SELECT ev.id, ev.employe_id, ev.poste_id, ev.statut,
                   e.user_id AS employee_user_id,
                   COALESCE(u.full_name, u.username) AS employee_name
            FROM evaluations_employe ev
            JOIN employes e ON e.id = ev.employe_id
            JOIN users u ON u.id = e.user_id
            WHERE ev.id = ?
            FOR UPDATE OF ev
            """, rs -> {
                if (!rs.next()) return null;
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", rs.getInt("id"));
                row.put("employeId", rs.getInt("employe_id"));
                row.put("posteId", rs.getInt("poste_id"));
                row.put("statut", rs.getString("statut"));
                row.put("employeeUserId", rs.getInt("employee_user_id"));
                row.put("employeeName", rs.getString("employee_name"));
                return row;
            }, evaluationId);
        if (evaluation == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Evaluation introuvable.");
        if (!"SOUMISE".equals(evaluation.get("statut"))) throw conflict("Cette evaluation est deja validee.");

        jdbc.update("""
            UPDATE evaluations_employe
            SET statut = 'VALIDEE', valide_par_id = ?, validee_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """, rhId, evaluationId);

        int needsCreated = jdbc.update("""
            INSERT INTO besoins_formation (
                employe_id, competence_id, poste_id, evaluation_ligne_id,
                niveau_attendu, niveau_acquis_initial, seuil_ecart, ecart_initial, statut
            )
            SELECT ?, l.competence_id, ?, l.id, l.niveau_attendu, l.niveau_acquis,
                   l.seuil_ecart, l.ecart, 'OUVERT'
            FROM evaluation_lignes l
            WHERE l.evaluation_id = ?
              AND l.ecart >= l.seuil_ecart
              AND NOT EXISTS (
                  SELECT 1 FROM besoins_formation b
                  WHERE b.employe_id = ? AND b.competence_id = l.competence_id
                    AND b.statut IN ('OUVERT', 'PLANIFIE')
              )
            """, evaluation.get("employeId"), evaluation.get("posteId"), evaluationId, evaluation.get("employeId"));

        notifications.notifyUser(((Number) evaluation.get("employeeUserId")).intValue(),
            "EVALUATION_VALIDEE", "Evaluation validee",
            "Votre evaluation a ete validee par les RH. Les besoins de formation ont ete mis a jour.",
            "/espace/employe/evaluations/" + evaluationId);

        Map<String, Object> result = detail(rhId, evaluationId, "rh");
        result.put("besoinsCrees", needsCreated);
        result.put("message", needsCreated == 0
            ? "Evaluation validee. Aucun nouveau besoin de formation n'a ete cree."
            : "Evaluation validee. " + needsCreated + " besoin(s) de formation cree(s).");
        return result;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> rhOverview(Integer rhId) {
        requireRole(rhId, "rh");
        return jdbc.queryForList("""
            SELECT ev.id, ev.employe_id AS "employeId", COALESCE(eu.full_name, eu.username) AS employe,
                   e.matricule, p.intitule AS poste, d.nom AS departement,
                   ev.date_evaluation AS "dateEvaluation", ev.statut,
                   ev.soumise_at AS "soumiseAt", ev.validee_at AS "valideeAt",
                   (SELECT COUNT(*) FROM evaluation_lignes l WHERE l.evaluation_id = ev.id) AS "nbCriteres",
                   (SELECT COUNT(*) FROM evaluation_lignes l WHERE l.evaluation_id = ev.id AND l.ecart >= l.seuil_ecart) AS "nbEcarts",
                   (SELECT COUNT(*) FROM besoins_formation b WHERE b.evaluation_ligne_id IN
                       (SELECT l.id FROM evaluation_lignes l WHERE l.evaluation_id = ev.id)) AS "nbBesoins"
            FROM evaluations_employe ev
            JOIN employes e ON e.id = ev.employe_id
            JOIN users eu ON eu.id = e.user_id
            JOIN postes p ON p.id = ev.poste_id
            JOIN departements d ON d.id = p.departement_id
            ORDER BY CASE ev.statut WHEN 'SOUMISE' THEN 0 ELSE 1 END, ev.date_evaluation DESC, ev.id DESC
            """);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> employeeEvaluations(Integer userId) {
        Integer employeeId = employeeId(userId);
        requireRoleAny(userId, "employe", "manager");
        return jdbc.queryForList("""
            SELECT ev.id, COALESCE(eu.full_name, eu.username) AS employe, p.intitule AS poste,
                   d.nom AS departement, ev.date_evaluation AS "dateEvaluation", ev.statut, ev.validee_at AS "valideeAt",
                   (SELECT COUNT(*) FROM evaluation_lignes l WHERE l.evaluation_id = ev.id) AS "nbCriteres",
                   (SELECT COUNT(*) FROM evaluation_lignes l WHERE l.evaluation_id = ev.id AND l.ecart >= l.seuil_ecart) AS "nbEcarts",
                   (SELECT COUNT(*) FROM besoins_formation b WHERE b.evaluation_ligne_id IN
                       (SELECT l.id FROM evaluation_lignes l WHERE l.evaluation_id = ev.id)) AS "nbBesoins"
            FROM evaluations_employe ev
            JOIN employes e ON e.id = ev.employe_id
            JOIN users eu ON eu.id = e.user_id
            JOIN postes p ON p.id = ev.poste_id
            JOIN departements d ON d.id = p.departement_id
            WHERE ev.employe_id = ? AND ev.statut = 'VALIDEE'
            ORDER BY ev.date_evaluation DESC, ev.id DESC
            """, employeeId);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> detail(Integer actorId, Integer evaluationId, String view) {
        Map<String, Object> evaluation = jdbc.query("""
            SELECT ev.id, ev.employe_id AS "employeId", ev.poste_id AS "posteId",
                   COALESCE(eu.full_name, eu.username) AS employe, eu.username,
                   p.intitule AS poste, p.departement_id AS department_id, d.nom AS departement,
                   ev.date_evaluation AS "dateEvaluation", ev.statut, ev.commentaire,
                   ev.bareme_min AS "baremeMin", ev.bareme_max AS "baremeMax",
                   ev.soumise_at AS "soumiseAt", ev.validee_at AS "valideeAt",
                   COALESCE(evu.full_name, evu.username) AS evaluateur,
                   COALESCE(vu.full_name, vu.username) AS valideur
            FROM evaluations_employe ev
            JOIN employes e ON e.id = ev.employe_id
            JOIN users eu ON eu.id = e.user_id
            JOIN postes p ON p.id = ev.poste_id
            JOIN departements d ON d.id = p.departement_id
            JOIN users evu ON evu.id = ev.evaluateur_id
            LEFT JOIN users vu ON vu.id = ev.valide_par_id
            WHERE ev.id = ?
            """, rs -> {
                if (!rs.next()) return null;
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", rs.getInt("id"));
                row.put("employeId", rs.getInt("employeId"));
                row.put("posteId", rs.getInt("posteId"));
                row.put("employe", rs.getString("employe"));
                row.put("username", rs.getString("username"));
                row.put("poste", rs.getString("poste"));
                row.put("departmentId", rs.getInt("department_id"));
                row.put("departement", rs.getString("departement"));
                row.put("dateEvaluation", rs.getObject("dateEvaluation", LocalDate.class));
                row.put("statut", rs.getString("statut"));
                row.put("commentaire", rs.getString("commentaire"));
                row.put("baremeMin", rs.getInt("baremeMin"));
                row.put("baremeMax", rs.getInt("baremeMax"));
                row.put("soumiseAt", rs.getObject("soumiseAt"));
                row.put("valideeAt", rs.getObject("valideeAt"));
                row.put("evaluateur", rs.getString("evaluateur"));
                row.put("valideur", rs.getString("valideur"));
                return row;
            }, evaluationId);
        if (evaluation == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Evaluation introuvable.");

        if ("manager".equals(view)) {
            Integer departmentId = managerDepartment(actorId);
            if (!departmentId.equals(evaluation.get("departmentId"))) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Evaluation introuvable dans votre equipe.");
            }
        } else if ("rh".equals(view)) {
            requireRole(actorId, "rh");
        } else if ("employee".equals(view)) {
            Integer employeeId = employeeId(actorId);
            requireRoleAny(actorId, "employe", "manager");
            if (!employeeId.equals(evaluation.get("employeId")) || !"VALIDEE".equals(evaluation.get("statut"))) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Evaluation introuvable.");
            }
        } else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acces non autorise.");
        }

        List<Map<String, Object>> lignes = jdbc.queryForList("""
            SELECT l.id, l.competence_id AS "competenceId", c.code AS "competenceCode",
                   c.libelle AS competence, l.niveau_attendu AS "niveauAttendu",
                   l.niveau_acquis AS "niveauAcquis", l.seuil_ecart AS "seuilEcart",
                   l.ecart, l.obligatoire, l.commentaire
            FROM evaluation_lignes l
            JOIN competences c ON c.id = l.competence_id
            WHERE l.evaluation_id = ?
            ORDER BY c.libelle, c.code
            """, evaluationId);
        List<Map<String, Object>> besoins = jdbc.queryForList("""
            SELECT b.id, b.competence_id AS "competenceId", c.libelle AS competence,
                   b.statut, b.niveau_attendu AS "niveauAttendu",
                   b.niveau_acquis_initial AS "niveauAcquisInitial",
                   b.seuil_ecart AS "seuilEcart", b.ecart_initial AS "ecartInitial",
                   b.created_at AS "createdAt"
            FROM besoins_formation b
            JOIN competences c ON c.id = b.competence_id
            WHERE b.evaluation_ligne_id IN (
                SELECT l.id FROM evaluation_lignes l WHERE l.evaluation_id = ?
            )
            ORDER BY c.libelle, b.id
            """, evaluationId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("evaluation", evaluation);
        result.put("lignes", lignes);
        result.put("besoins", besoins);
        return result;
    }

    private List<Map<String, Object>> criteriaForPoste(Integer posteId) {
        return jdbc.queryForList("""
            SELECT cp.id, cp.competence_id AS "competenceId", c.code AS "competenceCode",
                   c.libelle AS competence, cp.niveau_attendu AS "niveauAttendu",
                   cp.seuil_ecart AS "seuilEcart", cp.obligatoire, cp.commentaire
            FROM criteres_poste cp
            JOIN competences c ON c.id = cp.competence_id
            WHERE cp.poste_id = ?
            ORDER BY c.libelle, c.code
            """, posteId);
    }

    private Map<String, Object> targetEmployee(Integer employeeId, Integer managerId) {
        if (employeeId == null) throw bad("Le salarie est obligatoire.");
        Map<String, Object> employee = jdbc.query("""
            SELECT e.id AS "employeId", e.user_id AS "userId", e.matricule,
                   COALESCE(u.full_name, u.username) AS employe, p.id AS "posteId",
                   p.intitule AS poste, p.departement_id AS "departementId",
                   p.niveau_min AS "niveauMin", p.niveau_max AS "niveauMax",
                   e.date_embauche AS "dateEmbauche", COALESCE(st.code, 'ACTIF') AS "statutCode"
            FROM employes e
            JOIN users u ON u.id = e.user_id
            JOIN postes p ON p.id = e.poste_id
            LEFT JOIN situation_administrative_employe sa ON sa.employe_id = e.id
            LEFT JOIN statuts_administratifs_employe st ON st.id = sa.statut_id
            WHERE e.id = ?
            """, rs -> {
                if (!rs.next()) return null;
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("employeId", rs.getInt("employeId"));
                row.put("userId", rs.getInt("userId"));
                row.put("matricule", rs.getString("matricule"));
                row.put("employe", rs.getString("employe"));
                row.put("posteId", rs.getInt("posteId"));
                row.put("poste", rs.getString("poste"));
                row.put("departementId", rs.getInt("departementId"));
                row.put("niveauMin", rs.getInt("niveauMin"));
                row.put("niveauMax", rs.getInt("niveauMax"));
                row.put("dateEmbauche", rs.getObject("dateEmbauche", LocalDate.class));
                row.put("statutCode", rs.getString("statutCode"));
                return row;
            }, employeeId);
        if (employee == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Salarie introuvable.");
        if (((Number) employee.get("userId")).intValue() == managerId) {
            throw conflict("Un manager ne peut pas evaluer son propre dossier.");
        }
        if ("SORTI".equals(employee.get("statutCode"))) {
            throw conflict("Ce salarie est sorti de l'effectif.");
        }
        return employee;
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

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }
}
