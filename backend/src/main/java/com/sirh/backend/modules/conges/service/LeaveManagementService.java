package com.sirh.backend.modules.conges.service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.sirh.backend.modules.notifications.service.NotificationCenterService;
import com.sirh.backend.modules.dossier.service.EmployeeDocumentStorageService;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LeaveManagementService {
    private static final ZoneId HR_ZONE = ZoneId.of("Indian/Antananarivo");
    private final JdbcTemplate jdbc;
    private final NotificationCenterService notifications;
    private final EmployeeDocumentStorageService documentStorage;

    public LeaveManagementService(JdbcTemplate jdbc, NotificationCenterService notifications, EmployeeDocumentStorageService documentStorage) {
        this.jdbc = jdbc;
        this.notifications = notifications;
        this.documentStorage = documentStorage;
    }

    @Transactional
    public Map<String, Object> attachJustificatif(Integer userId, Integer requestId, org.springframework.web.multipart.MultipartFile file) {
        Integer employeeId = employeeId(userId);
        Map<String, Object> request = jdbc.query("""
            SELECT d.id,d.statut_id,s.code FROM demandes_conge d
            JOIN statuts_demande_conge s ON s.id=d.statut_id
            WHERE d.id=? AND d.employe_id=? FOR UPDATE OF d
            """, rs -> {
                if (!rs.next()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Demande introuvable.");
                return Map.of("id", rs.getInt("id"), "status", rs.getString("code"));
            }, requestId, employeeId);
        if (!List.of("A_VALIDER_MANAGER", "A_VALIDER_RH").contains(request.get("status")))
            throw conflict("Un justificatif ne peut être ajouté qu’à une demande en cours de validation.");
        EmployeeDocumentStorageService.StoredDocument stored = documentStorage.store(employeeId, file);
        jdbc.update("""
            INSERT INTO justificatifs_demande_conge(demande_id,cle_stockage,nom_fichier,type_mime,taille_octets,ajoute_par_id)
            VALUES (?,?,?,?,?,?)
            """, requestId, stored.key(), stored.fileName(), stored.mimeType(), stored.size(), userId);
        return Map.of("demandeId", requestId, "nomFichier", stored.fileName(), "message", "Justificatif ajouté.");
    }

    @Transactional
    public Map<String, Object> employeeOverview(Integer userId) {
        Integer employeeId = employeeId(userId);
        accrue(employeeId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("soldes", balances(employeeId));
        result.put("types", jdbc.queryForList("SELECT id,code,libelle,justificatif_requis AS \"justificatifRequis\",acquisition_active AS \"acquisitionActive\" FROM types_conge WHERE actif=TRUE ORDER BY libelle"));
        result.put("demandes", jdbc.queryForList("""
            SELECT dc.id, tc.libelle AS type, dc.date_debut AS dateDebut, dc.date_fin AS dateFin,
                   dc.jours_demandes AS jours, dc.motif, s.code AS statutCode, s.libelle AS statut,
                   dc.soumise_at AS soumiseAt, tc.justificatif_requis AS "justificatifRequis",
                   EXISTS(SELECT 1 FROM justificatifs_demande_conge jd WHERE jd.demande_id=dc.id) AS "justificatifPresent"
            FROM demandes_conge dc JOIN types_conge tc ON tc.id=dc.type_conge_id
            JOIN statuts_demande_conge s ON s.id=dc.statut_id
            WHERE dc.employe_id=? ORDER BY dc.soumise_at DESC,dc.id DESC
            """, employeeId));
        return result;
    }

    @Transactional
    public Map<String, Object> submit(Integer userId, Integer typeId, LocalDate start, LocalDate end, String reason) {
        Integer employeeId = employeeId(userId);
        if (start == null || end == null || start.isBefore(LocalDate.now(HR_ZONE)) || end.isBefore(start)) {
            throw bad("Choisissez une période de congé valide à partir d’aujourd’hui.");
        }
        BigDecimal days = workingDays(employeeId, start, end);
        if (days.signum() <= 0) throw bad("La période ne contient aucun jour ouvré à demander.");
        Map<String, Object> type = one("SELECT id,code,libelle,acquisition_active FROM types_conge WHERE id=? AND actif=TRUE", typeId,
            "Type de congé introuvable.");
        if (Boolean.TRUE.equals(type.get("acquisition_active")) && (reason == null || reason.isBlank())) {
            throw bad("Indiquez le motif de la demande.");
        }
        jdbc.queryForObject("SELECT id FROM employes WHERE id=? FOR UPDATE", Integer.class, employeeId);
        if (overlaps(employeeId, start, end, null)) throw conflict("Cette période chevauche une autre demande ou une absence déjà enregistrée.");
        Map<String, Object> miseAPied = miseAPiedOverlap(employeeId, start, end);
        if (miseAPied != null) throw conflict("Cette période chevauche une mise à pied du "
            + miseAPied.get("date_debut") + " au " + miseAPied.get("date_fin") + ".");
        if (Boolean.TRUE.equals(type.get("acquisition_active"))) {
            BigDecimal available = balance(employeeId, typeId, null);
            if (available.compareTo(days) < 0) throw conflict("Solde insuffisant. Solde disponible après réservation des demandes en cours : " + available + " jour(s).");
        }
        Integer statusId = lookup("statuts_demande_conge", "A_VALIDER_MANAGER");
        Integer id = jdbc.queryForObject("""
            INSERT INTO demandes_conge(employe_id,type_conge_id,statut_id,date_debut,date_fin,jours_demandes,motif)
            VALUES (?,?,?,?,?,?,?) RETURNING id
            """, Integer.class, employeeId, typeId, statusId, start, end, days, clean(reason));
        Integer departmentId = departmentForEmployee(employeeId);
        List<Integer> managers = jdbc.queryForList("""
            SELECT DISTINCT e.user_id FROM employes e JOIN postes p ON p.id=e.poste_id
            JOIN user_roles ur ON ur.user_id=e.user_id JOIN roles r ON r.id=ur.role_id
            JOIN users u ON u.id=e.user_id AND u.is_suspended=FALSE
            WHERE p.departement_id=? AND LOWER(r.role_name)='manager'
            """, Integer.class, departmentId);
        for (Integer managerId : managers) notifications.notifyUser(managerId, "CONGE_A_VALIDER", "Demande de congé à traiter",
            "Une demande de congé de " + username(userId) + " attend votre décision.", "/espace/manager/equipe/conges");
        return Map.of("id", id, "type", type.get("libelle"), "jours", days, "statut", "À valider par le manager");
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> managerQueue(Integer managerId) {
        Integer departmentId = managerDepartment(managerId);
        return jdbc.queryForList(queueSql() + " WHERE p.departement_id=? AND s.code='A_VALIDER_MANAGER' ORDER BY dc.soumise_at,dc.id", departmentId);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> rhQueue(Integer rhId) {
        requireRole(rhId, "rh");
        return jdbc.queryForList(queueSql() + " WHERE s.code='A_VALIDER_RH' ORDER BY dc.soumise_at,dc.id");
    }

    @Transactional
    public Map<String, Object> managerDecision(Integer managerId, Integer requestId, String decision, String comment) {
        Integer departmentId = managerDepartment(managerId);
        Map<String, Object> request = lockRequest(requestId);
        if (!departmentId.equals(request.get("departement_id")) || !"A_VALIDER_MANAGER".equals(request.get("statut_code"))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Demande de congé introuvable dans votre équipe ou déjà traitée.");
        }
        String code = normalizeDecision(decision, comment);
        Integer decisionId = lookup("decisions_conge", code);
        Integer stepId = lookup("etapes_validation_conge", "MANAGER");
        jdbc.update("INSERT INTO validations_conge(demande_id,etape_id,decision_id,validateur_id,commentaire) VALUES (?,?,?,?,?)",
            requestId, stepId, decisionId, managerId, clean(comment));
        String next = "APPROUVEE".equals(code) ? "A_VALIDER_RH" : "REFUSEE";
        updateRequestStatus(requestId, next);
        Integer employeeUserId = ((Number) request.get("user_id")).intValue();
        if ("A_VALIDER_RH".equals(next)) {
            notifications.notifyRole("rh", "CONGE_A_VALIDER", "Congé à valider par les RH",
                "Le manager a validé une demande de congé. Elle attend votre contrôle.", "/espace/rh/conges/demandes");
        } else {
            notifications.notifyUser(employeeUserId, "CONGE_STATUT", "Demande de congé refusée",
                "Votre demande de congé a été refusée. " + safeComment(comment), "/espace/employe/conges");
        }
        return Map.of("statut", next, "message", "Décision du manager enregistrée.");
    }

    @Transactional
    public Map<String, Object> rhDecision(Integer rhId, Integer requestId, String decision, String comment) {
        requireRole(rhId, "rh");
        Map<String, Object> request = lockRequest(requestId);
        if (!"A_VALIDER_RH".equals(request.get("statut_code"))) throw conflict("Cette demande n’attend plus une décision RH.");
        String code = normalizeDecision(decision, comment);
        Integer decisionId = lookup("decisions_conge", code);
        Integer stepId = lookup("etapes_validation_conge", "RH");
        jdbc.update("INSERT INTO validations_conge(demande_id,etape_id,decision_id,validateur_id,commentaire) VALUES (?,?,?,?,?)",
            requestId, stepId, decisionId, rhId, clean(comment));
        String warning = null;
        if ("APPROUVEE".equals(code)) {
            Integer employeeId = ((Number) request.get("employe_id")).intValue();
            Integer typeId = ((Number) request.get("type_conge_id")).intValue();
            LocalDate start = (LocalDate) request.get("date_debut");
            LocalDate end = (LocalDate) request.get("date_fin");
            BigDecimal days = (BigDecimal) request.get("jours_demandes");
            boolean requiredDocument = Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT justificatif_requis FROM types_conge WHERE id=?", Boolean.class, typeId));
            if (requiredDocument && !Boolean.TRUE.equals(jdbc.queryForObject(
                    "SELECT EXISTS(SELECT 1 FROM justificatifs_demande_conge WHERE demande_id=?)", Boolean.class, requestId)))
                throw conflict("Un justificatif est obligatoire avant l’approbation finale.");
            if (overlaps(employeeId, start, end, requestId)) throw conflict("La période chevauche désormais une autre absence approuvée.");
            Map<String, Object> miseAPied = miseAPiedOverlap(employeeId, start, end);
            if (miseAPied != null) throw conflict("Cette période chevauche une mise à pied du "
                + miseAPied.get("date_debut") + " au " + miseAPied.get("date_fin") + ".");
            if (Boolean.TRUE.equals(request.get("acquisition_active"))) {
                BigDecimal available = balance(employeeId, typeId, requestId);
                if (available.compareTo(days) < 0) throw conflict("Le solde est devenu insuffisant avant la validation finale.");
                jdbc.update("""
                    INSERT INTO mouvements_solde_conge(employe_id,type_conge_id,type_mouvement_id,demande_id,jours,periode_debut,periode_fin,acteur_id,commentaire)
                    VALUES (?,?,(SELECT id FROM types_mouvement_solde_conge WHERE code='PRISE'),?,?,?,?,?,?)
                    """, employeeId, typeId, requestId, days.negate(), start, end, rhId, "Congé approuvé");
            }
            createApprovedAbsence(employeeId, requestId, start, end, rhId);
            warning = minimumCoverageWarning(employeeId, start, end);
        }
        String next = "APPROUVEE".equals(code) ? "APPROUVEE" : "REFUSEE";
        updateRequestStatus(requestId, next);
        Integer employeeUserId = ((Number) request.get("user_id")).intValue();
        notifications.notifyUser(employeeUserId, "CONGE_STATUT", "Demande de congé traitée",
            "Votre demande de congé a été " + ("APPROUVEE".equals(next) ? "approuvée." : "refusée. " + safeComment(comment)),
            "/espace/employe/conges");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("statut", next);
        result.put("alerteEffectif", warning);
        result.put("message", "Décision RH enregistrée.");
        return result;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> absenceHistory(Integer rhId, LocalDate start, LocalDate end) {
        requireRole(rhId, "rh");
        LocalDate from = start == null ? LocalDate.now(HR_ZONE).withDayOfMonth(1) : start;
        LocalDate to = end == null ? from.plusMonths(1).minusDays(1) : end;
        if (to.isBefore(from) || to.isAfter(from.plusMonths(3))) throw bad("La période doit être cohérente et ne pas dépasser trois mois.");
        return jdbc.queryForList("""
            SELECT a.id,e.matricule,COALESCE(u.full_name,u.username) AS employe,d.nom AS departement,
                   ta.libelle AS type,a.date_debut AS "dateDebut",a.date_fin AS "dateFin",a.motif,a.justifiee,
                   EXISTS(SELECT 1 FROM justificatifs_absence ja WHERE ja.absence_id=a.id) AS "justificatifPresent"
            FROM absences a JOIN employes e ON e.id=a.employe_id JOIN users u ON u.id=e.user_id
            JOIN postes p ON p.id=e.poste_id JOIN departements d ON d.id=p.departement_id
            JOIN types_absence ta ON ta.id=a.type_absence_id
            WHERE (a.date_debut AT TIME ZONE 'Indian/Antananarivo')::date<=?
              AND (a.date_fin AT TIME ZONE 'Indian/Antananarivo')::date>? ORDER BY a.date_debut DESC,a.id DESC
            """, to, from);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> absenceTypes(Integer rhId) {
        requireRole(rhId, "rh");
        return jdbc.queryForList("SELECT code,libelle,justificatif_requis AS \"justificatifRequis\",impacte_planning AS \"impactePlanning\" FROM types_absence WHERE actif=TRUE ORDER BY libelle");
    }

    @Transactional
    public Map<String, Object> recordAbsence(Integer rhId, Integer employeeId, String typeCode,
            LocalDate start, LocalDate end, String reason, boolean justified, org.springframework.web.multipart.MultipartFile attachment) {
        requireRole(rhId, "rh");
        if (start == null || end == null || end.isBefore(start) || reason == null || reason.isBlank() || reason.length() > 2000)
            throw bad("Employé, dates cohérentes et motif sont obligatoires pour enregistrer une absence.");
        Map<String, Object> type = jdbc.query("SELECT id,justificatif_requis,impacte_planning FROM types_absence WHERE code=? AND actif=TRUE", rs -> {
            if (!rs.next()) throw bad("Type d’absence actif introuvable.");
            return Map.of("id", rs.getInt("id"), "justificatifRequis", rs.getBoolean("justificatif_requis"), "impactePlanning", rs.getBoolean("impacte_planning"));
        }, typeCode);
        if (Boolean.TRUE.equals(type.get("justificatifRequis")) && (attachment == null || attachment.isEmpty()))
            throw bad("Un justificatif est obligatoire pour ce type d’absence.");
        jdbc.queryForObject("SELECT id FROM employes WHERE id=? FOR UPDATE", Integer.class, employeeId);
        if (overlaps(employeeId, start, end, null)) throw conflict("Cette période chevauche déjà une demande ou une absence.");
        Integer absenceId = jdbc.queryForObject("""
            INSERT INTO absences(employe_id,type_absence_id,date_debut,date_fin,enregistree_par_id,motif,justifiee)
            VALUES (?,?,?, ?,?,?,?) RETURNING id
            """, Integer.class, employeeId, type.get("id"), start.atStartOfDay(HR_ZONE).toOffsetDateTime(),
            end.plusDays(1).atStartOfDay(HR_ZONE).toOffsetDateTime(), rhId, reason.trim(), justified);
        if (attachment != null && !attachment.isEmpty()) {
            EmployeeDocumentStorageService.StoredDocument stored = documentStorage.store(employeeId, attachment);
            jdbc.update("""
                INSERT INTO justificatifs_absence(absence_id,cle_stockage,nom_fichier,type_mime,taille_octets,ajoute_par_id)
                VALUES (?,?,?,?,?,?)
                """, absenceId, stored.key(), stored.fileName(), stored.mimeType(), stored.size(), rhId);
        }
        if (Boolean.TRUE.equals(type.get("impactePlanning"))) {
          applyAbsenceToPlanning(employeeId, absenceId, start, end, rhId,
              lookup("types_jour_planning", "ABSENCE"), "Absence enregistrée : " + absenceId);
        }
        String warning = Boolean.TRUE.equals(type.get("impactePlanning")) ? minimumCoverageWarning(employeeId, start, end) : null;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", absenceId);
        result.put("message", Boolean.TRUE.equals(type.get("impactePlanning"))
            ? "Absence enregistrée. Aucun solde de congé n’a été débité automatiquement."
            : "Événement enregistré. Le planning journalier et le solde de congé restent inchangés.");
        result.put("alerteEffectif", warning);
        return result;
    }

    @Transactional(readOnly = true)
    public LeaveDocument downloadAbsenceJustificatif(Integer rhId, Integer absenceId) {
        requireRole(rhId, "rh");
        return jdbc.query("""
            SELECT ja.cle_stockage,ja.nom_fichier,ja.type_mime,e.id AS employe_id
            FROM justificatifs_absence ja JOIN absences a ON a.id=ja.absence_id
            JOIN employes e ON e.id=a.employe_id WHERE a.id=? ORDER BY ja.id DESC LIMIT 1
            """, rs -> {
                if (!rs.next()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Justificatif introuvable.");
                Integer employeeId = rs.getInt("employe_id");
                return new LeaveDocument(documentStorage.read(rs.getString("cle_stockage"), employeeId), rs.getString("nom_fichier"), rs.getString("type_mime"));
            }, absenceId);
    }

    @Transactional(readOnly = true)
    public LeaveDocument downloadLeaveJustificatif(Integer rhId, Integer requestId) {
        requireRole(rhId, "rh");
        return jdbc.query("""
            SELECT jd.cle_stockage,jd.nom_fichier,jd.type_mime,e.id AS employe_id
            FROM justificatifs_demande_conge jd JOIN demandes_conge dc ON dc.id=jd.demande_id
            JOIN employes e ON e.id=dc.employe_id WHERE dc.id=? ORDER BY jd.id DESC LIMIT 1
            """, rs -> {
                if (!rs.next()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Justificatif introuvable.");
                Integer employeeId = rs.getInt("employe_id");
                return new LeaveDocument(documentStorage.read(rs.getString("cle_stockage"), employeeId), rs.getString("nom_fichier"), rs.getString("type_mime"));
            }, requestId);
    }

    public record LeaveDocument(byte[] bytes, String fileName, String mimeType) { }

    private String minimumCoverageWarning(Integer employeeId, LocalDate start, LocalDate end) {
        Integer posteId = jdbc.queryForObject("SELECT poste_id FROM employes WHERE id=?", Integer.class, employeeId);
        List<String> warnings = new ArrayList<>();
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            if (!coverageWorkday(posteId, date)) continue;
            Integer minimum = jdbc.queryForObject("""
                SELECT COALESCE((SELECT minimum_presence FROM regles_effectif_minimal
                    WHERE poste_id=? AND actif=TRUE AND date_debut<=? AND (date_fin IS NULL OR date_fin>=?)
                    ORDER BY date_debut DESC LIMIT 1),0)
                """, Integer.class, posteId, date, date);
            if (minimum == null || minimum == 0) continue;
            Integer active = jdbc.queryForObject("""
                SELECT COUNT(*) FROM employes e LEFT JOIN situation_administrative_employe sa ON sa.employe_id=e.id
                LEFT JOIN statuts_administratifs_employe se ON se.id=sa.statut_id
                WHERE e.poste_id=? AND e.date_embauche<=? AND COALESCE(se.code,'ACTIF') NOT IN ('SORTI','EN_PREPARATION')
              AND NOT EXISTS (SELECT 1 FROM absences a JOIN types_absence ta ON ta.id=a.type_absence_id
                  WHERE a.employe_id=e.id AND ta.impacte_planning=TRUE
                    AND (a.date_debut AT TIME ZONE 'Indian/Antananarivo')::date<=?
                    AND (a.date_fin AT TIME ZONE 'Indian/Antananarivo')::date>?)
                """, Integer.class, posteId, date, date, date);
            int projected = Math.max(0, active == null ? 0 : active);
            if (projected < minimum) warnings.add(date + ": " + projected + "/" + minimum);
        }
        if (warnings.isEmpty()) return null;
        String department = jdbc.queryForObject("SELECT nom FROM postes p JOIN departements d ON d.id=p.departement_id WHERE p.id=?", String.class, posteId);
        String summary = "Sous-effectif prévisionnel pour le poste du département " + department + " : " + String.join(", ", warnings);
        Integer departmentId = departmentForEmployee(employeeId);
        List<Integer> managers = jdbc.queryForList("""
            SELECT DISTINCT e.user_id FROM employes e JOIN postes p ON p.id=e.poste_id
            JOIN user_roles ur ON ur.user_id=e.user_id JOIN roles r ON r.id=ur.role_id
            WHERE p.departement_id=? AND LOWER(r.role_name)='manager'
            """, Integer.class, departmentId);
        for (Integer managerId : managers) notifications.notifyUser(managerId, "EFFECTIF_SOUS_MINIMUM", "Alerte de couverture d’équipe", summary,
            "/espace/manager/equipe/conges");
        notifications.notifyRole("rh", "EFFECTIF_SOUS_MINIMUM", "Alerte de couverture d’équipe", summary,
            "/espace/rh/conges/planning");
        return summary;
    }

    private void createApprovedAbsence(Integer employeeId, Integer requestId, LocalDate start, LocalDate end, Integer actorId) {
        Integer absenceType = lookup("types_absence", "ABSENCE_AUTORISEE");
        OffsetDateTime startAt = start.atStartOfDay(HR_ZONE).toOffsetDateTime();
        OffsetDateTime endAt = end.plusDays(1).atStartOfDay(HR_ZONE).toOffsetDateTime();
        Integer absenceId = jdbc.queryForObject("""
            INSERT INTO absences(employe_id,type_absence_id,demande_conge_id,date_debut,date_fin,enregistree_par_id,motif,justifiee)
            VALUES (?,?,?,?,?,?,?,TRUE) RETURNING id
            """, Integer.class, employeeId, absenceType, requestId, startAt, endAt, actorId, "Congé approuvé");
        Integer congeDayType = lookup("types_jour_planning", "CONGE");
        applyAbsenceToPlanning(employeeId, absenceId, start, end, actorId, congeDayType,
            "Congé approuvé, absence " + absenceId);
    }

    private void applyAbsenceToPlanning(Integer employeeId, Integer absenceId, LocalDate start, LocalDate end,
            Integer actorId, Integer absenceDayType, String observation) {
        Integer workDayType = lookup("types_jour_planning", "TRAVAIL");
        Integer departmentId = departmentForEmployee(employeeId);
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            if (!employeeWorkday(employeeId, date)) continue;
            List<Map<String, Object>> scheduled = jdbc.queryForList("""
                SELECT j.id,j.type_jour_id,t.code AS type_code FROM jours_planning_employe j
                JOIN types_jour_planning t ON t.id=j.type_jour_id
                WHERE j.employe_id=? AND j.date_travail=? FOR UPDATE OF j
                """, employeeId, date);
            if (scheduled.isEmpty()) {
                Integer planningId = ensurePlanningForLeave(actorId, departmentId,
                    date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)));
                jdbc.update("""
                    INSERT INTO jours_planning_employe(planning_id,employe_id,date_travail,type_jour_id,observation,modifie_par_id)
                    VALUES (?,?,?,?,?,?) ON CONFLICT (employe_id,date_travail) DO NOTHING
                    """, planningId, employeeId, date, absenceDayType, observation, actorId);
                continue;
            }
            Map<String, Object> scheduledDay = scheduled.getFirst();
            if (!"TRAVAIL".equals(scheduledDay.get("type_code"))) continue;
            jdbc.update("""
                INSERT INTO historiques_jour_planning(jour_planning_id,planning_id,employe_id,date_travail,
                    ancien_type_jour_id,nouveau_type_jour_id,ancienne_heure_debut,ancienne_heure_fin,
                    ancienne_pause_debut,ancienne_pause_fin,observation,modifie_par_id)
                SELECT j.id,j.planning_id,j.employe_id,j.date_travail,j.type_jour_id,?,j.heure_debut,j.heure_fin,
                    j.pause_debut,j.pause_fin,?,?
                FROM jours_planning_employe j WHERE j.id=?
                """, absenceDayType, observation, actorId, scheduledDay.get("id"));
            jdbc.update("""
                UPDATE jours_planning_employe SET type_jour_id=?,heure_debut=NULL,heure_fin=NULL,
                    pause_debut=NULL,pause_fin=NULL,observation=?,modifie_par_id=?,version=version+1,updated_at=CURRENT_TIMESTAMP
                WHERE id=? AND type_jour_id=?
                """, absenceDayType, observation, actorId, scheduledDay.get("id"), workDayType);
        }
        List<Integer> managers = jdbc.queryForList("""
            SELECT DISTINCT e.user_id FROM employes e JOIN postes p ON p.id=e.poste_id
            JOIN user_roles ur ON ur.user_id=e.user_id JOIN roles r ON r.id=ur.role_id
            JOIN users u ON u.id=e.user_id AND u.is_suspended=FALSE
            WHERE p.departement_id=? AND LOWER(r.role_name)='manager'
            """, Integer.class, departmentId);
        for (Integer managerId : managers) notifications.notifyUser(managerId, "CONGE_STATUT", "Absence ajoutée au planning",
            "Une absence impactant la présence a été reportée sur le planning de votre département.", "/espace/manager/planning/equipe");
    }

    private Integer ensurePlanningForLeave(Integer actorId, Integer departmentId, LocalDate monday) {
        jdbc.update("""
            INSERT INTO plannings_equipe(departement_id,semaine_debut,statut_id,cree_par_id)
            SELECT ?,?,id,? FROM statuts_planning_equipe WHERE code='BROUILLON'
            ON CONFLICT (departement_id,semaine_debut) DO NOTHING
            """, departmentId, monday, actorId);
        return jdbc.queryForObject("SELECT id FROM plannings_equipe WHERE departement_id=? AND semaine_debut=?", Integer.class, departmentId, monday);
    }

    private boolean overlaps(Integer employeeId, LocalDate start, LocalDate end, Integer exceptId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
            SELECT EXISTS (
              SELECT 1 FROM demandes_conge d JOIN statuts_demande_conge s ON s.id=d.statut_id
              WHERE d.employe_id=? AND s.code NOT IN ('REFUSEE','ANNULEE') AND d.date_debut<=? AND d.date_fin>=?
                AND d.id IS DISTINCT FROM ?
              UNION ALL
              SELECT 1 FROM absences a JOIN types_absence ta ON ta.id=a.type_absence_id
                WHERE a.employe_id=? AND ta.impacte_planning=TRUE
                  AND (a.date_debut AT TIME ZONE 'Indian/Antananarivo')::date<=?
                  AND (a.date_fin AT TIME ZONE 'Indian/Antananarivo')::date>?
            )
            """, Boolean.class, employeeId, end, start, exceptId, employeeId, end, start));
    }

    private Map<String, Object> miseAPiedOverlap(Integer employeeId, LocalDate start, LocalDate end) {
        List<Map<String, Object>> rows = jdbc.queryForList("""
            SELECT sa.date_debut, sa.date_fin
            FROM sanctions sa
            JOIN types_sanction ts ON ts.id = sa.type_id AND ts.code = 'MISE_A_PIED'
            JOIN procedures_disciplinaires pr ON pr.id = sa.procedure_id
            JOIN incidents i ON i.id = pr.incident_id
            WHERE i.employe_id = ? AND sa.date_debut <= ? AND sa.date_fin >= ?
            ORDER BY sa.date_debut LIMIT 1
            """, employeeId, end, start);
        return rows.isEmpty() ? null : rows.getFirst();
    }

    private BigDecimal balance(Integer employeeId, Integer typeId, Integer exceptRequest) {
        BigDecimal earnedAndAdjusted = jdbc.queryForObject("""
            SELECT COALESCE(SUM(jours),0) FROM mouvements_solde_conge WHERE employe_id=? AND type_conge_id=?
            """, BigDecimal.class, employeeId, typeId);
        BigDecimal reserved = jdbc.queryForObject("""
            SELECT COALESCE(SUM(jours_demandes),0) FROM demandes_conge d
            JOIN statuts_demande_conge s ON s.id=d.statut_id
            WHERE d.employe_id=? AND d.type_conge_id=? AND s.code IN ('A_VALIDER_MANAGER','A_VALIDER_RH')
              AND d.id IS DISTINCT FROM ?
            """, BigDecimal.class, employeeId, typeId, exceptRequest);
        return earnedAndAdjusted.subtract(reserved);
    }

    private void accrue(Integer employeeId) {
        LocalDate hireDate = jdbc.queryForObject("SELECT date_embauche FROM employes WHERE id=?", LocalDate.class, employeeId);
        if (hireDate == null) return;
        int completeMonths = (int) java.time.temporal.ChronoUnit.MONTHS.between(hireDate, LocalDate.now(HR_ZONE));
        Integer movementType = lookup("types_mouvement_solde_conge", "ACQUISITION");
        List<Integer> types = jdbc.queryForList("SELECT id FROM types_conge WHERE actif=TRUE AND acquisition_active=TRUE", Integer.class);
        for (Integer typeId : types) {
            for (int month = 1; month <= completeMonths; month++) {
                LocalDate periodEnd = hireDate.plusMonths(month);
                BigDecimal rate = jdbc.query("""
                SELECT jours_par_mois FROM regles_acquisition_conge
                    WHERE type_conge_id=? AND date_debut<=? AND (date_fin IS NULL OR date_fin>=?)
                    ORDER BY date_debut DESC LIMIT 1
                    """, rs -> rs.next() ? rs.getBigDecimal(1) : null, typeId, periodEnd, periodEnd);
                if (rate == null || rate.signum() == 0) continue;
                jdbc.update("""
                    INSERT INTO mouvements_solde_conge(employe_id,type_conge_id,type_mouvement_id,jours,periode_debut,periode_fin,commentaire)
                    VALUES (?,?,?,?,?,?,?) ON CONFLICT DO NOTHING
                    """, employeeId, typeId, movementType, rate, periodEnd.minusMonths(1), periodEnd,
                    "Acquisition mensuelle calculée selon la règle RH en vigueur.");
            }
        }
    }

    private List<Map<String, Object>> balances(Integer employeeId) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> type : jdbc.queryForList("SELECT id,code,libelle,acquisition_active FROM types_conge WHERE actif=TRUE ORDER BY libelle")) {
            Integer id = ((Number) type.get("id")).intValue();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("typeId", id); item.put("code", type.get("code")); item.put("type", type.get("libelle"));
            item.put("solde", balance(employeeId, id, null)); item.put("acquisitionActive", type.get("acquisition_active"));
            result.add(item);
        }
        return result;
    }

    private Map<String, Object> lockRequest(Integer id) {
        return jdbc.query("""
            SELECT d.id,d.employe_id,d.type_conge_id,d.statut_id,d.date_debut,d.date_fin,d.jours_demandes,
                   s.code AS statut_code,t.acquisition_active,e.user_id,p.departement_id,u.username
            FROM demandes_conge d JOIN statuts_demande_conge s ON s.id=d.statut_id
            JOIN types_conge t ON t.id=d.type_conge_id JOIN employes e ON e.id=d.employe_id
            JOIN postes p ON p.id=e.poste_id JOIN users u ON u.id=e.user_id
            WHERE d.id=? FOR UPDATE OF d
            """, rs -> {
                if (!rs.next()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Demande de congé introuvable.");
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", rs.getInt("id")); item.put("employe_id", rs.getInt("employe_id"));
                item.put("type_conge_id", rs.getInt("type_conge_id")); item.put("statut_id", rs.getInt("statut_id"));
                item.put("date_debut", rs.getObject("date_debut", LocalDate.class)); item.put("date_fin", rs.getObject("date_fin", LocalDate.class));
                item.put("jours_demandes", rs.getBigDecimal("jours_demandes")); item.put("statut_code", rs.getString("statut_code"));
                item.put("acquisition_active", rs.getBoolean("acquisition_active")); item.put("user_id", rs.getInt("user_id"));
                item.put("departement_id", rs.getInt("departement_id")); item.put("username", rs.getString("username"));
                return item;
            }, id);
    }

    private String queueSql() {
        return """
            SELECT dc.id,dc.employe_id AS "employeId",e.user_id AS "userId",u.username,COALESCE(u.full_name,u.username) AS employe,
                   d.nom AS departement,p.intitule AS poste,tc.libelle AS type,dc.date_debut AS "dateDebut",
                   dc.date_fin AS "dateFin",dc.jours_demandes AS jours,dc.motif,s.code AS "statutCode",
                   s.libelle AS statut,dc.soumise_at AS "soumiseAt",tc.justificatif_requis AS "justificatifRequis",
                   EXISTS(SELECT 1 FROM justificatifs_demande_conge jd WHERE jd.demande_id=dc.id) AS "justificatifPresent",
                   (SELECT COUNT(DISTINCT a.employe_id)
                    FROM absences a JOIN employes ae ON ae.id=a.employe_id JOIN postes ap ON ap.id=ae.poste_id
                    WHERE a.demande_conge_id IS NOT NULL AND ap.departement_id=p.departement_id
                      AND a.employe_id<>dc.employe_id
                      AND a.date_debut < ((dc.date_fin + 1)::timestamp AT TIME ZONE 'Indian/Antananarivo')
                      AND a.date_fin > (dc.date_debut::timestamp AT TIME ZONE 'Indian/Antananarivo')) AS "colleguesEnCongeChevauchant",
                   (SELECT COUNT(DISTINCT autre.employe_id)
                    FROM demandes_conge autre JOIN statuts_demande_conge autre_statut ON autre_statut.id=autre.statut_id
                    JOIN employes autre_employe ON autre_employe.id=autre.employe_id JOIN postes autre_poste ON autre_poste.id=autre_employe.poste_id
                    WHERE autre.id<>dc.id AND autre.employe_id<>dc.employe_id AND autre_poste.departement_id=p.departement_id
                      AND autre_statut.code IN ('A_VALIDER_MANAGER','A_VALIDER_RH')
                      AND autre.date_debut<=dc.date_fin AND autre.date_fin>=dc.date_debut) AS "colleguesAvecDemandeChevauchante",
                   (SELECT COALESCE(MAX(journee.effectif_absent),0)
                    FROM generate_series(dc.date_debut,dc.date_fin,INTERVAL '1 day') jour(date)
                    CROSS JOIN LATERAL (
                        SELECT COUNT(DISTINCT indisponible.employe_id) AS effectif_absent
                        FROM (
                            SELECT a.employe_id FROM absences a JOIN employes ae ON ae.id=a.employe_id JOIN postes ap ON ap.id=ae.poste_id
                            WHERE a.demande_conge_id IS NOT NULL AND ap.departement_id=p.departement_id AND a.employe_id<>dc.employe_id
                              AND (a.date_debut AT TIME ZONE 'Indian/Antananarivo')::date<=jour.date
                              AND (a.date_fin AT TIME ZONE 'Indian/Antananarivo')::date>jour.date
                            UNION
                            SELECT autre.employe_id FROM demandes_conge autre
                            JOIN statuts_demande_conge autre_statut ON autre_statut.id=autre.statut_id
                            JOIN employes autre_employe ON autre_employe.id=autre.employe_id JOIN postes autre_poste ON autre_poste.id=autre_employe.poste_id
                            WHERE autre.id<>dc.id AND autre.employe_id<>dc.employe_id AND autre_poste.departement_id=p.departement_id
                              AND autre_statut.code IN ('A_VALIDER_MANAGER','A_VALIDER_RH')
                              AND autre.date_debut<=jour.date AND autre.date_fin>=jour.date
                            UNION SELECT dc.employe_id
                        ) indisponible
                    ) journee) AS "picAbsenceProjete",
                   (SELECT COUNT(DISTINCT ae.id)
                    FROM employes ae JOIN postes ap ON ap.id=ae.poste_id
                    LEFT JOIN situation_administrative_employe situation ON situation.employe_id=ae.id
                    LEFT JOIN statuts_administratifs_employe statut_employe ON statut_employe.id=situation.statut_id
                    WHERE ap.departement_id=p.departement_id AND ae.date_embauche<=CURRENT_DATE
                      AND COALESCE(statut_employe.code,'ACTIF') NOT IN ('SORTI','EN_PREPARATION')) AS "effectifActifDepartement"
            FROM demandes_conge dc JOIN employes e ON e.id=dc.employe_id JOIN users u ON u.id=e.user_id
            JOIN postes p ON p.id=e.poste_id JOIN departements d ON d.id=p.departement_id
            JOIN types_conge tc ON tc.id=dc.type_conge_id JOIN statuts_demande_conge s ON s.id=dc.statut_id
            """;
    }

    private Integer employeeId(Integer userId) {
        Integer id = jdbc.query("SELECT id FROM employes WHERE user_id=?", rs -> rs.next() ? rs.getInt(1) : null, userId);
        if (id == null) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Aucun dossier salarié actif n’est lié à ce compte.");
        return id;
    }

    private Integer departmentForEmployee(Integer employeeId) {
        return jdbc.query("SELECT p.departement_id FROM employes e JOIN postes p ON p.id=e.poste_id WHERE e.id=?",
            rs -> rs.next() ? rs.getInt(1) : null, employeeId);
    }

    private Integer managerDepartment(Integer managerId) {
        requireRole(managerId, "manager");
        List<Integer> ids = jdbc.queryForList("""
            SELECT DISTINCT p.departement_id FROM employes e JOIN postes p ON p.id=e.poste_id
            WHERE e.user_id=? AND p.actif=TRUE
            """, Integer.class, managerId);
        if (ids.size() != 1) throw conflict("Le compte manager doit être rattaché à un seul département actif.");
        return ids.getFirst();
    }

    private void requireRole(Integer userId, String role) {
        boolean allowed = Boolean.TRUE.equals(jdbc.queryForObject("""
            SELECT EXISTS (SELECT 1 FROM users u JOIN statuts_validation_compte s ON s.id=u.administrative_validation_status_id
              JOIN user_roles ur ON ur.user_id=u.id JOIN roles r ON r.id=ur.role_id
              WHERE u.id=? AND u.is_suspended=FALSE AND s.code='APPROVED' AND LOWER(r.role_name)=LOWER(?))
            """, Boolean.class, userId, role));
        if (!allowed) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Un compte " + role + " validé est requis.");
    }

    private Integer lookup(String table, String code) {
        if (!List.of("statuts_demande_conge", "decisions_conge", "etapes_validation_conge", "types_mouvement_solde_conge", "types_absence", "types_jour_planning").contains(table))
            throw new IllegalArgumentException("Référentiel non autorisé.");
        Integer id = jdbc.query("SELECT id FROM " + table + " WHERE code=?", rs -> rs.next() ? rs.getInt(1) : null, code);
        if (id == null) throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Référentiel manquant : " + code + ". Réimportez les scripts SQL finaux.");
        return id;
    }

    private void updateRequestStatus(Integer id, String code) {
        jdbc.update("UPDATE demandes_conge SET statut_id=?,decidee_at=CASE WHEN ? IN ('APPROUVEE','REFUSEE') THEN CURRENT_TIMESTAMP ELSE decidee_at END,updated_at=CURRENT_TIMESTAMP WHERE id=?",
            lookup("statuts_demande_conge", code), code, id);
    }

    private String normalizeDecision(String decision, String comment) {
        String code = decision == null ? "" : decision.trim().toUpperCase(java.util.Locale.ROOT);
        if (!List.of("APPROUVEE", "REFUSEE").contains(code)) throw bad("Décision invalide.");
        if ("REFUSEE".equals(code) && (comment == null || comment.isBlank())) throw bad("Un commentaire est obligatoire pour refuser la demande.");
        return code;
    }

    private BigDecimal workingDays(Integer employeeId, LocalDate start, LocalDate end) {
        long count = 0;
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1))
            if (employeeWorkday(employeeId, date)) count++;
        return BigDecimal.valueOf(count);
    }

    private boolean employeeWorkday(Integer employeeId, LocalDate date) {
        List<String> scheduled = jdbc.queryForList("""
            SELECT t.code FROM jours_planning_employe j
            JOIN types_jour_planning t ON t.id=j.type_jour_id
            JOIN plannings_equipe p ON p.id=j.planning_id
            WHERE j.employe_id=? AND j.date_travail=? AND t.actif=TRUE
            ORDER BY CASE WHEN (SELECT s.code FROM statuts_planning_equipe s WHERE s.id=p.statut_id)='PUBLIE' THEN 0 ELSE 1 END
            LIMIT 1
            """, String.class, employeeId, date);
        if (!scheduled.isEmpty()) return "TRAVAIL".equals(scheduled.getFirst());
        return calendarWorkday(date);
    }

    private boolean coverageWorkday(Integer posteId, LocalDate date) {
        List<Integer> planned = jdbc.queryForList("""
            SELECT 1 FROM jours_planning_employe j
            JOIN types_jour_planning t ON t.id=j.type_jour_id
            JOIN employes e ON e.id=j.employe_id
            WHERE e.poste_id=? AND j.date_travail=? AND t.actif=TRUE LIMIT 1
            """, Integer.class, posteId, date);
        if (!planned.isEmpty()) return Boolean.TRUE.equals(jdbc.queryForObject("""
            SELECT EXISTS(SELECT 1 FROM jours_planning_employe j
                JOIN types_jour_planning t ON t.id=j.type_jour_id
                JOIN employes e ON e.id=j.employe_id
                WHERE e.poste_id=? AND j.date_travail=? AND t.code='TRAVAIL' AND t.actif=TRUE)
            """, Boolean.class, posteId, date));
        return calendarWorkday(date);
    }

    private boolean calendarWorkday(LocalDate date) {
        Boolean holiday = jdbc.queryForObject(
            "SELECT EXISTS(SELECT 1 FROM jours_feries WHERE date_ferie=? AND actif=TRUE)", Boolean.class, date);
        if (Boolean.TRUE.equals(holiday)) return false;
        Boolean workday = jdbc.queryForObject("""
            SELECT ouvrable_par_defaut FROM jours_semaine
            WHERE numero_iso=? AND actif=TRUE
            """, Boolean.class, date.getDayOfWeek().getValue());
        if (workday == null) throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
            "Calendrier hebdomadaire incomplet. Vérifiez les données de référence SQL.");
        return workday;
    }

    private String username(Integer userId) {
        return jdbc.query("SELECT COALESCE(full_name,username) FROM users WHERE id=?", rs -> rs.next() ? rs.getString(1) : "Un salarié", userId);
    }

    private Map<String, Object> one(String sql, Integer parameter, String message) {
        return jdbc.query(sql, rs -> {
            if (!rs.next()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("id", rs.getInt("id")); result.put("code", rs.getString("code"));
            result.put("libelle", rs.getString("libelle")); result.put("acquisition_active", rs.getBoolean("acquisition_active"));
            return result;
        }, parameter);
    }

    private String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private String safeComment(String value) { return value == null || value.isBlank() ? "" : "Motif : " + value.trim(); }
    private ResponseStatusException bad(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
    private ResponseStatusException conflict(String message) { return new ResponseStatusException(HttpStatus.CONFLICT, message); }
}

