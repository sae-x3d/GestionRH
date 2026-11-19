package com.sirh.backend.modules.carriere.service;

import com.sirh.backend.modules.carriere.dto.ChangementPosteRequest;
import com.sirh.backend.modules.carriere.dto.ChangementSalaireRequest;
import com.sirh.backend.modules.carriere.dto.MutationRequest;
import com.sirh.backend.modules.carriere.dto.PromotionRequest;
import com.sirh.backend.modules.notifications.service.NotificationCenterService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CareerManagementService {
    private static final ZoneId HR_ZONE = ZoneId.of("Indian/Antananarivo");
    private static final String EMPLOYEE_SELECT = """
        SELECT e.id, e.user_id, e.matricule, COALESCE(u.full_name, u.username) AS nom,
               e.date_embauche, e.poste_id, p.departement_id, p.intitule AS poste, d.nom AS departement,
               COALESCE(sa.code, 'ACTIF') AS statut_code, COALESCE(sa.libelle, 'Actif') AS statut,
               (SELECT MAX(h.date_effet) FROM historique_affectation_employe h WHERE h.employe_id = e.id) AS derniere_affectation,
               (SELECT MAX(h.date_effet) FROM historique_salaire_employe h WHERE h.employe_id = e.id) AS dernier_salaire,
               contrat.salaire_base_mensuel AS salaire_base, contrat.id AS contrat_id, contrat.numero AS contrat_numero
        FROM employes e
        JOIN users u ON u.id = e.user_id
        JOIN postes p ON p.id = e.poste_id
        JOIN departements d ON d.id = p.departement_id
        LEFT JOIN situation_administrative_employe situation ON situation.employe_id = e.id
        LEFT JOIN statuts_administratifs_employe sa ON sa.id = situation.statut_id
        LEFT JOIN LATERAL (
            SELECT ct.id, ct.numero, ct.salaire_base_mensuel
            FROM candidatures c
            JOIN contrats ct ON ct.candidature_id = c.id
            JOIN statuts_contrat sc ON sc.id = ct.statut_id
            WHERE c.candidat_id = e.user_id AND sc.code = 'ACTIF'
            ORDER BY ct.date_debut DESC, ct.id DESC
            LIMIT 1
        ) contrat ON TRUE
        """;

    private final JdbcTemplate jdbc;
    private final NotificationCenterService notifications;

    public CareerManagementService(JdbcTemplate jdbc, NotificationCenterService notifications) {
        this.jdbc = jdbc;
        this.notifications = notifications;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> rhDirectory(Integer actorId, String query) {
        requireActiveRole(actorId, "rh");
        return directory(query, null, true);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> rhCareer(Integer actorId, Integer employeeId) {
        requireActiveRole(actorId, "rh");
        return career(employeeId, true, true);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> rhSituation(Integer actorId, Integer employeeId, LocalDate date) {
        requireActiveRole(actorId, "rh");
        return situation(employee(employeeId, false), consultationDate(date), true);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> managerTeam(Integer actorId, String query) {
        Integer departmentId = managerDepartment(actorId);
        return directory(query, departmentId, false);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> managerCareer(Integer actorId, Integer employeeId) {
        EmployeeSnapshot employee = employee(employeeId, false);
        ensureManagerScope(actorId, employee);
        return career(employeeId, false, false);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> managerSituation(Integer actorId, Integer employeeId, LocalDate date) {
        EmployeeSnapshot employee = employee(employeeId, false);
        ensureManagerScope(actorId, employee);
        return situation(employee, consultationDate(date), false);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> ownCareer(Integer actorId) {
        requireActiveEmployeeAccount(actorId);
        return career(employeeIdForUser(actorId), true, false);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> ownSituation(Integer actorId, LocalDate date) {
        requireActiveEmployeeAccount(actorId);
        return situation(employee(employeeIdForUser(actorId), false), consultationDate(date), true);
    }

    @Transactional
    public Map<String, Object> promotion(Integer actorId, PromotionRequest request) {
        requireActiveRole(actorId, "rh");
        EmployeeSnapshot employee = employee(request.employeId(), true);
        ensureChangeable(employee);
        validateEffectiveDate(employee, request.dateEffet(), true, true);
        PostSnapshot target = post(request.nouveauPosteId());
        if (Objects.equals(employee.posteId(), target.id())) throw bad("Le nouveau poste doit être différent du poste actuel.");
        if (!Objects.equals(employee.departementId(), target.departementId()))
            throw bad("Une promotion conserve le même département. Utilisez une mutation pour changer de département.");
        if (employee.salaireBase() == null || employee.contratId() == null)
            throw conflict("Aucun contrat actif avec un salaire de base n’est disponible pour ce salarié.");
        validateSalaryRange(target, request.nouveauSalaireBase());
        if (request.nouveauSalaireBase().compareTo(employee.salaireBase()) <= 0)
            throw bad("Une promotion doit augmenter le salaire de base.");
        String motif = cleanMotif(request.motif());

        Integer affectationId = insertAffectation(employee, target, "PROMOTION", "PROMOTION",
            request.dateEffet(), motif, actorId);
        Integer salaireId = insertSalaire(employee, request.nouveauSalaireBase(), "PROMOTION",
            request.dateEffet(), motif, actorId);
        jdbc.update("""
            INSERT INTO promotions
                (employe_id, ancien_poste_id, nouveau_poste_id, departement_id, ancien_salaire_base,
                 nouveau_salaire_base, date_effet, motif, decide_par_id, historique_affectation_id,
                 historique_salaire_id)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """, employee.id(), employee.posteId(), target.id(), target.departementId(),
            employee.salaireBase(), request.nouveauSalaireBase(), request.dateEffet(), motif, actorId,
            affectationId, salaireId);
        updateEmployeePost(employee.id(), target.id(), "PROMOTION");
        updateContractSalary(employee.contratId(), request.nouveauSalaireBase());
        recordMovement(employee, actorId, request.dateEffet(), motif, affectationId,
            "Promotion : " + employee.poste() + " → " + target.intitule() + " (" + target.departement() + ")");
        return career(employee.id(), true, true);
    }

    @Transactional
    public Map<String, Object> mutation(Integer actorId, MutationRequest request) {
        requireActiveRole(actorId, "rh");
        EmployeeSnapshot employee = employee(request.employeId(), true);
        ensureChangeable(employee);
        validateEffectiveDate(employee, request.dateEffet(), true, true);
        PostSnapshot target = post(request.nouveauPosteId());
        if (Objects.equals(employee.posteId(), target.id())) throw bad("Le nouveau poste doit être différent du poste actuel.");
        if (Objects.equals(employee.departementId(), target.departementId()))
            throw bad("Une mutation change de département. Utilisez un changement de poste pour rester dans le même département.");
        if (employee.salaireBase() == null || employee.contratId() == null)
            throw conflict("Aucun contrat actif avec un salaire de base n’est disponible pour ce salarié.");
        BigDecimal requestedSalary = request.nouveauSalaireBase();
        boolean salaryChange = requestedSalary != null && requestedSalary.compareTo(employee.salaireBase()) != 0;
        if (requestedSalary != null) validateSalaryRange(target, requestedSalary);
        else validateSalaryRange(target, employee.salaireBase());
        String motif = cleanMotif(request.motif());

        Integer affectationId = insertAffectation(employee, target, "MUTATION", "TRANSFERT",
            request.dateEffet(), motif, actorId);
        Integer salaireId = salaryChange
            ? insertSalaire(employee, requestedSalary, "MUTATION", request.dateEffet(), motif, actorId)
            : null;
        BigDecimal recordedSalary = salaryChange ? requestedSalary : employee.salaireBase();
        jdbc.update("""
            INSERT INTO mutations
                (employe_id, ancien_poste_id, nouveau_poste_id, ancien_departement_id, nouveau_departement_id,
                 ancien_salaire_base, nouveau_salaire_base, date_effet, motif, decide_par_id,
                 historique_affectation_id, historique_salaire_id)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """, employee.id(), employee.posteId(), target.id(), employee.departementId(), target.departementId(),
            employee.salaireBase(), recordedSalary, request.dateEffet(), motif, actorId,
            affectationId, salaireId);
        updateEmployeePost(employee.id(), target.id(), "TRANSFERT");
        if (salaryChange) updateContractSalary(employee.contratId(), requestedSalary);
        recordMovement(employee, actorId, request.dateEffet(), motif, affectationId,
            "Mutation : " + employee.departement() + " / " + employee.poste() + " → "
                + target.departement() + " / " + target.intitule());
        return career(employee.id(), true, true);
    }

    @Transactional
    public Map<String, Object> changementPoste(Integer actorId, ChangementPosteRequest request) {
        requireActiveRole(actorId, "rh");
        EmployeeSnapshot employee = employee(request.employeId(), true);
        ensureChangeable(employee);
        validateEffectiveDate(employee, request.dateEffet(), true, false);
        PostSnapshot target = post(request.nouveauPosteId());
        if (Objects.equals(employee.posteId(), target.id())) throw bad("Le nouveau poste doit être différent du poste actuel.");
        if (!Objects.equals(employee.departementId(), target.departementId()))
            throw bad("Un changement de poste conserve le même département. Utilisez une mutation pour changer de département.");
        if (employee.salaireBase() == null)
            throw conflict("Aucun contrat actif avec un salaire de base n’est disponible pour ce salarié.");
        validateSalaryRange(target, employee.salaireBase());
        String motif = cleanMotif(request.motif());

        Integer affectationId = insertAffectation(employee, target, "CHANGEMENT_POSTE", "PROMOTION",
            request.dateEffet(), motif, actorId);
        updateEmployeePost(employee.id(), target.id(), "PROMOTION");
        recordMovement(employee, actorId, request.dateEffet(), motif, affectationId,
            "Changement de poste : " + employee.poste() + " → " + target.intitule());
        return career(employee.id(), true, true);
    }

    @Transactional
    public Map<String, Object> changementSalaire(Integer actorId, ChangementSalaireRequest request) {
        requireActiveRole(actorId, "rh");
        EmployeeSnapshot employee = employee(request.employeId(), true);
        ensureChangeable(employee);
        validateEffectiveDate(employee, request.dateEffet(), false, true);
        PostSnapshot currentPost = post(employee.posteId());
        if (employee.salaireBase() == null || employee.contratId() == null)
            throw conflict("Aucun contrat actif avec un salaire de base n’est disponible pour ce salarié.");
        validateSalaryRange(currentPost, request.nouveauSalaireBase());
        if (request.nouveauSalaireBase().compareTo(employee.salaireBase()) == 0)
            throw bad("Le nouveau salaire doit être différent du salaire actuel.");
        String motif = cleanMotif(request.motif());

        Integer salaireId = insertSalaire(employee, request.nouveauSalaireBase(), "CHANGEMENT_SALAIRE",
            request.dateEffet(), motif, actorId);
        updateContractSalary(employee.contratId(), request.nouveauSalaireBase());
        recordMovement(employee, actorId, request.dateEffet(), motif, salaireId,
            "Changement de salaire : " + employee.salaireBase() + " → " + request.nouveauSalaireBase() + " Ar");
        return career(employee.id(), true, true);
    }

    private Map<String, Object> career(Integer employeeId, boolean includeSalary, boolean includeOptions) {
        EmployeeSnapshot employee = employee(employeeId, false);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("employe", employeeMap(employee, includeSalary));
        result.put("salaireVisible", includeSalary);
        result.put("situationActuelle", situation(employee, LocalDate.now(HR_ZONE), includeSalary));
        result.put("historiqueAffectations", affectationHistory(employeeId));
        if (includeSalary) result.put("historiqueSalaires", salaryHistory(employeeId));
        result.put("promotions", promotionHistory(employeeId, includeSalary));
        result.put("mutations", mutationHistory(employeeId, includeSalary));
        if (includeOptions) {
            result.put("postesDisponibles", postesDisponibles());
            result.put("typesMouvement", jdbc.queryForList("""
                SELECT code, libelle, categorie FROM types_mouvement_carriere
                WHERE actif = TRUE ORDER BY ordre, libelle
                """));
        }
        return result;
    }

    private Map<String, Object> situation(EmployeeSnapshot employee, LocalDate date, boolean includeSalary) {
        if (date.isAfter(LocalDate.now(HR_ZONE))) throw bad("La consultation ne peut pas porter sur une date future.");
        if (date.isBefore(employee.dateEmbauche())) throw bad("La date demandée précède la date d’embauche du salarié.");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("employeId", employee.id());
        result.put("matricule", employee.matricule());
        result.put("nom", employee.nom());
        result.put("date", date);
        result.put("dateEmbauche", employee.dateEmbauche());
        result.put("anciennete", seniority(employee.dateEmbauche(), date));
        result.put("salaireVisible", includeSalary);

        Map<String, Object> affectation = latestAffectation(employee.id(), date);
        if (affectation == null) {
            affectation = new LinkedHashMap<>();
            affectation.put("id", null);
            affectation.put("dateEffet", null);
            affectation.put("type", "SITUATION_ACTUELLE");
            affectation.put("typeLibelle", "Situation actuelle");
            affectation.put("motif", null);
            affectation.put("source", "SITUATION_ACTUELLE");
            affectation.put("ancienPosteId", null);
            affectation.put("ancienPoste", null);
            affectation.put("ancienDepartementId", null);
            affectation.put("ancienDepartement", null);
            affectation.put("acteur", "Système");
        } else {
            affectation.put("source", "HISTORIQUE");
        }
        affectation.put("nouveauPosteId", affectation.containsKey("nouveauPosteId")
            ? affectation.get("nouveauPosteId") : employee.posteId());
        affectation.put("nouveauPoste", affectation.containsKey("nouveauPoste")
            ? affectation.get("nouveauPoste") : employee.poste());
        affectation.put("nouveauDepartementId", affectation.containsKey("nouveauDepartementId")
            ? affectation.get("nouveauDepartementId") : employee.departementId());
        affectation.put("nouveauDepartement", affectation.containsKey("nouveauDepartement")
            ? affectation.get("nouveauDepartement") : employee.departement());
        result.put("affectation", affectation);
        result.put("posteId", affectation.get("nouveauPosteId"));
        result.put("poste", affectation.get("nouveauPoste"));
        result.put("departementId", affectation.get("nouveauDepartementId"));
        result.put("departement", affectation.get("nouveauDepartement"));

        Map<String, Object> statut = latestStatus(employee.id(), date);
        result.put("statutCode", statut == null ? employee.statutCode() : statut.get("code"));
        result.put("statut", statut == null ? employee.statut() : statut.get("libelle"));

        if (includeSalary) {
            Map<String, Object> remuneration = latestSalaire(employee.id(), date);
            if (remuneration == null) {
                remuneration = new LinkedHashMap<>();
                remuneration.put("id", null);
                remuneration.put("dateEffet", null);
                remuneration.put("type", "SITUATION_ACTUELLE");
                remuneration.put("typeLibelle", "Situation actuelle");
                remuneration.put("motif", null);
                remuneration.put("source", "SITUATION_ACTUELLE");
                remuneration.put("ancienSalaireBase", null);
                remuneration.put("nouveauSalaireBase", employee.salaireBase());
                remuneration.put("contratNumero", employee.contratNumero());
                remuneration.put("acteur", "Système");
            } else {
                remuneration.put("source", "HISTORIQUE");
            }
            result.put("salaireBase", remuneration.get("nouveauSalaireBase"));
            result.put("remuneration", remuneration);
        }
        return result;
    }

    private List<Map<String, Object>> directory(String query, Integer departmentId, boolean includeSalary) {
        StringBuilder where = new StringBuilder(" WHERE TRUE ");
        List<Object> args = new ArrayList<>();
        if (departmentId != null) {
            where.append(" AND d.id = ? ");
            args.add(departmentId);
        }
        if (query != null && !query.isBlank()) {
            String term = "%" + query.trim().toLowerCase(java.util.Locale.ROOT) + "%";
            where.append(" AND (LOWER(u.full_name) LIKE ? OR LOWER(u.username) LIKE ? OR LOWER(e.matricule) LIKE ?) ");
            args.add(term);
            args.add(term);
            args.add(term);
        }
        List<Map<String, Object>> rows = jdbc.queryForList("""
            SELECT e.id, e.matricule, COALESCE(u.full_name, u.username) AS nom, e.date_embauche,
                   p.id AS poste_id, p.intitule AS poste, d.id AS departement_id, d.nom AS departement,
                   COALESCE(sa.code, 'ACTIF') AS statut_code, COALESCE(sa.libelle, 'Actif') AS statut,
                   contrat.salaire_base_mensuel AS salaire_base,
                   (SELECT MAX(h.date_effet) FROM historique_affectation_employe h WHERE h.employe_id = e.id) AS derniere_affectation,
                   (SELECT MAX(h.date_effet) FROM historique_salaire_employe h WHERE h.employe_id = e.id) AS dernier_salaire
            FROM employes e
            JOIN users u ON u.id = e.user_id
            JOIN postes p ON p.id = e.poste_id
            JOIN departements d ON d.id = p.departement_id
            LEFT JOIN situation_administrative_employe situation ON situation.employe_id = e.id
            LEFT JOIN statuts_administratifs_employe sa ON sa.id = situation.statut_id
            LEFT JOIN LATERAL (
                SELECT ct.salaire_base_mensuel
                FROM candidatures c
                JOIN contrats ct ON ct.candidature_id = c.id
                JOIN statuts_contrat sc ON sc.id = ct.statut_id
                WHERE c.candidat_id = e.user_id AND sc.code = 'ACTIF'
                ORDER BY ct.date_debut DESC, ct.id DESC
                LIMIT 1
            ) contrat ON TRUE
            """ + where + " ORDER BY u.full_name, e.matricule", args.toArray());
        if (!includeSalary) {
            for (Map<String, Object> row : rows) {
                row.remove("salaire_base");
                row.remove("dernier_salaire");
            }
        }
        return rows;
    }

    private Map<String, Object> employeeMap(EmployeeSnapshot employee, boolean includeSalary) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", employee.id());
        map.put("matricule", employee.matricule());
        map.put("nom", employee.nom());
        map.put("dateEmbauche", employee.dateEmbauche());
        map.put("anciennete", seniority(employee.dateEmbauche(), LocalDate.now(HR_ZONE)));
        map.put("posteId", employee.posteId());
        map.put("poste", employee.poste());
        map.put("departementId", employee.departementId());
        map.put("departement", employee.departement());
        map.put("statutCode", employee.statutCode());
        map.put("statut", employee.statut());
        if (includeSalary) {
            map.put("salaireBase", employee.salaireBase());
            map.put("contratNumero", employee.contratNumero());
        }
        return map;
    }

    private List<Map<String, Object>> affectationHistory(Integer employeeId) {
        return jdbc.query("""
            SELECT h.id, h.date_effet, h.motif,
                   t.code AS type_code, t.libelle AS type_libelle,
                   h.ancien_poste_id, oldp.intitule AS ancien_poste,
                   COALESCE(h.ancien_departement_id, oldp.departement_id) AS ancien_departement_id,
                   oldd.nom AS ancien_departement,
                   h.nouveau_poste_id, newp.intitule AS nouveau_poste,
                   COALESCE(h.nouveau_departement_id, newp.departement_id) AS nouveau_departement_id,
                   newd.nom AS nouveau_departement,
                   COALESCE(u.full_name, u.username) AS acteur
            FROM historique_affectation_employe h
            JOIN types_mouvement_carriere t ON t.id = h.type_mouvement_id
            LEFT JOIN postes oldp ON oldp.id = h.ancien_poste_id
            LEFT JOIN departements oldd ON oldd.id = COALESCE(h.ancien_departement_id, oldp.departement_id)
            JOIN postes newp ON newp.id = h.nouveau_poste_id
            JOIN departements newd ON newd.id = COALESCE(h.nouveau_departement_id, newp.departement_id)
            LEFT JOIN users u ON u.id = h.acteur_id
            WHERE h.employe_id = ?
            ORDER BY h.date_effet DESC, h.id DESC
            """, (rs, rowNum) -> {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", rs.getInt("id"));
                row.put("dateEffet", rs.getDate("date_effet").toLocalDate());
                row.put("motif", rs.getString("motif"));
                row.put("type", rs.getString("type_code"));
                row.put("typeLibelle", rs.getString("type_libelle"));
                row.put("ancienPosteId", rs.getObject("ancien_poste_id") == null ? null : rs.getInt("ancien_poste_id"));
                row.put("ancienPoste", rs.getString("ancien_poste"));
                row.put("ancienDepartementId", rs.getObject("ancien_departement_id") == null ? null : rs.getInt("ancien_departement_id"));
                row.put("ancienDepartement", rs.getString("ancien_departement"));
                row.put("nouveauPosteId", rs.getInt("nouveau_poste_id"));
                row.put("nouveauPoste", rs.getString("nouveau_poste"));
                row.put("nouveauDepartementId", rs.getInt("nouveau_departement_id"));
                row.put("nouveauDepartement", rs.getString("nouveau_departement"));
                row.put("acteur", rs.getString("acteur") == null ? "Système" : rs.getString("acteur"));
                return row;
            }, employeeId);
    }

    private Map<String, Object> latestAffectation(Integer employeeId, LocalDate date) {
        List<Map<String, Object>> rows = jdbc.query("""
            SELECT h.id, h.date_effet, h.motif,
                   t.code AS type_code, t.libelle AS type_libelle,
                   h.ancien_poste_id, oldp.intitule AS ancien_poste,
                   COALESCE(h.ancien_departement_id, oldp.departement_id) AS ancien_departement_id,
                   oldd.nom AS ancien_departement,
                   h.nouveau_poste_id, newp.intitule AS nouveau_poste,
                   COALESCE(h.nouveau_departement_id, newp.departement_id) AS nouveau_departement_id,
                   newd.nom AS nouveau_departement,
                   COALESCE(u.full_name, u.username) AS acteur
            FROM historique_affectation_employe h
            JOIN types_mouvement_carriere t ON t.id = h.type_mouvement_id
            LEFT JOIN postes oldp ON oldp.id = h.ancien_poste_id
            LEFT JOIN departements oldd ON oldd.id = COALESCE(h.ancien_departement_id, oldp.departement_id)
            JOIN postes newp ON newp.id = h.nouveau_poste_id
            JOIN departements newd ON newd.id = COALESCE(h.nouveau_departement_id, newp.departement_id)
            LEFT JOIN users u ON u.id = h.acteur_id
            WHERE h.employe_id = ? AND h.date_effet <= ?
            ORDER BY h.date_effet DESC, h.id DESC
            LIMIT 1
            """, (rs, rowNum) -> {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", rs.getInt("id"));
                row.put("dateEffet", rs.getDate("date_effet").toLocalDate());
                row.put("motif", rs.getString("motif"));
                row.put("type", rs.getString("type_code"));
                row.put("typeLibelle", rs.getString("type_libelle"));
                row.put("ancienPosteId", rs.getObject("ancien_poste_id") == null ? null : rs.getInt("ancien_poste_id"));
                row.put("ancienPoste", rs.getString("ancien_poste"));
                row.put("ancienDepartementId", rs.getObject("ancien_departement_id") == null ? null : rs.getInt("ancien_departement_id"));
                row.put("ancienDepartement", rs.getString("ancien_departement"));
                row.put("nouveauPosteId", rs.getInt("nouveau_poste_id"));
                row.put("nouveauPoste", rs.getString("nouveau_poste"));
                row.put("nouveauDepartementId", rs.getInt("nouveau_departement_id"));
                row.put("nouveauDepartement", rs.getString("nouveau_departement"));
                row.put("acteur", rs.getString("acteur") == null ? "Système" : rs.getString("acteur"));
                return row;
            }, employeeId, date);
        return rows.isEmpty() ? null : rows.getFirst();
    }

    private List<Map<String, Object>> salaryHistory(Integer employeeId) {
        return jdbc.query("""
            SELECT h.id, h.date_effet, h.motif, h.ancien_salaire_base, h.nouveau_salaire_base,
                   t.code AS type_code, t.libelle AS type_libelle,
                   ct.numero AS contrat_numero,
                   COALESCE(u.full_name, u.username) AS acteur
            FROM historique_salaire_employe h
            JOIN types_mouvement_carriere t ON t.id = h.type_mouvement_id
            LEFT JOIN contrats ct ON ct.id = h.contrat_id
            LEFT JOIN users u ON u.id = h.acteur_id
            WHERE h.employe_id = ?
            ORDER BY h.date_effet DESC, h.id DESC
            """, (rs, rowNum) -> salaryRow(rs), employeeId);
    }

    private Map<String, Object> latestSalaire(Integer employeeId, LocalDate date) {
        List<Map<String, Object>> rows = jdbc.query("""
            SELECT h.id, h.date_effet, h.motif, h.ancien_salaire_base, h.nouveau_salaire_base,
                   t.code AS type_code, t.libelle AS type_libelle,
                   ct.numero AS contrat_numero,
                   COALESCE(u.full_name, u.username) AS acteur
            FROM historique_salaire_employe h
            JOIN types_mouvement_carriere t ON t.id = h.type_mouvement_id
            LEFT JOIN contrats ct ON ct.id = h.contrat_id
            LEFT JOIN users u ON u.id = h.acteur_id
            WHERE h.employe_id = ? AND h.date_effet <= ?
            ORDER BY h.date_effet DESC, h.id DESC
            LIMIT 1
            """, (rs, rowNum) -> salaryRow(rs), employeeId, date);
        return rows.isEmpty() ? null : rows.getFirst();
    }

    private Map<String, Object> salaryRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", rs.getInt("id"));
        row.put("dateEffet", rs.getDate("date_effet").toLocalDate());
        row.put("motif", rs.getString("motif"));
        row.put("type", rs.getString("type_code"));
        row.put("typeLibelle", rs.getString("type_libelle"));
        row.put("ancienSalaireBase", rs.getBigDecimal("ancien_salaire_base"));
        row.put("nouveauSalaireBase", rs.getBigDecimal("nouveau_salaire_base"));
        row.put("contratNumero", rs.getString("contrat_numero"));
        row.put("acteur", rs.getString("acteur") == null ? "Système" : rs.getString("acteur"));
        return row;
    }

    private List<Map<String, Object>> promotionHistory(Integer employeeId, boolean includeSalary) {
        List<Map<String, Object>> rows = jdbc.query("""
            SELECT p.id, p.date_effet, p.motif, p.ancien_salaire_base, p.nouveau_salaire_base,
                   oldp.intitule AS ancien_poste, newp.intitule AS nouveau_poste, d.nom AS departement,
                   COALESCE(u.full_name, u.username) AS decide_par
            FROM promotions p
            JOIN postes oldp ON oldp.id = p.ancien_poste_id
            JOIN postes newp ON newp.id = p.nouveau_poste_id
            JOIN departements d ON d.id = p.departement_id
            LEFT JOIN users u ON u.id = p.decide_par_id
            WHERE p.employe_id = ?
            ORDER BY p.date_effet DESC, p.id DESC
            """, (rs, rowNum) -> {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", rs.getInt("id"));
                row.put("dateEffet", rs.getDate("date_effet").toLocalDate());
                row.put("motif", rs.getString("motif"));
                row.put("ancienPoste", rs.getString("ancien_poste"));
                row.put("nouveauPoste", rs.getString("nouveau_poste"));
                row.put("departement", rs.getString("departement"));
                row.put("ancienSalaireBase", rs.getBigDecimal("ancien_salaire_base"));
                row.put("nouveauSalaireBase", rs.getBigDecimal("nouveau_salaire_base"));
                row.put("decidePar", rs.getString("decide_par") == null ? "Système" : rs.getString("decide_par"));
                return row;
            }, employeeId);
        if (!includeSalary) {
            for (Map<String, Object> row : rows) {
                row.remove("ancienSalaireBase");
                row.remove("nouveauSalaireBase");
            }
        }
        return rows;
    }

    private List<Map<String, Object>> mutationHistory(Integer employeeId, boolean includeSalary) {
        List<Map<String, Object>> rows = jdbc.query("""
            SELECT m.id, m.date_effet, m.motif, m.ancien_salaire_base, m.nouveau_salaire_base,
                   oldp.intitule AS ancien_poste, newp.intitule AS nouveau_poste,
                   oldd.nom AS ancien_departement, newd.nom AS nouveau_departement,
                   COALESCE(u.full_name, u.username) AS decide_par
            FROM mutations m
            JOIN postes oldp ON oldp.id = m.ancien_poste_id
            JOIN postes newp ON newp.id = m.nouveau_poste_id
            JOIN departements oldd ON oldd.id = m.ancien_departement_id
            JOIN departements newd ON newd.id = m.nouveau_departement_id
            LEFT JOIN users u ON u.id = m.decide_par_id
            WHERE m.employe_id = ?
            ORDER BY m.date_effet DESC, m.id DESC
            """, (rs, rowNum) -> {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", rs.getInt("id"));
                row.put("dateEffet", rs.getDate("date_effet").toLocalDate());
                row.put("motif", rs.getString("motif"));
                row.put("ancienPoste", rs.getString("ancien_poste"));
                row.put("nouveauPoste", rs.getString("nouveau_poste"));
                row.put("ancienDepartement", rs.getString("ancien_departement"));
                row.put("nouveauDepartement", rs.getString("nouveau_departement"));
                row.put("ancienSalaireBase", rs.getBigDecimal("ancien_salaire_base"));
                row.put("nouveauSalaireBase", rs.getBigDecimal("nouveau_salaire_base"));
                row.put("decidePar", rs.getString("decide_par") == null ? "Système" : rs.getString("decide_par"));
                return row;
            }, employeeId);
        if (!includeSalary) {
            for (Map<String, Object> row : rows) {
                row.remove("ancienSalaireBase");
                row.remove("nouveauSalaireBase");
            }
        }
        return rows;
    }

    private Map<String, Object> latestStatus(Integer employeeId, LocalDate date) {
        List<Map<String, Object>> rows = jdbc.query("""
            SELECT s.code, s.libelle, h.date_effet, h.motif
            FROM historique_statut_employe h
            JOIN statuts_administratifs_employe s ON s.id = h.statut_id
            WHERE h.employe_id = ? AND h.date_effet <= ?
            ORDER BY h.date_effet DESC, h.id DESC
            LIMIT 1
            """, (rs, rowNum) -> {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("code", rs.getString("code"));
                row.put("libelle", rs.getString("libelle"));
                row.put("dateEffet", rs.getDate("date_effet").toLocalDate());
                row.put("motif", rs.getString("motif"));
                return row;
            }, employeeId, date);
        return rows.isEmpty() ? null : rows.getFirst();
    }

    private List<Map<String, Object>> postesDisponibles() {
        return jdbc.queryForList("""
            SELECT p.id, p.intitule, p.departement_id AS departement_id, d.nom AS departement,
                   p.salaire_base_min, p.salaire_base_max
            FROM postes p
            JOIN departements d ON d.id = p.departement_id
            WHERE p.actif = TRUE AND d.actif = TRUE
            ORDER BY d.nom, p.intitule
            """);
    }

    private EmployeeSnapshot employee(Integer employeeId, boolean forUpdate) {
        if (forUpdate) {
            Integer locked = jdbc.query("SELECT id FROM employes WHERE id = ? FOR UPDATE",
                rs -> rs.next() ? rs.getInt(1) : null, employeeId);
            if (locked == null) throw notFound("Fiche salarié introuvable.");
        }
        List<EmployeeSnapshot> rows = jdbc.query(EMPLOYEE_SELECT + " WHERE e.id = ?", (rs, rowNum) -> new EmployeeSnapshot(
            rs.getInt("id"), rs.getInt("user_id"), rs.getString("matricule"), rs.getString("nom"),
            rs.getDate("date_embauche").toLocalDate(), rs.getInt("poste_id"), rs.getInt("departement_id"),
            rs.getString("poste"), rs.getString("departement"), rs.getString("statut_code"), rs.getString("statut"),
            rs.getDate("derniere_affectation") == null ? null : rs.getDate("derniere_affectation").toLocalDate(),
            rs.getDate("dernier_salaire") == null ? null : rs.getDate("dernier_salaire").toLocalDate(),
            rs.getBigDecimal("salaire_base"),
            rs.getObject("contrat_id") == null ? null : rs.getInt("contrat_id"),
            rs.getString("contrat_numero")), employeeId);
        if (rows.isEmpty()) throw notFound("Fiche salarié introuvable.");
        return rows.getFirst();
    }

    private PostSnapshot post(Integer postId) {
        List<PostSnapshot> rows = jdbc.query("""
            SELECT p.id, p.departement_id, d.nom AS departement, p.intitule,
                   p.salaire_base_min, p.salaire_base_max, p.actif
            FROM postes p
            JOIN departements d ON d.id = p.departement_id
            WHERE p.id = ?
            """, (rs, rowNum) -> new PostSnapshot(rs.getInt("id"), rs.getInt("departement_id"),
                rs.getString("departement"), rs.getString("intitule"), rs.getBigDecimal("salaire_base_min"),
                rs.getBigDecimal("salaire_base_max"), rs.getBoolean("actif")), postId);
        if (rows.isEmpty()) throw notFound("Poste introuvable.");
        PostSnapshot post = rows.getFirst();
        if (!post.actif()) throw bad("Le poste sélectionné est inactif.");
        return post;
    }

    private Integer insertAffectation(EmployeeSnapshot employee, PostSnapshot target, String type,
            String provenance, LocalDate date, String motif, Integer actorId) {
        return jdbc.queryForObject("""
            INSERT INTO historique_affectation_employe
                (employe_id, ancien_poste_id, nouveau_poste_id, ancien_departement_id, nouveau_departement_id,
                 provenance_id, type_mouvement_id, date_effet, motif, acteur_id)
            VALUES (?, ?, ?, ?, ?, (SELECT id FROM provenances_employe WHERE code = ?),
                    (SELECT id FROM types_mouvement_carriere WHERE code = ?), ?, ?, ?)
            RETURNING id
            """, Integer.class, employee.id(), employee.posteId(), target.id(), employee.departementId(),
            target.departementId(), provenance, type, date, motif, actorId);
    }

    private Integer insertSalaire(EmployeeSnapshot employee, BigDecimal newSalary, String type,
            LocalDate date, String motif, Integer actorId) {
        return jdbc.queryForObject("""
            INSERT INTO historique_salaire_employe
                (employe_id, ancien_salaire_base, nouveau_salaire_base, type_mouvement_id, contrat_id,
                 date_effet, motif, acteur_id)
            VALUES (?, ?, ?, (SELECT id FROM types_mouvement_carriere WHERE code = ?), ?, ?, ?, ?)
            RETURNING id
            """, Integer.class, employee.id(), employee.salaireBase(), newSalary, type,
            employee.contratId(), date, motif, actorId);
    }

    private void updateEmployeePost(Integer employeeId, Integer postId, String provenance) {
        jdbc.update("""
            UPDATE employes
            SET poste_id = ?, provenance_id = (SELECT id FROM provenances_employe WHERE code = ?),
                updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """, postId, provenance, employeeId);
    }

    private void updateContractSalary(Integer contractId, BigDecimal salary) {
        int changed = jdbc.update("UPDATE contrats SET salaire_base_mensuel = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
            salary, contractId);
        if (changed != 1) throw conflict("Le contrat actif du salarié est introuvable.");
    }

    private void recordMovement(EmployeeSnapshot employee, Integer actorId, LocalDate date, String motif,
            Integer objectId, String summary) {
        String safeSummary = truncate(summary, 300);
        jdbc.update("""
            INSERT INTO evenements_dossier_employe
                (employe_id, type_evenement_id, acteur_id, objet_type, objet_id, date_effet, motif, resume)
            SELECT ?, t.id, ?, 'CARRIERE', ?, ?, ?, ?
            FROM types_evenements_dossier t
            WHERE t.code = 'CARRIERE_EVENEMENT'
            """, employee.id(), actorId, objectId, date, motif, safeSummary);
        notifications.notifyUser(employee.userId(), "CARRIERE_STATUT", "Mise à jour de votre carrière",
            safeSummary, "/espace/employe/carriere");
    }

    private void ensureChangeable(EmployeeSnapshot employee) {
        if ("SORTI".equalsIgnoreCase(employee.statutCode()))
            throw conflict("Un salarié sorti ne peut plus faire l’objet d’un mouvement de carrière.");
    }

    private void validateEffectiveDate(EmployeeSnapshot employee, LocalDate date,
            boolean affectation, boolean salaire) {
        if (date == null) throw bad("La date d’effet est obligatoire.");
        LocalDate today = LocalDate.now(HR_ZONE);
        if (date.isAfter(today)) throw bad("La date d’effet ne peut pas être dans le futur.");
        if (date.isBefore(employee.dateEmbauche()))
            throw bad("La date d’effet ne peut pas précéder la date d’embauche.");
        if (affectation && employee.derniereAffectation() != null && !date.isAfter(employee.derniereAffectation()))
            throw bad("La date d’effet doit être postérieure au dernier mouvement d’affectation ("
                + employee.derniereAffectation() + ").");
        if (salaire && employee.dernierSalaire() != null && !date.isAfter(employee.dernierSalaire()))
            throw bad("La date d’effet doit être postérieure au dernier mouvement de salaire ("
                + employee.dernierSalaire() + ").");
    }

    private void validateSalaryRange(PostSnapshot post, BigDecimal salary) {
        if (salary == null || salary.signum() <= 0) throw bad("Le salaire de base doit être positif.");
        if (salary.compareTo(post.salaireMin()) < 0 || salary.compareTo(post.salaireMax()) > 0)
            throw bad("Le salaire de base doit rester dans la fourchette du poste « " + post.intitule()
                + " » (" + post.salaireMin() + " à " + post.salaireMax() + " Ar).");
    }

    private String cleanMotif(String motif) {
        if (motif == null || motif.isBlank()) throw bad("Le motif du mouvement est obligatoire.");
        String cleaned = motif.trim();
        if (cleaned.length() > 2000) throw bad("Le motif ne peut pas dépasser 2000 caractères.");
        return cleaned;
    }

    private LocalDate consultationDate(LocalDate date) {
        LocalDate value = date == null ? LocalDate.now(HR_ZONE) : date;
        if (value.isAfter(LocalDate.now(HR_ZONE))) throw bad("La consultation ne peut pas porter sur une date future.");
        return value;
    }

    private String seniority(LocalDate hired, LocalDate reference) {
        if (hired == null || hired.isAfter(reference)) return "—";
        Period period = Period.between(hired, reference);
        if (period.getYears() == 0 && period.getMonths() == 0) return period.getDays() + " j";
        StringBuilder value = new StringBuilder(period.getYears() + " an" + (period.getYears() > 1 ? "s" : ""));
        if (period.getMonths() > 0) value.append(' ').append(period.getMonths()).append(" mois");
        return value.toString();
    }

    private String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max - 1) + "…";
    }

    private Integer employeeIdForUser(Integer userId) {
        Integer id = jdbc.query("SELECT id FROM employes WHERE user_id = ?",
            rs -> rs.next() ? rs.getInt(1) : null, userId);
        if (id == null) throw notFound("Aucune fiche salarié n’est associée à ce compte.");
        return id;
    }

    private Integer managerDepartment(Integer managerId) {
        requireActiveRole(managerId, "manager");
        List<Integer> departments = jdbc.queryForList("""
            SELECT DISTINCT p.departement_id
            FROM employes e
            JOIN postes p ON p.id = e.poste_id
            JOIN departements d ON d.id = p.departement_id
            WHERE e.user_id = ? AND p.actif = TRUE AND d.actif = TRUE
            """, Integer.class, managerId);
        if (departments.size() != 1)
            throw conflict("Le compte manager doit être rattaché à un seul département actif.");
        return departments.getFirst();
    }

    private void ensureManagerScope(Integer managerId, EmployeeSnapshot employee) {
        Integer departmentId = managerDepartment(managerId);
        if (!Objects.equals(departmentId, employee.departementId()))
            throw forbidden("Ce salarié n’est pas rattaché à votre département.");
    }

    private void requireActiveRole(Integer userId, String role) {
        Boolean active = jdbc.queryForObject("""
            SELECT EXISTS (
                SELECT 1 FROM users u
                JOIN user_roles ur ON ur.user_id = u.id
                JOIN roles r ON r.id = ur.role_id
                JOIN statuts_validation_compte v ON v.id = u.administrative_validation_status_id
                WHERE u.id = ? AND u.is_suspended = FALSE AND v.code = 'APPROVED' AND LOWER(r.role_name) = ?
            )
            """, Boolean.class, userId, role);
        if (!Boolean.TRUE.equals(active)) throw forbidden("Un compte " + role + " actif est requis.");
    }

    private void requireActiveEmployeeAccount(Integer userId) {
        Boolean active = jdbc.queryForObject("""
            SELECT EXISTS (
                SELECT 1 FROM users u
                JOIN statuts_validation_compte v ON v.id = u.administrative_validation_status_id
                WHERE u.id = ? AND u.is_suspended = FALSE AND v.code = 'APPROVED'
                  AND EXISTS (
                      SELECT 1 FROM user_roles ur
                      JOIN roles r ON r.id = ur.role_id
                      WHERE ur.user_id = u.id AND LOWER(r.role_name) IN ('employe', 'manager')
                  )
            )
            """, Boolean.class, userId);
        if (!Boolean.TRUE.equals(active)) throw forbidden("Un compte salarié actif est requis.");
    }

    private static ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private static ResponseStatusException forbidden(String message) {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, message);
    }

    private static ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }

    private static ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }

    private record EmployeeSnapshot(
        Integer id,
        Integer userId,
        String matricule,
        String nom,
        LocalDate dateEmbauche,
        Integer posteId,
        Integer departementId,
        String poste,
        String departement,
        String statutCode,
        String statut,
        LocalDate derniereAffectation,
        LocalDate dernierSalaire,
        BigDecimal salaireBase,
        Integer contratId,
        String contratNumero
    ) { }

    private record PostSnapshot(
        Integer id,
        Integer departementId,
        String departement,
        String intitule,
        BigDecimal salaireMin,
        BigDecimal salaireMax,
        boolean actif
    ) { }
}
