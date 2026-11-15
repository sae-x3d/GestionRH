package com.sirh.backend.modules.temps.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.sirh.backend.modules.notifications.service.NotificationCenterService;
import com.sirh.backend.modules.temps.dto.BulkPlanningAssignmentRequest;
import com.sirh.backend.modules.temps.dto.PunchActionRequest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TimeManagementService {
    private static final ZoneId HR_ZONE = ZoneId.of("Indian/Antananarivo");
    private final JdbcTemplate jdbc;
    private final NotificationCenterService notifications;

    public TimeManagementService(JdbcTemplate jdbc, NotificationCenterService notifications) {
        this.jdbc = jdbc;
        this.notifications = notifications;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> employeeWeek(Integer userId, LocalDate week) {
        LocalDate monday = monday(week);
        Integer employeeId = employeeId(userId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("semaineDebut", monday);
        result.put("semaineFin", monday.plusDays(6));
        result.put("horairesParDefaut", defaultSchedule());
        result.put("jours", jdbc.queryForList("""
            SELECT COALESCE(j.id, -a.id) AS id, cal.date_travail::date AS date,
                   CASE WHEN a.id IS NOT NULL THEN CASE WHEN a.demande_conge_id IS NOT NULL THEN 'CONGE' ELSE 'ABSENCE' END ELSE tj.code END AS type_code,
                   COALESCE(taffiche.libelle, tj.libelle) AS type_jour,
                   CASE WHEN a.id IS NULL THEN j.heure_debut END AS debut,
                   CASE WHEN a.id IS NULL THEN j.heure_fin END AS fin,
                   CASE WHEN a.id IS NULL THEN j.pause_debut END AS pauseDebut,
                   CASE WHEN a.id IS NULL THEN j.pause_fin END AS pauseFin,
                   COALESCE(a.motif, j.observation) AS observation,
                   (SELECT json_agg(json_build_object('type', tep.code, 'libelle', tep.libelle, 'horodatage', ep.horodatage)
                                    ORDER BY ep.horodatage)
                    FROM sessions_pointage sp JOIN evenements_pointage ep ON ep.session_id = sp.id
                    JOIN types_evenement_pointage tep ON tep.id = ep.type_id
                    WHERE sp.employe_id = ? AND sp.date_service = cal.date_travail) AS pointages
            FROM generate_series(?::date, ?::date, INTERVAL '1 day') AS cal(date_travail)
            LEFT JOIN jours_planning_employe j ON j.employe_id = ? AND j.date_travail = cal.date_travail
            LEFT JOIN plannings_equipe pe ON pe.id = j.planning_id
            LEFT JOIN statuts_planning_equipe ps ON ps.id = pe.statut_id AND ps.code = 'PUBLIE'
            LEFT JOIN types_jour_planning tj ON tj.id = j.type_jour_id AND ps.id IS NOT NULL
            LEFT JOIN LATERAL (
                SELECT ab.id, ab.demande_conge_id, ab.motif
                FROM absences ab JOIN types_absence ta ON ta.id = ab.type_absence_id AND ta.impacte_planning=TRUE
                WHERE ab.employe_id=?
                  AND (ab.date_debut AT TIME ZONE 'Indian/Antananarivo')::date <= cal.date_travail
                  AND (ab.date_fin AT TIME ZONE 'Indian/Antananarivo')::date > cal.date_travail
                  AND (
                    EXISTS (SELECT 1 FROM jours_planning_employe jp
                        JOIN types_jour_planning jpt ON jpt.id=jp.type_jour_id
                        WHERE jp.employe_id=ab.employe_id AND jp.date_travail=cal.date_travail
                          AND jpt.code IN ('TRAVAIL','CONGE','ABSENCE'))
                    OR (NOT EXISTS (SELECT 1 FROM jours_planning_employe jp
                            WHERE jp.employe_id=ab.employe_id AND jp.date_travail=cal.date_travail)
                        AND EXISTS (SELECT 1 FROM jours_semaine js WHERE js.numero_iso=EXTRACT(ISODOW FROM cal.date_travail)
                            AND js.actif=TRUE AND js.ouvrable_par_defaut=TRUE)
                        AND NOT EXISTS (SELECT 1 FROM jours_feries jf WHERE jf.date_ferie=cal.date_travail AND jf.actif=TRUE))
                  )
                ORDER BY ab.id DESC LIMIT 1
            ) a ON TRUE
            LEFT JOIN types_jour_planning taffiche ON taffiche.code = CASE
                WHEN a.id IS NOT NULL AND a.demande_conge_id IS NOT NULL THEN 'CONGE'
                WHEN a.id IS NOT NULL THEN 'ABSENCE' END
            WHERE ps.id IS NOT NULL OR a.id IS NOT NULL
            ORDER BY cal.date_travail
            """, employeeId, monday, monday.plusDays(6), employeeId, employeeId));
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> managerWeek(Integer userId, LocalDate week) {
        Integer departmentId = managerDepartment(userId);
        return weekView(departmentId, monday(week), true);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> globalWeek(Integer userId, String role, LocalDate week) {
        requireRole(userId, role);
        LocalDate monday = monday(week);
        List<Map<String, Object>> departments = jdbc.queryForList("""
            SELECT d.id, d.code, d.nom FROM departements d WHERE d.actif = TRUE ORDER BY d.nom
            """);
        List<Map<String, Object>> teams = new ArrayList<>();
        for (Map<String, Object> department : departments) {
            teams.add(weekView(((Number) department.get("id")).intValue(), monday, false));
        }
        return Map.of("semaineDebut", monday, "semaineFin", monday.plusDays(6), "departements", teams);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> presence(Integer userId, String role, LocalDate requestedDate) {
        Integer departmentId = null;
        if ("manager".equals(role)) departmentId = managerDepartment(userId);
        else requireRole(userId, role);
        LocalDate date = requestedDate == null ? LocalDate.now(HR_ZONE) : requestedDate;
        LocalTime now = LocalTime.now(HR_ZONE);
        int noShowGraceMinutes = ruleMinutes("TOLERANCE_RETARD");
        List<Map<String, Object>> employees = jdbc.queryForList("""
            WITH equipe AS (
                SELECT e.id AS employe_id, e.matricule, COALESCE(u.full_name, u.username) AS nom,
                       p.intitule AS poste, d.id AS departement_id, d.nom AS departement,
                       tj.code AS type_jour_code, tj.libelle AS type_jour,
                       j.heure_debut AS debut, j.heure_fin AS fin, j.pause_debut AS pauseDebut, j.pause_fin AS pauseFin,
                       sp.id AS session_id, arrivee.horodatage AS heure_arrivee,
                       debut_pause.horodatage AS debut_pause_reel, fin_pause.horodatage AS fin_pause_reel,
                       fin_service.horodatage AS heure_depart
                FROM employes e JOIN users u ON u.id = e.user_id
                JOIN statuts_validation_compte svc ON svc.id = u.administrative_validation_status_id AND svc.code = 'APPROVED'
                JOIN postes p ON p.id = e.poste_id JOIN departements d ON d.id = p.departement_id
                LEFT JOIN situation_administrative_employe sa ON sa.employe_id = e.id
                LEFT JOIN statuts_administratifs_employe se ON se.id = sa.statut_id
                LEFT JOIN jours_planning_employe j ON j.employe_id = e.id AND j.date_travail = ?
                LEFT JOIN plannings_equipe pe ON pe.id = j.planning_id
                LEFT JOIN statuts_planning_equipe ps ON ps.id = pe.statut_id AND ps.code = 'PUBLIE'
                LEFT JOIN types_jour_planning tj ON tj.id = j.type_jour_id AND ps.id IS NOT NULL
                LEFT JOIN sessions_pointage sp ON sp.employe_id = e.id AND sp.date_service = ?
                LEFT JOIN evenements_pointage arrivee ON arrivee.session_id = sp.id AND arrivee.type_id = (SELECT id FROM types_evenement_pointage WHERE code = 'ARRIVEE')
                LEFT JOIN evenements_pointage debut_pause ON debut_pause.session_id = sp.id AND debut_pause.type_id = (SELECT id FROM types_evenement_pointage WHERE code = 'DEBUT_PAUSE')
                LEFT JOIN evenements_pointage fin_pause ON fin_pause.session_id = sp.id AND fin_pause.type_id = (SELECT id FROM types_evenement_pointage WHERE code = 'FIN_PAUSE')
                LEFT JOIN evenements_pointage fin_service ON fin_service.session_id = sp.id AND fin_service.type_id = (SELECT id FROM types_evenement_pointage WHERE code = 'FIN_SERVICE')
                WHERE d.actif = TRUE AND p.actif = TRUE AND u.is_suspended = FALSE
                  AND COALESCE(se.code, 'ACTIF') = 'ACTIF'
                  AND (CAST(? AS INTEGER) IS NULL OR d.id = CAST(? AS INTEGER))
            ), codes AS (
                SELECT equipe.*,
                    CASE
                        WHEN type_jour_code = 'REPOS' THEN 'REPOS'
                        WHEN type_jour_code IS DISTINCT FROM 'TRAVAIL' THEN 'NON_PLANIFIE'
                        WHEN ? > CURRENT_DATE AND heure_arrivee IS NULL THEN 'A_VENIR'
                        WHEN heure_arrivee IS NULL AND (? > CURRENT_DATE OR (? = CURRENT_DATE AND debut IS NOT NULL AND ? > debut + (? * INTERVAL '1 minute'))) THEN 'ABSENT'
                        WHEN heure_arrivee IS NULL THEN 'EN_ATTENTE'
                        WHEN heure_depart IS NOT NULL THEN 'TERMINE'
                        WHEN debut_pause_reel IS NOT NULL AND fin_pause_reel IS NULL THEN 'EN_PAUSE'
                        ELSE 'PRESENT'
                    END AS statut_code
                FROM equipe
            )
            SELECT codes.employe_id, codes.matricule, codes.nom, codes.poste,
                   codes.departement_id, codes.departement, codes.type_jour, codes.debut, codes.fin,
                   codes.pauseDebut, codes.pauseFin, codes.heure_arrivee AS arrivee,
                   codes.debut_pause_reel AS debut_pause, codes.fin_pause_reel AS fin_pause,
                   codes.heure_depart AS depart, statut.code AS statut_code, statut.libelle AS statut,
                   (SELECT COUNT(*) FROM anomalies_pointage anomaly JOIN types_anomalie_pointage atype ON atype.id = anomaly.type_id
                    WHERE anomaly.session_id = codes.session_id AND anomaly.resolue_at IS NULL AND atype.code <> 'RAS') AS anomalies,
                   (SELECT string_agg(anomalie.libelle || ' : ' || anomaly.details, ' · ' ORDER BY anomaly.id)
                    FROM anomalies_pointage anomaly JOIN types_anomalie_pointage anomalie ON anomalie.id = anomaly.type_id
                    WHERE anomaly.session_id = codes.session_id AND anomaly.resolue_at IS NULL AND anomalie.code <> 'RAS') AS details_anomalies
            FROM codes JOIN statuts_presence_pointage statut ON statut.code = codes.statut_code
            ORDER BY codes.departement, codes.nom, codes.matricule
            """, date, date, departmentId, departmentId, date, date, date, now, noShowGraceMinutes);
        Map<String, Object> counts = new LinkedHashMap<>();
        counts.put("effectif", employees.size());
        for (Map<String, Object> status : jdbc.queryForList("SELECT code, libelle FROM statuts_presence_pointage ORDER BY ordre")) {
            String code = (String) status.get("code");
            counts.put(code, employees.stream().filter(employee -> code.equals(employee.get("statut_code"))).count());
        }
        counts.put("anomalies", employees.stream().mapToLong(employee -> ((Number) employee.get("anomalies")).longValue()).sum());
        return Map.of("date", date, "compteurs", counts, "employes", employees,
            "departement", departmentId == null ? Map.of("nom", "Tous les départements")
                : jdbc.queryForMap("SELECT id, code, nom FROM departements WHERE id = ?", departmentId));
    }

    private Map<String, Object> defaultSchedule() {
        return jdbc.queryForList("""
            SELECT heure_debut AS "heureDebut",heure_fin AS "heureFin",pause_debut AS "pauseDebut",pause_fin AS "pauseFin"
            FROM parametres_horaires_planning WHERE actif=TRUE ORDER BY id LIMIT 1
            """).stream().findFirst().orElse(Map.of());
    }

    private Map<String, Object> weekView(Integer departmentId, LocalDate monday, boolean managerView) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> department = jdbc.queryForList(
            "SELECT id, code, nom FROM departements WHERE id = ? AND actif = TRUE", departmentId);
        if (department.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Département introuvable.");
        result.put("departement", department.getFirst());
        result.put("semaineDebut", monday);
        result.put("semaineFin", monday.plusDays(6));
        List<Map<String, Object>> planning = jdbc.queryForList("""
            SELECT p.id, s.code AS statut_code, s.libelle AS statut, p.version, p.publie_at AS publieAt
            FROM plannings_equipe p JOIN statuts_planning_equipe s ON s.id = p.statut_id
            WHERE p.departement_id = ? AND p.semaine_debut = ?
            """, departmentId, monday);
        result.put("planning", planning.isEmpty() ? null : planning.getFirst());
        List<Map<String, Object>> employees = jdbc.queryForList("""
            SELECT e.id AS employe_id, e.matricule, COALESCE(u.full_name, u.username) AS nom,
                   p.intitule AS poste, d.nom AS departement
            FROM employes e JOIN users u ON u.id = e.user_id
            JOIN statuts_validation_compte svc ON svc.id = u.administrative_validation_status_id AND svc.code = 'APPROVED'
            JOIN postes p ON p.id = e.poste_id JOIN departements d ON d.id = p.departement_id
            LEFT JOIN situation_administrative_employe sa ON sa.employe_id = e.id
            LEFT JOIN statuts_administratifs_employe statut_employe ON statut_employe.id = sa.statut_id
            WHERE d.id = ? AND d.actif = TRUE AND p.actif = TRUE AND u.is_suspended = FALSE
              AND COALESCE(statut_employe.code, 'ACTIF') = 'ACTIF'
            ORDER BY nom, e.matricule
            """, departmentId);
        result.put("employes", employees);
        result.put("joursOuvrables", jdbc.queryForList(
            "SELECT numero_iso FROM jours_semaine WHERE actif=TRUE AND ouvrable_par_defaut=TRUE ORDER BY numero_iso"));
        Integer planningId = planning.isEmpty() ? null : ((Number) planning.getFirst().get("id")).intValue();
        result.put("jours", planningId == null ? List.of() : jdbc.queryForList("""
            SELECT j.id, j.employe_id AS employeId, j.date_travail AS date, tj.code AS type_code,
                   tj.libelle AS type_jour, j.heure_debut AS debut, j.heure_fin AS fin,
                   j.pause_debut AS pauseDebut, j.pause_fin AS pauseFin, j.observation,
                   (SELECT COUNT(*) FROM evenements_pointage ep
                    JOIN sessions_pointage sp ON sp.id = ep.session_id
                    JOIN types_evenement_pointage tep ON tep.id = ep.type_id
                    WHERE sp.employe_id = j.employe_id AND sp.date_service = j.date_travail
                      AND tep.code = 'ARRIVEE') AS pointe
            FROM jours_planning_employe j JOIN types_jour_planning tj ON tj.id = j.type_jour_id
            WHERE j.planning_id = ? ORDER BY j.date_travail, j.employe_id
            """, planningId));
        List<Map<String, Object>> counts = new ArrayList<>();
        for (int day = 0; day < 7; day++) {
            LocalDate date = monday.plusDays(day);
            Map<String, Object> count = jdbc.queryForMap("""
                SELECT ?::date AS date,
                    (SELECT COUNT(*) FROM employes e JOIN postes p ON p.id = e.poste_id
                     JOIN users u ON u.id = e.user_id JOIN statuts_validation_compte svc
                       ON svc.id = u.administrative_validation_status_id AND svc.code = 'APPROVED'
                     LEFT JOIN situation_administrative_employe sa ON sa.employe_id = e.id
                     LEFT JOIN statuts_administratifs_employe se ON se.id = sa.statut_id
                     WHERE p.departement_id = ? AND p.actif AND u.is_suspended = FALSE
                       AND COALESCE(se.code, 'ACTIF') = 'ACTIF') AS effectif_actif,
                    COUNT(*) FILTER (WHERE tj.code = 'TRAVAIL') AS planifies,
                    COUNT(*) FILTER (WHERE tj.code = 'REPOS') AS repos,
                    COUNT(*) FILTER (WHERE tj.code IN ('CONGE','ABSENCE')) AS absents,
                    COUNT(*) FILTER (WHERE tj.code = 'TRAVAIL' AND j.id IS NOT NULL AND EXISTS (
                        SELECT 1 FROM sessions_pointage sp JOIN evenements_pointage ep ON ep.session_id = sp.id
                        JOIN types_evenement_pointage tep ON tep.id = ep.type_id
                        WHERE sp.employe_id = j.employe_id AND sp.date_service = j.date_travail AND tep.code = 'ARRIVEE'
                    )) AS arrives
                FROM (SELECT 1) seed
                LEFT JOIN plannings_equipe pe ON pe.departement_id = ? AND pe.semaine_debut = ?
                LEFT JOIN jours_planning_employe j ON j.planning_id = pe.id AND j.date_travail = ?
                LEFT JOIN types_jour_planning tj ON tj.id = j.type_jour_id
                """, date, departmentId, departmentId, monday, date);
            int active = ((Number) count.get("effectif_actif")).intValue();
            int scheduled = ((Number) count.get("planifies")).intValue();
            int rest = ((Number) count.get("repos")).intValue();
            int absents = ((Number) count.get("absents")).intValue();
            count.put("nonPlanifies", Math.max(0, active - scheduled - rest - absents));
            counts.add(count);
        }
        result.put("compteurs", counts);
        result.put("typesJour", jdbc.queryForList("SELECT id, code, libelle FROM types_jour_planning WHERE actif = TRUE ORDER BY id"));
        result.put("horairesParDefaut", defaultSchedule());
        if (managerView) result.put("modifiable", planning.isEmpty() || "BROUILLON".equals(planning.getFirst().get("statut_code")));
        return result;
    }

    @Transactional
    public Map<String, Object> assignBulk(Integer userId, BulkPlanningAssignmentRequest request) {
        Integer departmentId = managerDepartment(userId);
        if (request.employeIds().size() > 100 || request.dates().size() > 7) {
            throw badRequest("Sélection trop grande : limite de 100 agents et 7 dates par opération.");
        }
        LocalDate week = monday(request.dates().getFirst());
        if (week.isBefore(monday(LocalDate.now(HR_ZONE)))) throw new ResponseStatusException(HttpStatus.CONFLICT, "Une semaine passée est consultable mais ne peut plus être modifiée.");
        Set<LocalDate> dates = new LinkedHashSet<>(request.dates());
        if (dates.size() != request.dates().size() || dates.stream().anyMatch(date -> date.isBefore(week) || date.isAfter(week.plusDays(6)))) {
            throw badRequest("Les dates doivent être uniques et appartenir à la même semaine.");
        }
        Set<Integer> employeeIds = new LinkedHashSet<>(request.employeIds());
        if (employeeIds.size() != request.employeIds().size()) throw badRequest("La sélection contient un agent en double.");
        String placeholders = String.join(",", java.util.Collections.nCopies(employeeIds.size(), "?"));
        List<Integer> validEmployees = employeeIds.isEmpty() ? List.of() : jdbc.queryForList("""
            SELECT e.id FROM employes e JOIN postes p ON p.id = e.poste_id
            JOIN users u ON u.id = e.user_id AND u.is_suspended = FALSE
            JOIN statuts_validation_compte svc ON svc.id = u.administrative_validation_status_id AND svc.code = 'APPROVED'
            LEFT JOIN situation_administrative_employe sa ON sa.employe_id = e.id
            LEFT JOIN statuts_administratifs_employe se ON se.id = sa.statut_id
            WHERE p.departement_id = ? AND p.actif = TRUE AND COALESCE(se.code, 'ACTIF') = 'ACTIF'
              AND e.id IN (""" + placeholders + ")", Integer.class, args(departmentId, employeeIds));
        if (employeeIds.isEmpty() || validEmployees.size() != employeeIds.size()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Un ou plusieurs agents ne relèvent pas de votre département.");
        }
        List<Map<String, Object>> dayTypes = jdbc.queryForList("SELECT code FROM types_jour_planning WHERE id = ? AND actif = TRUE", request.typeJourId());
        if (dayTypes.isEmpty()) throw badRequest("Type de journée indisponible.");
        boolean workday = "TRAVAIL".equals(dayTypes.getFirst().get("code"));
        for (Integer employeeId : employeeIds) {
            for (LocalDate date : dates) {
                if (Boolean.TRUE.equals(jdbc.queryForObject("""
                        SELECT EXISTS(SELECT 1 FROM absences a JOIN types_absence ta ON ta.id=a.type_absence_id
                          WHERE a.employe_id=? AND a.demande_conge_id IS NOT NULL AND ta.impacte_planning=TRUE
                            AND a.date_debut::date<=? AND a.date_fin::date>?)
                        """, Boolean.class, employeeId, date, date))) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Cette journée est protégée par un congé approuvé. La planification ne peut pas l’écraser.");
                }
            }
        }
        if (workday && (request.heureDebut() == null || request.heureFin() == null || !request.heureFin().isAfter(request.heureDebut()))) {
            throw badRequest("Pour un jour travaillé, indiquez une heure de début et une heure de fin valides.");
        }
        if (!workday && (request.heureDebut() != null || request.heureFin() != null || request.pauseDebut() != null || request.pauseFin() != null)) {
            throw badRequest("Un jour de repos ne peut pas contenir d’horaires de travail.");
        }
        if ((request.pauseDebut() == null) != (request.pauseFin() == null)
                || request.pauseDebut() != null && (!request.pauseDebut().isBefore(request.pauseFin())
                || request.pauseDebut().isBefore(request.heureDebut()) || request.pauseFin().isAfter(request.heureFin()))) {
            throw badRequest("Les horaires de pause sont incomplets ou incohérents avec le service.");
        }
        Integer planningId = planningForUpdate(userId, departmentId, week);
        Map<String, Object> current = jdbc.queryForMap("""
            SELECT s.code AS statut FROM plannings_equipe p JOIN statuts_planning_equipe s ON s.id = p.statut_id
            WHERE p.id = ? FOR UPDATE OF p
            """, planningId);
        if (!"BROUILLON".equals(current.get("statut"))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Le planning est publié. Rouvrez-le avant toute modification.");
        }
        Integer jourId = jdbc.queryForObject("SELECT id FROM types_jour_planning WHERE code = ?", Integer.class, dayTypes.getFirst().get("code"));
        for (Integer employeeId : employeeIds) {
            for (LocalDate date : dates) {
                List<Map<String, Object>> oldRows = jdbc.queryForList("SELECT * FROM jours_planning_employe WHERE employe_id = ? AND date_travail = ? FOR UPDATE", employeeId, date);
                if (!oldRows.isEmpty() && !oldRows.getFirst().get("planning_id").equals(planningId)) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Une affectation existe déjà dans un autre planning pour cet agent et cette date.");
                }
                Map<String, Object> old = oldRows.isEmpty() ? Map.of() : oldRows.getFirst();
                jdbc.update("""
                    INSERT INTO jours_planning_employe
                        (planning_id, employe_id, date_travail, type_jour_id, heure_debut, heure_fin,
                         pause_debut, pause_fin, observation, modifie_par_id)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (employe_id, date_travail) DO UPDATE SET
                        type_jour_id = EXCLUDED.type_jour_id, heure_debut = EXCLUDED.heure_debut,
                        heure_fin = EXCLUDED.heure_fin, pause_debut = EXCLUDED.pause_debut,
                        pause_fin = EXCLUDED.pause_fin, observation = EXCLUDED.observation,
                        modifie_par_id = EXCLUDED.modifie_par_id, version = jours_planning_employe.version + 1,
                        updated_at = CURRENT_TIMESTAMP
                    WHERE jours_planning_employe.planning_id = EXCLUDED.planning_id
                    """, planningId, employeeId, date, jourId, request.heureDebut(), request.heureFin(),
                    request.pauseDebut(), request.pauseFin(), cleanObservation(request.observation()), userId);
                Integer rowId = jdbc.queryForObject("SELECT id FROM jours_planning_employe WHERE employe_id = ? AND date_travail = ?", Integer.class, employeeId, date);
                jdbc.update("""
                    INSERT INTO historiques_jour_planning
                        (jour_planning_id, planning_id, employe_id, date_travail, ancien_type_jour_id,
                         nouveau_type_jour_id, ancienne_heure_debut, ancienne_heure_fin, ancienne_pause_debut,
                         ancienne_pause_fin, nouvelle_heure_debut, nouvelle_heure_fin, nouvelle_pause_debut,
                         nouvelle_pause_fin, observation, modifie_par_id)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, rowId, planningId, employeeId, date, old.get("type_jour_id"), jourId,
                    old.get("heure_debut"), old.get("heure_fin"), old.get("pause_debut"), old.get("pause_fin"),
                    request.heureDebut(), request.heureFin(), request.pauseDebut(), request.pauseFin(),
                    cleanObservation(request.observation()), userId);
            }
        }
        jdbc.update("UPDATE plannings_equipe SET version = version + 1, updated_at = CURRENT_TIMESTAMP WHERE id = ?", planningId);
        for (Integer employeeId : employeeIds) {
            Integer recipient = jdbc.queryForObject("SELECT user_id FROM employes WHERE id = ?", Integer.class, employeeId);
            notifications.notifyUser(recipient, "PLANNING_MODIFIE", "Votre planning a été modifié",
                "Une affectation de votre semaine du " + week + " a été mise à jour.", "/espace/employe/planning");
        }
        return managerWeek(userId, week);
    }

    @Transactional
    public Map<String, Object> publish(Integer userId, Integer planningId) {
        Integer departmentId = managerDepartment(userId);
        Map<String, Object> planning = ownPlanning(planningId, departmentId);
        if (!"BROUILLON".equals(planning.get("statut_code"))) throw badRequest("Seul un brouillon peut être publié.");
        LocalDate monday = (LocalDate) planning.get("semaine_debut");
        if (monday.isBefore(monday(LocalDate.now(HR_ZONE)))) throw new ResponseStatusException(HttpStatus.CONFLICT, "Une semaine passée ne peut pas être publiée.");
        Integer missing = jdbc.queryForObject("""
            SELECT COUNT(*) FROM employes e JOIN postes p ON p.id = e.poste_id
            JOIN users u ON u.id = e.user_id AND u.is_suspended = FALSE
            JOIN statuts_validation_compte svc ON svc.id = u.administrative_validation_status_id AND svc.code = 'APPROVED'
            LEFT JOIN situation_administrative_employe sa ON sa.employe_id = e.id
            LEFT JOIN statuts_administratifs_employe se ON se.id = sa.statut_id
            CROSS JOIN generate_series(?::date, ?::date, INTERVAL '1 day') d(date_travail)
            LEFT JOIN jours_planning_employe j ON j.employe_id = e.id AND j.date_travail = d.date_travail
            WHERE p.departement_id = ? AND p.actif AND COALESCE(se.code, 'ACTIF') = 'ACTIF' AND j.id IS NULL
            """, Integer.class, monday, monday.plusDays(6), departmentId);
        if (missing > 0) throw new ResponseStatusException(HttpStatus.CONFLICT, "Le planning est incomplet : attribuez un jour travaillé ou un repos à chaque agent pour les sept jours.");
        Integer oldStatusId = ((Number) planning.get("statut_id")).intValue();
        Integer publishedId = jdbc.queryForObject("SELECT id FROM statuts_planning_equipe WHERE code = 'PUBLIE'", Integer.class);
        jdbc.update("UPDATE plannings_equipe SET statut_id = ?, publie_par_id = ?, publie_at = CURRENT_TIMESTAMP, version = version + 1, updated_at = CURRENT_TIMESTAMP WHERE id = ?", publishedId, userId, planningId);
        jdbc.update("INSERT INTO historique_statut_planning (planning_id, ancien_statut_id, nouveau_statut_id, acteur_id, motif) VALUES (?, ?, ?, ?, ?)", planningId, oldStatusId, publishedId, userId, "Publication du planning complet");
        Set<Integer> userIds = new LinkedHashSet<>(jdbc.queryForList("""
            SELECT e.user_id FROM employes e JOIN postes p ON p.id = e.poste_id
            WHERE p.departement_id = ? AND e.id IN (SELECT employe_id FROM jours_planning_employe WHERE planning_id = ?)
            """, Integer.class, departmentId, planningId));
        for (Integer recipient : userIds) notifications.notifyUser(recipient, "PLANNING_PUBLIE", "Planning publié",
            "Le planning de votre équipe pour la semaine du " + monday + " est disponible.", "/espace/employe/planning");
        return managerWeek(userId, monday);
    }

    @Transactional
    public Map<String, Object> reopen(Integer userId, Integer planningId) {
        Integer departmentId = managerDepartment(userId);
        Map<String, Object> planning = ownPlanning(planningId, departmentId);
        if (!"PUBLIE".equals(planning.get("statut_code"))) throw badRequest("Seul un planning publié peut être rouvert.");
        if (((LocalDate) planning.get("semaine_debut")).isBefore(monday(LocalDate.now(HR_ZONE)))) throw new ResponseStatusException(HttpStatus.CONFLICT, "Un planning historique ne peut plus être rouvert.");
        Integer oldStatusId = ((Number) planning.get("statut_id")).intValue();
        Integer draftId = jdbc.queryForObject("SELECT id FROM statuts_planning_equipe WHERE code = 'BROUILLON'", Integer.class);
        jdbc.update("UPDATE plannings_equipe SET statut_id = ?, publie_par_id = NULL, publie_at = NULL, version = version + 1, updated_at = CURRENT_TIMESTAMP WHERE id = ?", draftId, planningId);
        jdbc.update("INSERT INTO historique_statut_planning (planning_id, ancien_statut_id, nouveau_statut_id, acteur_id, motif) VALUES (?, ?, ?, ?, ?)", planningId, oldStatusId, draftId, userId, "Réouverture pour correction");
        return managerWeek(userId, (LocalDate) planning.get("semaine_debut"));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> punchDay(Integer userId, LocalDate requestedDate) {
        int employeeId = employeeId(userId);
        LocalDate date = requestedDate == null ? LocalDate.now(HR_ZONE) : requestedDate;
        if (!date.equals(LocalDate.now(HR_ZONE))) throw badRequest("Le pointage est limité à la journée en cours.");
        return punchStatus(employeeId, date);
    }

    @Transactional
    public Map<String, Object> recordPunch(Integer userId, PunchActionRequest request) {
        int employeeId = employeeId(userId);
        LocalDate date = LocalDate.now(HR_ZONE);
        if (request.idempotencyKey().length() > 100 || !request.idempotencyKey().matches("[A-Za-z0-9-]{16,100}")) {
            throw badRequest("Référence de pointage invalide. Rechargez la page et réessayez.");
        }
        List<Map<String, Object>> existingRequest = jdbc.queryForList("""
            SELECT sp.id FROM evenements_pointage ep JOIN sessions_pointage sp ON sp.id = ep.session_id
            WHERE ep.empreinte_requete = ? AND sp.employe_id = ?
            """, request.idempotencyKey(), employeeId);
        if (!existingRequest.isEmpty()) return punchStatus(employeeId, date);
        String action = request.action().trim().toUpperCase(java.util.Locale.ROOT);
        List<Map<String, Object>> actionRef = jdbc.queryForList("SELECT id, ordre FROM types_evenement_pointage WHERE code = ?", action);
        if (actionRef.isEmpty()) throw badRequest("Action de pointage inconnue.");
        Integer sessionId = ensureSession(employeeId, date);
        List<Map<String, Object>> events = jdbc.queryForList("""
            SELECT t.code, t.ordre, e.horodatage FROM evenements_pointage e
            JOIN types_evenement_pointage t ON t.id = e.type_id WHERE e.session_id = ? ORDER BY t.ordre
            FOR UPDATE OF e
            """, sessionId);
        int expectedOrder = events.size() + 1;
        int receivedOrder = ((Number) actionRef.getFirst().get("ordre")).intValue();
        if (receivedOrder != expectedOrder) {
            String next = receivedOrder > expectedOrder ? (String) jdbc.queryForObject("SELECT code FROM types_evenement_pointage WHERE ordre = ?", String.class, expectedOrder) : null;
            throw new ResponseStatusException(HttpStatus.CONFLICT, next == null
                ? "Cette action a déjà été enregistrée." : "Action impossible. Enregistrez d’abord : " + next + ".");
        }
        OffsetDateTime now = OffsetDateTime.now(HR_ZONE);
        jdbc.update("""
            INSERT INTO evenements_pointage (session_id, type_id, horodatage, source, empreinte_requete)
            VALUES (?, ?, ?, 'ESPACE_EMPLOYE', ?)
            """, sessionId, actionRef.getFirst().get("id"), now, request.idempotencyKey());
        detectAnomaly(sessionId, employeeId, date, action, now);
        if ("FIN_SERVICE".equals(action)) {
            Integer problems = jdbc.queryForObject("""
                SELECT COUNT(*) FROM anomalies_pointage a JOIN types_anomalie_pointage t ON t.id = a.type_id
                WHERE a.session_id = ? AND t.code <> 'RAS'
                """, Integer.class, sessionId);
            if (problems == 0) createAnomaly(sessionId, "RAS", "Pointage complet, aucune anomalie détectée.");
        }
        return punchStatus(employeeId, date);
    }

    private void detectAnomaly(Integer sessionId, Integer employeeId, LocalDate date, String action, OffsetDateTime now) {
        List<Map<String, Object>> schedule = jdbc.queryForList("""
            SELECT t.code, j.heure_debut, j.heure_fin, j.pause_debut, j.pause_fin
            FROM jours_planning_employe j JOIN types_jour_planning t ON t.id = j.type_jour_id
            JOIN plannings_equipe pe ON pe.id = j.planning_id
            JOIN statuts_planning_equipe ps ON ps.id = pe.statut_id AND ps.code = 'PUBLIE'
            WHERE j.employe_id = ? AND j.date_travail = ?
            """, employeeId, date);
        if (schedule.isEmpty() || !"TRAVAIL".equals(schedule.getFirst().get("code"))) {
            if ("ARRIVEE".equals(action)) createAnomaly(sessionId, "HORS_PLANNING", "Arrivée enregistrée sans affectation de travail planifiée.");
            return;
        }
        Map<String, Object> shift = schedule.getFirst();
        if ("ARRIVEE".equals(action) && shift.get("heure_debut") != null) {
            LocalTime start = (LocalTime) shift.get("heure_debut");
            int tolerance = ruleMinutes("TOLERANCE_RETARD");
            if (now.toLocalTime().isAfter(start.plusMinutes(tolerance))) createAnomaly(sessionId, "RETARD", "Arrivée à " + now.toLocalTime().withSecond(0).withNano(0) + ", début prévu à " + start + ".");
        }
        if ("FIN_PAUSE".equals(action) && shift.get("pause_debut") != null && shift.get("pause_fin") != null) {
            OffsetDateTime pauseStart = jdbc.queryForObject("""
                SELECT horodatage FROM evenements_pointage ep JOIN types_evenement_pointage t ON t.id = ep.type_id
                WHERE ep.session_id = ? AND t.code = 'DEBUT_PAUSE'
                """, OffsetDateTime.class, sessionId);
            long actual = java.time.Duration.between(pauseStart, now).toMinutes();
            long planned = java.time.Duration.between((LocalTime) shift.get("pause_debut"), (LocalTime) shift.get("pause_fin")).toMinutes();
            if (actual > planned + ruleMinutes("TOLERANCE_PAUSE")) createAnomaly(sessionId, "PAUSE_DEPASSEE", "Pause de " + actual + " minutes, durée prévue " + planned + " minutes.");
        }
        if ("FIN_SERVICE".equals(action) && shift.get("heure_fin") != null) {
            LocalTime end = (LocalTime) shift.get("heure_fin");
            int tolerance = ruleMinutes("TOLERANCE_DEPART");
            if (now.toLocalTime().isBefore(end.minusMinutes(tolerance))) createAnomaly(sessionId, "DEPART_ANTICIPE", "Départ à " + now.toLocalTime().withSecond(0).withNano(0) + ", fin prévue à " + end + ".");
        }
    }

    private Map<String, Object> punchStatus(Integer employeeId, LocalDate date) {
        List<Map<String, Object>> sessions = jdbc.queryForList("SELECT id FROM sessions_pointage WHERE employe_id = ? AND date_service = ?", employeeId, date);
        Integer sessionId = sessions.isEmpty() ? null : ((Number) sessions.getFirst().get("id")).intValue();
        List<Map<String, Object>> events = sessionId == null ? List.of() : jdbc.queryForList("""
            SELECT t.code, t.libelle, e.horodatage FROM evenements_pointage e
            JOIN types_evenement_pointage t ON t.id = e.type_id WHERE e.session_id = ? ORDER BY t.ordre
            """, sessionId);
        List<Map<String, Object>> anomalies = sessionId == null ? List.of() : jdbc.queryForList("""
            SELECT t.code, t.libelle, niveau.code AS niveau, a.details, a.detectee_at AS detecteeAt
            FROM anomalies_pointage a JOIN types_anomalie_pointage t ON t.id = a.type_id
            JOIN niveaux_anomalie_pointage niveau ON niveau.id = t.niveau_id
            WHERE a.session_id = ? ORDER BY a.id
            """, sessionId);
        Integer nextOrder = events.size() + 1;
        List<Map<String, Object>> next = nextOrder <= 4 ? jdbc.queryForList("SELECT code, libelle FROM types_evenement_pointage WHERE ordre = ?", nextOrder) : List.of();
        List<Map<String, Object>> planned = jdbc.queryForList("""
            SELECT tj.code AS type_code, tj.libelle AS type_jour, j.heure_debut AS debut, j.heure_fin AS fin,
                   j.pause_debut AS pauseDebut, j.pause_fin AS pauseFin
            FROM jours_planning_employe j JOIN types_jour_planning tj ON tj.id = j.type_jour_id
            JOIN plannings_equipe pe ON pe.id = j.planning_id
            JOIN statuts_planning_equipe ps ON ps.id = pe.statut_id AND ps.code = 'PUBLIE'
            WHERE j.employe_id = ? AND j.date_travail = ?
            """, employeeId, date);
        long workedMinutes = workedMinutes(events);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("date", date);
        response.put("sessionId", sessionId == null ? 0 : sessionId);
        response.put("pointages", events);
        response.put("anomalies", anomalies);
        response.put("prochaineAction", next.isEmpty() ? "TERMINE" : next.getFirst().get("code"));
        response.put("prochaineActionLibelle", next.isEmpty() ? "Service terminé" : next.getFirst().get("libelle"));
        response.put("planning", planned.isEmpty() ? Map.of() : planned.getFirst());
        response.put("minutesTravaillees", workedMinutes);
        response.put("tempsTravaille", String.format("%02d h %02d", workedMinutes / 60, workedMinutes % 60));
        return response;
    }

    private static long workedMinutes(List<Map<String, Object>> events) {
        Map<String, OffsetDateTime> times = new LinkedHashMap<>();
        events.forEach(event -> times.put((String) event.get("code"), (OffsetDateTime) event.get("horodatage")));
        OffsetDateTime arrival = times.get("ARRIVEE");
        if (arrival == null) return 0;
        OffsetDateTime pauseStart = times.get("DEBUT_PAUSE");
        OffsetDateTime pauseEnd = times.get("FIN_PAUSE");
        OffsetDateTime serviceEnd = times.getOrDefault("FIN_SERVICE", OffsetDateTime.now(HR_ZONE));
        long minutes = pauseStart == null
            ? java.time.Duration.between(arrival, serviceEnd).toMinutes()
            : java.time.Duration.between(arrival, pauseStart).toMinutes()
                + (pauseEnd == null ? 0 : java.time.Duration.between(pauseEnd, serviceEnd).toMinutes());
        return Math.max(0, minutes);
    }

    private Integer ensureSession(Integer employeeId, LocalDate date) {
        List<Map<String, Object>> scheduled = jdbc.queryForList("""
            SELECT j.id FROM jours_planning_employe j JOIN plannings_equipe p ON p.id = j.planning_id
            JOIN statuts_planning_equipe s ON s.id = p.statut_id AND s.code = 'PUBLIE'
            WHERE j.employe_id = ? AND j.date_travail = ?
            """, employeeId, date);
        Integer scheduleId = scheduled.isEmpty() ? null : ((Number) scheduled.getFirst().get("id")).intValue();
        jdbc.update("INSERT INTO sessions_pointage (employe_id, jour_planning_id, date_service) VALUES (?, ?, ?) ON CONFLICT (employe_id, date_service) DO NOTHING", employeeId, scheduleId, date);
        return jdbc.queryForObject("SELECT id FROM sessions_pointage WHERE employe_id = ? AND date_service = ? FOR UPDATE", Integer.class, employeeId, date);
    }

    private void createAnomaly(Integer sessionId, String code, String details) {
        jdbc.update("""
            INSERT INTO anomalies_pointage (session_id, type_id, details)
            SELECT ?, id, ? FROM types_anomalie_pointage WHERE code = ? AND actif = TRUE
            ON CONFLICT (session_id, type_id) DO NOTHING
            """, sessionId, details, code);
    }

    private int ruleMinutes(String code) {
        String legacyCode = "TOLERANCE_RETARD".equals(code) ? "RETARD_TOLERE" : code;
        List<Integer> rules = jdbc.queryForList("SELECT valeur_minutes FROM regles_temps_travail WHERE code IN (?, ?) AND actif = TRUE AND date_debut <= CURRENT_DATE AND (date_fin IS NULL OR date_fin >= CURRENT_DATE) ORDER BY date_debut DESC LIMIT 1", Integer.class, code, legacyCode);
        if (rules.isEmpty()) throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Règle de temps manquante en base : " + code + ". Réimportez le script de données.");
        return rules.getFirst();
    }

    private Integer planningForUpdate(Integer userId, Integer departmentId, LocalDate monday) {
        jdbc.update("""
            INSERT INTO plannings_equipe (departement_id, semaine_debut, statut_id, cree_par_id)
            SELECT ?, ?, id, ? FROM statuts_planning_equipe WHERE code = 'BROUILLON'
            ON CONFLICT (departement_id, semaine_debut) DO NOTHING
            """, departmentId, monday, userId);
        return jdbc.queryForObject("SELECT id FROM plannings_equipe WHERE departement_id = ? AND semaine_debut = ?", Integer.class, departmentId, monday);
    }

    private Map<String, Object> ownPlanning(Integer planningId, Integer departmentId) {
        List<Map<String, Object>> found = jdbc.queryForList("""
            SELECT p.id, p.semaine_debut, p.statut_id, s.code AS statut_code
            FROM plannings_equipe p JOIN statuts_planning_equipe s ON s.id = p.statut_id
            WHERE p.id = ? AND p.departement_id = ? FOR UPDATE OF p
            """, planningId, departmentId);
        if (found.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Planning introuvable pour votre département.");
        return found.getFirst();
    }

    private Integer managerDepartment(Integer userId) {
        List<Integer> ids = jdbc.queryForList("""
            SELECT DISTINCT p.departement_id FROM employes e JOIN postes p ON p.id = e.poste_id
            JOIN user_roles ur ON ur.user_id = e.user_id JOIN roles r ON r.id = ur.role_id
            JOIN users u ON u.id = e.user_id AND u.is_suspended = FALSE
            JOIN statuts_validation_compte svc ON svc.id = u.administrative_validation_status_id AND svc.code = 'APPROVED'
            WHERE e.user_id = ? AND LOWER(r.role_name) = 'manager' AND p.actif = TRUE
            """, Integer.class, userId);
        if (ids.isEmpty()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Aucun département manager accessible.");
        if (ids.size() > 1) throw new ResponseStatusException(HttpStatus.CONFLICT, "Votre compte manager est rattaché à plusieurs départements ; le planning nécessite un département unique.");
        return ids.getFirst();
    }

    private Integer employeeId(Integer userId) {
        List<Integer> ids = jdbc.queryForList("""
            SELECT e.id FROM employes e JOIN users u ON u.id = e.user_id AND u.is_suspended = FALSE
            JOIN statuts_validation_compte svc ON svc.id = u.administrative_validation_status_id AND svc.code = 'APPROVED'
            LEFT JOIN situation_administrative_employe sa ON sa.employe_id = e.id
            LEFT JOIN statuts_administratifs_employe se ON se.id = sa.statut_id
            WHERE e.user_id = ? AND COALESCE(se.code, 'ACTIF') = 'ACTIF'
            """, Integer.class, userId);
        if (ids.isEmpty()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Aucun dossier salarié actif n’est associé à ce compte.");
        return ids.getFirst();
    }

    private void requireRole(Integer userId, String role) {
        Integer count = jdbc.queryForObject("""
            SELECT COUNT(*) FROM user_roles ur JOIN roles r ON r.id = ur.role_id
            JOIN users u ON u.id = ur.user_id AND u.is_suspended = FALSE
            JOIN statuts_validation_compte svc ON svc.id = u.administrative_validation_status_id AND svc.code = 'APPROVED'
            WHERE ur.user_id = ? AND LOWER(r.role_name) = LOWER(?)
            """, Integer.class, userId, role);
        if (count == null || count == 0) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès réservé au rôle " + role + ".");
    }

    private static Object[] args(Integer departmentId, Set<Integer> employeeIds) {
        List<Object> result = new ArrayList<>(); result.add(departmentId); result.addAll(employeeIds); return result.toArray();
    }

    private static LocalDate monday(LocalDate date) {
        if (date == null || date.getDayOfWeek() != java.time.DayOfWeek.MONDAY) throw badRequest("La semaine doit commencer un lundi.");
        return date;
    }

    private static String cleanObservation(String observation) {
        if (observation == null || observation.isBlank()) return null;
        String value = observation.trim();
        if (value.length() > 500) throw badRequest("L’observation ne peut pas dépasser 500 caractères.");
        return value;
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
