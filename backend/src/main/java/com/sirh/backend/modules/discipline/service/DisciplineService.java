package com.sirh.backend.modules.discipline.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import com.sirh.backend.modules.discipline.dto.DecisionRequest;
import com.sirh.backend.modules.discipline.dto.DemandeExplicationRequest;
import com.sirh.backend.modules.discipline.dto.DeclareIncidentRequest;
import com.sirh.backend.modules.notifications.service.NotificationCenterService;
import com.sirh.backend.modules.temps.service.TimeManagementService;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DisciplineService {
    private static final ZoneId HR_ZONE = ZoneId.of("Indian/Antananarivo");
    private static final String RH_LINK = "/espace/rh/discipline";
    private static final String EMPLOYE_LINK = "/espace/employe/discipline";

    private final JdbcTemplate jdbc;
    private final NotificationCenterService notifications;
    private final TimeManagementService timeManagement;
    private final DisciplineStorageService storage;

    public DisciplineService(JdbcTemplate jdbc, NotificationCenterService notifications,
            TimeManagementService timeManagement, DisciplineStorageService storage) {
        this.jdbc = jdbc;
        this.notifications = notifications;
        this.timeManagement = timeManagement;
        this.storage = storage;
    }

    // ------------------------------------------------------------------
    // Incidents (#14, #15, #20)
    // ------------------------------------------------------------------

    @Transactional
    public Map<String, Object> declareIncident(Integer declarantId, DeclareIncidentRequest request, boolean managerScope) {
        if (managerScope) {
            requireRole(declarantId, "manager");
            Integer departmentId = timeManagement.managerDepartmentId(declarantId);
            Integer employeeDepartment = employeeDepartment(request.employeId());
            if (employeeDepartment == null || !departmentId.equals(employeeDepartment)) {
                throw forbidden("Vous ne pouvez déclarer un incident que pour un employé de votre équipe.");
            }
        } else {
            requireRole(declarantId, "rh");
            if (!employeeExists(request.employeId())) throw bad("Employé introuvable.");
        }
        Integer graviteId = lookupCode("gravites_incident", request.graviteCode(), "Gravité d’incident");
        String titre = request.titre().trim();
        String description = request.description().trim();
        Integer incidentId = jdbc.queryForObject("""
            INSERT INTO incidents (employe_id, declarant_id, gravite_id, titre, description, date_faits)
            VALUES (?, ?, ?, ?, ?, ?) RETURNING id
            """, Integer.class, request.employeId(), declarantId, graviteId, titre, description, request.dateFaits());
        journal(declarantId, "CREATION", "INCIDENT", incidentId, request.employeId(), "Incident déclaré : " + titre);
        return incidentSummary(incidentId);
    }

    @Transactional
    public List<Map<String, Object>> managerIncidents(Integer managerId) {
        requireRole(managerId, "manager");
        List<Map<String, Object>> rows = jdbc.queryForList("""
            SELECT i.id, i.titre, i.description, i.date_faits AS "dateFaits", i.created_at AS "createdAt",
                   i.employe_id AS "employeId", e.matricule, COALESCE(u.full_name, u.username) AS employe,
                   p.intitule AS poste, d.nom AS departement,
                   g.code AS "graviteCode", g.libelle AS gravite,
                   pr.id AS "procedureId", sp.code AS "statutCode", sp.libelle AS statut
            FROM incidents i
            JOIN employes e ON e.id = i.employe_id JOIN users u ON u.id = e.user_id
            JOIN postes p ON p.id = e.poste_id JOIN departements d ON d.id = p.departement_id
            JOIN gravites_incident g ON g.id = i.gravite_id
            LEFT JOIN procedures_disciplinaires pr ON pr.incident_id = i.id
            LEFT JOIN statuts_procedure_disciplinaire sp ON sp.id = pr.statut_id
            WHERE i.declarant_id = ?
            ORDER BY i.created_at DESC, i.id DESC
            """, managerId);
        journal(managerId, "LECTURE", "INCIDENTS_MANAGER", null, null,
            "Consultation de la liste des incidents de l’équipe (" + rows.size() + ")");
        return rows;
    }

    @Transactional
    public List<Map<String, Object>> rhIncidents(Integer rhId, boolean sansProcedure) {
        requireRole(rhId, "rh");
        String filter = sansProcedure ? " WHERE pr.id IS NULL " : "";
        List<Map<String, Object>> rows = jdbc.queryForList("""
            SELECT i.id, i.titre, i.description, i.date_faits AS "dateFaits", i.created_at AS "createdAt",
                   i.employe_id AS "employeId", e.matricule, COALESCE(u.full_name, u.username) AS employe,
                   p.intitule AS poste, d.nom AS departement,
                   g.code AS "graviteCode", g.libelle AS gravite,
                   pr.id AS "procedureId", sp.code AS "statutCode", sp.libelle AS statut
            FROM incidents i
            JOIN employes e ON e.id = i.employe_id JOIN users u ON u.id = e.user_id
            JOIN postes p ON p.id = e.poste_id JOIN departements d ON d.id = p.departement_id
            JOIN gravites_incident g ON g.id = i.gravite_id
            LEFT JOIN procedures_disciplinaires pr ON pr.incident_id = i.id
            LEFT JOIN statuts_procedure_disciplinaire sp ON sp.id = pr.statut_id
            """ + filter + """
            ORDER BY i.created_at DESC, i.id DESC
            """);
        journal(rhId, "LECTURE", sansProcedure ? "INCIDENTS_RH_SANS_PROCEDURE" : "INCIDENTS_RH", null, null,
            "Consultation de la liste des incidents RH (" + rows.size() + ")");
        return rows;
    }

    // ------------------------------------------------------------------
    // Procédure (#16, #17, #19, #20)
    // ------------------------------------------------------------------

    @Transactional
    public Map<String, Object> openProcedure(Integer rhId, Integer incidentId) {
        requireRole(rhId, "rh");
        Map<String, Object> incident = jdbc.query("""
            SELECT id, employe_id, titre FROM incidents WHERE id = ? FOR UPDATE
            """, rs -> {
                if (!rs.next()) return null;
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", rs.getInt("id"));
                row.put("employeId", rs.getInt("employe_id"));
                row.put("titre", rs.getString("titre"));
                return row;
            }, incidentId);
        if (incident == null) throw notFound("Incident introuvable.");
        Boolean exists = jdbc.queryForObject(
            "SELECT EXISTS(SELECT 1 FROM procedures_disciplinaires WHERE incident_id = ?)", Boolean.class, incidentId);
        if (Boolean.TRUE.equals(exists)) throw conflict("Une procédure est déjà ouverte pour cet incident.");
        Integer statutId = lookupCode("statuts_procedure_disciplinaire", "OUVERTE", "Statut");
        Integer procedureId = jdbc.queryForObject("""
            INSERT INTO procedures_disciplinaires (incident_id, statut_id, ouverte_par_id)
            VALUES (?, ?, ?) RETURNING id
            """, Integer.class, incidentId, statutId, rhId);
        Integer employeId = (Integer) incident.get("employeId");
        journal(rhId, "CREATION", "PROCEDURE", procedureId, employeId, "Ouverture de la procédure");
        notifications.notifyRole("rh", "DISCIPLINE_INCIDENT_DECLARE", "Procédure disciplinaire ouverte",
            "Une procédure a été ouverte pour l’incident « " + incident.get("titre") + " ».", RH_LINK);
        return procedureSummary(procedureId);
    }

    @Transactional
    public Map<String, Object> requestExplanation(Integer rhId, Integer procedureId, DemandeExplicationRequest request) {
        requireRole(rhId, "rh");
        Map<String, Object> procedure = jdbc.query("""
            SELECT pr.id, sp.code AS statut_code, i.employe_id, i.titre
            FROM procedures_disciplinaires pr
            JOIN statuts_procedure_disciplinaire sp ON sp.id = pr.statut_id
            JOIN incidents i ON i.id = pr.incident_id
            WHERE pr.id = ? FOR UPDATE OF pr
            """, rs -> {
                if (!rs.next()) return null;
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", rs.getInt("id"));
                row.put("statutCode", rs.getString("statut_code"));
                row.put("employeId", rs.getInt("employe_id"));
                row.put("titre", rs.getString("titre"));
                return row;
            }, procedureId);
        if (procedure == null) throw notFound("Procédure introuvable.");
        if (!"OUVERTE".equals(procedure.get("statutCode"))) throw conflict("La procédure n’est pas ouverte.");
        Boolean exists = jdbc.queryForObject(
            "SELECT EXISTS(SELECT 1 FROM demandes_explication WHERE procedure_id = ?)", Boolean.class, procedureId);
        if (Boolean.TRUE.equals(exists)) throw conflict("Une demande d’explication existe déjà pour cette procédure.");
        LocalDate dateLimite = request.dateLimite() == null
            ? today().plusDays(paramInt("DELAI_REPONSE_JOURS", 5)) : request.dateLimite();
        if (dateLimite.isBefore(today())) throw bad("La date limite ne peut pas être antérieure à aujourd’hui.");
        Integer demandeId = jdbc.queryForObject("""
            INSERT INTO demandes_explication (procedure_id, message, date_limite, demandee_par_id)
            VALUES (?, ?, ?, ?) RETURNING id
            """, Integer.class, procedureId, request.message().trim(), dateLimite, rhId);
        Integer statutId = lookupCode("statuts_procedure_disciplinaire", "EXPLICATION_DEMANDEE", "Statut");
        jdbc.update("UPDATE procedures_disciplinaires SET statut_id = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
            statutId, procedureId);
        Integer employeId = (Integer) procedure.get("employeId");
        journal(rhId, "CREATION", "DEMANDE_EXPLICATION", demandeId, employeId,
            "Demande d’explication, limite " + dateLimite);
        Integer employeeUserId = employeeUserId(employeId);
        if (employeeUserId != null) {
            notifications.notifyUser(employeeUserId, "DISCIPLINE_EXPLICATION_DEMANDEE", "Demande d’explication",
                "Une explication vous est demandée. Répondez avant le " + dateLimite + ".", EMPLOYE_LINK);
        }
        return Map.of("procedureId", procedureId, "demandeId", demandeId, "dateLimite", dateLimite);
    }

    @Transactional
    public Map<String, Object> decide(Integer rhId, Integer procedureId, DecisionRequest request) {
        requireRole(rhId, "rh");
        Map<String, Object> procedure = jdbc.query("""
            SELECT pr.id, sp.code AS statut_code, i.employe_id, i.titre
            FROM procedures_disciplinaires pr
            JOIN statuts_procedure_disciplinaire sp ON sp.id = pr.statut_id
            JOIN incidents i ON i.id = pr.incident_id
            WHERE pr.id = ? FOR UPDATE OF pr
            """, rs -> {
                if (!rs.next()) return null;
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", rs.getInt("id"));
                row.put("statutCode", rs.getString("statut_code"));
                row.put("employeId", rs.getInt("employe_id"));
                row.put("titre", rs.getString("titre"));
                return row;
            }, procedureId);
        if (procedure == null) throw notFound("Procédure introuvable.");
        String statut = (String) procedure.get("statutCode");
        if (List.of("DECIDEE", "SANS_SUITE", "CLOTUREE").contains(statut)) {
            throw conflict("Une décision a déjà été prise pour cette procédure.");
        }
        Map<String, Object> demande = jdbc.query("""
            SELECT d.id, d.date_limite,
                   EXISTS(SELECT 1 FROM reponses_explication r WHERE r.demande_id = d.id) AS has_reponse
            FROM demandes_explication d WHERE d.procedure_id = ?
            """, rs -> {
                if (!rs.next()) return null;
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", rs.getInt("id"));
                row.put("dateLimite", rs.getDate("date_limite").toLocalDate());
                row.put("hasReponse", rs.getBoolean("has_reponse"));
                return row;
            }, procedureId);
        if (demande == null) throw conflict("Une demande d’explication doit être envoyée avant la décision.");
        boolean hasReponse = Boolean.TRUE.equals(demande.get("hasReponse"));
        LocalDate dateLimite = (LocalDate) demande.get("dateLimite");
        if (!hasReponse && !dateLimite.isBefore(today())) {
            throw conflict("Décision impossible : aucune réponse reçue et le délai n’est pas expiré.");
        }
        String decision = request.decision().trim().toUpperCase(Locale.ROOT);
        if (!List.of("SANS_SUITE", "AVERTISSEMENT", "BLAME", "MISE_A_PIED").contains(decision)) {
            throw bad("Décision invalide.");
        }
        Integer employeId = (Integer) procedure.get("employeId");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("procedureId", procedureId);
        result.put("decision", decision);
        if ("SANS_SUITE".equals(decision)) {
            Integer statutId = lookupCode("statuts_procedure_disciplinaire", "SANS_SUITE", "Statut");
            jdbc.update("""
                UPDATE procedures_disciplinaires
                SET statut_id = ?, cloturee_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """, statutId, procedureId);
            journal(rhId, "DECISION", "PROCEDURE", procedureId, employeId, "Décision : sans suite");
        } else {
            if (request.motif() == null || request.motif().isBlank()) {
                throw bad("Le motif de la sanction est obligatoire.");
            }
            Map<String, Object> type = jdbc.query("""
                SELECT id, code, libelle, retenue_salaire, necessite_periode
                FROM types_sanction WHERE code = ? AND actif = TRUE
                """, rs -> {
                    if (!rs.next()) return null;
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", rs.getInt("id"));
                    row.put("code", rs.getString("code"));
                    row.put("libelle", rs.getString("libelle"));
                    row.put("retenue", rs.getBoolean("retenue_salaire"));
                    row.put("periode", rs.getBoolean("necessite_periode"));
                    return row;
                }, decision);
            if (type == null) throw bad("Type de sanction inconnu.");
            boolean needPeriod = Boolean.TRUE.equals(type.get("periode"));
            LocalDate dateDebut = request.dateDebut();
            LocalDate dateFin = request.dateFin();
            Integer nombreJours = null;
            if (needPeriod) {
                if (dateDebut == null || dateFin == null || dateFin.isBefore(dateDebut)) {
                    throw bad("La période de la mise à pied est obligatoire et cohérente.");
                }
                if ("MISE_A_PIED".equals(decision)) {
                    assertNoRetroactivePunch(employeId, dateDebut, dateFin);
                    assertNoApprovedLeaveOverlap(employeId, dateDebut, dateFin);
                }
                nombreJours = sanctionDayCount(dateDebut, dateFin);
                if (nombreJours <= 0) throw bad("La période ne contient aucun jour ouvré.");
            } else {
                dateDebut = null;
                dateFin = null;
            }
            Integer sanctionId = jdbc.queryForObject("""
                INSERT INTO sanctions (procedure_id, type_id, motif, date_debut, date_fin, nombre_jours, decide_par_id)
                VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id
                """, Integer.class, procedureId, type.get("id"), request.motif().trim(),
                dateDebut, dateFin, nombreJours, rhId);
            boolean periodOngoing = needPeriod && !dateFin.isBefore(today());
            Integer statutId = lookupCode("statuts_procedure_disciplinaire",
                periodOngoing ? "DECIDEE" : "CLOTUREE", "Statut");
            if (periodOngoing) {
                jdbc.update("""
                    UPDATE procedures_disciplinaires
                    SET statut_id = ?, cloturee_at = NULL, updated_at = CURRENT_TIMESTAMP
                    WHERE id = ?
                    """, statutId, procedureId);
            } else {
                jdbc.update("""
                    UPDATE procedures_disciplinaires
                    SET statut_id = ?, cloturee_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
                    WHERE id = ?
                    """, statutId, procedureId);
            }
            int retenues = 0;
            List<String> avertissements = List.of();
            if (Boolean.TRUE.equals(type.get("retenue")) && needPeriod) {
                RetenuesResult calcul = createRetenues(sanctionId, employeId, dateDebut, dateFin, rhId);
                retenues = calcul.created();
                avertissements = calcul.avertissements();
            }
            if ("MISE_A_PIED".equals(decision) && needPeriod) {
                timeManagement.applyMiseAPiedPlanning(employeId, dateDebut, dateFin, rhId,
                    "Mise à pied (sanction #" + sanctionId + ")");
            }
            journal(rhId, "DECISION", "SANCTION", sanctionId, employeId, "Décision : " + type.get("libelle"));
            result.put("sanctionId", sanctionId);
            result.put("retenues", retenues);
            if (!avertissements.isEmpty()) result.put("avertissements", avertissements);
        }
        Integer employeeUserId = employeeUserId(employeId);
        if (employeeUserId != null) {
            notifications.notifyUser(employeeUserId, "DISCIPLINE_DECISION", "Décision disciplinaire",
                "Une décision a été prise concernant une procédure disciplinaire.", EMPLOYE_LINK);
        }
        return result;
    }

    @Transactional
    public List<Map<String, Object>> listProcedures(Integer rhId, String statut) {
        requireRole(rhId, "rh");
        StringBuilder sql = new StringBuilder("""
            SELECT pr.id, pr.ouverte_at AS "ouverteAt", pr.cloturee_at AS "clotureeAt",
                   sp.code AS "statutCode", sp.libelle AS statut,
                   i.id AS "incidentId", i.titre, i.date_faits AS "dateFaits",
                   g.code AS "graviteCode", g.libelle AS gravite,
                   e.id AS "employeId", e.matricule, COALESCE(u.full_name, u.username) AS employe,
                   p.intitule AS poste, d.nom AS departement,
                   de.id AS "demandeId", de.date_limite AS "dateLimite",
                   (SELECT COUNT(*) FROM reponses_explication r WHERE r.demande_id = de.id) AS "reponseRecue",
                   s.id AS "sanctionId", ts.code AS "sanctionCode", ts.libelle AS sanction
            FROM procedures_disciplinaires pr
            JOIN statuts_procedure_disciplinaire sp ON sp.id = pr.statut_id
            JOIN incidents i ON i.id = pr.incident_id
            JOIN gravites_incident g ON g.id = i.gravite_id
            JOIN employes e ON e.id = i.employe_id JOIN users u ON u.id = e.user_id
            JOIN postes p ON p.id = e.poste_id JOIN departements d ON d.id = p.departement_id
            LEFT JOIN demandes_explication de ON de.procedure_id = pr.id
            LEFT JOIN sanctions s ON s.procedure_id = pr.id
            LEFT JOIN types_sanction ts ON ts.id = s.type_id
            """);
        List<Object> params = new ArrayList<>();
        if (statut != null && !statut.isBlank()) {
            sql.append(" WHERE sp.code = ? ");
            params.add(statut.trim().toUpperCase(Locale.ROOT));
        }
        sql.append(" ORDER BY pr.ouverte_at DESC, pr.id DESC");
        List<Map<String, Object>> rows = jdbc.queryForList(sql.toString(), params.toArray());
        journal(rhId, "LECTURE", "PROCEDURES_RH", null, null, statut == null || statut.isBlank()
            ? "Consultation de la liste des procédures (" + rows.size() + ")"
            : "Consultation des procédures " + statut.trim().toUpperCase(Locale.ROOT) + " (" + rows.size() + ")");
        return rows;
    }

    @Transactional
    public Map<String, Object> procedureDetail(Integer rhId, Integer procedureId) {
        requireRole(rhId, "rh");
        List<Map<String, Object>> rows = jdbc.queryForList("""
            SELECT pr.id, pr.incident_id AS "incidentId", pr.ouverte_at AS "ouverteAt",
                   pr.cloturee_at AS "clotureeAt", pr.ouverte_par_id AS "ouverteParId",
                   sp.code AS "statutCode", sp.libelle AS statut,
                   i.titre, i.description, i.date_faits AS "dateFaits", i.created_at AS "declareAt",
                   i.declarant_id AS "declarantId",
                   g.code AS "graviteCode", g.libelle AS gravite,
                   e.id AS "employeId", e.matricule, COALESCE(u.full_name, u.username) AS employe,
                   p.intitule AS poste, d.nom AS departement,
                   de.id AS "demandeId", de.message AS "demandeMessage", de.date_limite AS "dateLimite",
                   de.demandee_at AS "demandeeAt", de.demandee_par_id AS "demandeeParId",
                   r.id AS "reponseId", r.texte AS "reponseTexte", r.repondue_at AS "repondueAt",
                   r.repondue_par_id AS "repondueParId",
                   s.id AS "sanctionId", s.motif AS "sanctionMotif", s.date_debut AS "sanctionDebut",
                   s.date_fin AS "sanctionFin", s.nombre_jours AS "sanctionJours",
                   s.decidee_at AS "sanctionDecideeAt",
                   ts.code AS "sanctionCode", ts.libelle AS sanction
            FROM procedures_disciplinaires pr
            JOIN statuts_procedure_disciplinaire sp ON sp.id = pr.statut_id
            JOIN incidents i ON i.id = pr.incident_id
            JOIN gravites_incident g ON g.id = i.gravite_id
            JOIN employes e ON e.id = i.employe_id JOIN users u ON u.id = e.user_id
            JOIN postes p ON p.id = e.poste_id JOIN departements d ON d.id = p.departement_id
            LEFT JOIN demandes_explication de ON de.procedure_id = pr.id
            LEFT JOIN reponses_explication r ON r.demande_id = de.id
            LEFT JOIN sanctions s ON s.procedure_id = pr.id
            LEFT JOIN types_sanction ts ON ts.id = s.type_id
            WHERE pr.id = ?
            """, procedureId);
        if (rows.isEmpty()) throw notFound("Procédure introuvable.");
        Map<String, Object> detail = new LinkedHashMap<>(rows.getFirst());
        Integer employeId = (Integer) detail.get("employeId");
        detail.put("pieces", jdbc.queryForList("""
            SELECT pc.id, pc.nom_fichier AS "nomFichier", pc.type_mime AS "typeMime",
                   pc.taille_octets AS "tailleOctets", pc.reponse_id AS "reponseId",
                   pc.ajoutee_at AS "ajouteeAt", pc.ajoutee_par_id AS "ajouteeParId"
            FROM pieces_disciplinaires pc WHERE pc.incident_id = ? ORDER BY pc.ajoutee_at, pc.id
            """, detail.get("incidentId")));
        detail.put("retenues", jdbc.queryForList("""
            SELECT rs.id, rs.date_debut AS "dateDebut", rs.date_fin AS "dateFin",
                   rs.nombre_jours AS "nombreJours", rs.taux_journalier AS "tauxJournalier", rs.montant,
                   rs.variable_paie_id AS "variablePaieId"
            FROM retenues_sanction rs JOIN sanctions sa ON sa.id = rs.sanction_id
            WHERE sa.procedure_id = ? ORDER BY rs.date_debut
            """, procedureId));
        journal(rhId, "LECTURE", "PROCEDURE", procedureId, employeId, "Consultation du détail de la procédure");
        return detail;
    }

    // ------------------------------------------------------------------
    // Employé (#18)
    // ------------------------------------------------------------------

    @Transactional
    public List<Map<String, Object>> employeeProcedures(Integer userId) {
        Integer employeId = activeEmployeeId(userId);
        List<Map<String, Object>> rows = jdbc.queryForList("""
            SELECT pr.id AS "procedureId", pr.ouverte_at AS "ouverteAt", pr.cloturee_at AS "clotureeAt",
                   sp.code AS "statutCode", sp.libelle AS statut,
                   i.id AS "incidentId", i.titre, i.description, i.date_faits AS "dateFaits",
                   g.code AS "graviteCode", g.libelle AS gravite,
                   de.id AS "demandeId", de.message AS "demandeMessage", de.date_limite AS "dateLimite",
                   de.demandee_at AS "demandeeAt",
                   r.id AS "reponseId", r.texte AS "reponseTexte", r.repondue_at AS "repondueAt",
                   s.id AS "sanctionId", ts.code AS "sanctionCode", ts.libelle AS sanction,
                   s.motif AS "sanctionMotif", s.date_debut AS "sanctionDebut", s.date_fin AS "sanctionFin",
                   (SELECT COUNT(*) FROM pieces_disciplinaires pc WHERE pc.incident_id = i.id
                      AND (pc.reponse_id IS NOT NULL OR pc.ajoutee_par_id = ?)) AS "nombrePieces"
            FROM procedures_disciplinaires pr
            JOIN statuts_procedure_disciplinaire sp ON sp.id = pr.statut_id
            JOIN incidents i ON i.id = pr.incident_id
            JOIN gravites_incident g ON g.id = i.gravite_id
            LEFT JOIN demandes_explication de ON de.procedure_id = pr.id
            LEFT JOIN reponses_explication r ON r.demande_id = de.id
            LEFT JOIN sanctions s ON s.procedure_id = pr.id
            LEFT JOIN types_sanction ts ON ts.id = s.type_id
            WHERE i.employe_id = ?
            ORDER BY pr.ouverte_at DESC, pr.id DESC
            """, userId, employeId);
        for (Map<String, Object> row : rows) {
            row.put("pieces", jdbc.queryForList("""
                SELECT pc.id, pc.nom_fichier AS "nomFichier", pc.type_mime AS "typeMime",
                       pc.taille_octets AS "tailleOctets", pc.reponse_id AS "reponseId",
                       pc.ajoutee_at AS "ajouteeAt"
                FROM pieces_disciplinaires pc
                WHERE pc.incident_id = ? AND (pc.reponse_id IS NOT NULL OR pc.ajoutee_par_id = ?)
                ORDER BY pc.ajoutee_at, pc.id
                """, row.get("incidentId"), userId));
        }
        journal(userId, "LECTURE", "DISCIPLINE_EMPLOYE", employeId, employeId, "Consultation de ses procédures");
        return rows;
    }

    @Transactional
    public Map<String, Object> respondToExplanation(Integer userId, Integer demandeId, String texte,
            List<MultipartFile> pieces) {
        Integer employeId = activeEmployeeId(userId);
        Map<String, Object> demande = jdbc.query("""
            SELECT d.id, d.procedure_id, pr.id AS procedure_id2, sp.code AS statut_code,
                   i.id AS incident_id, i.employe_id
            FROM demandes_explication d
            JOIN procedures_disciplinaires pr ON pr.id = d.procedure_id
            JOIN statuts_procedure_disciplinaire sp ON sp.id = pr.statut_id
            JOIN incidents i ON i.id = pr.incident_id
            WHERE d.id = ? FOR UPDATE OF d, pr
            """, rs -> {
                if (!rs.next()) return null;
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", rs.getInt("id"));
                row.put("procedureId", rs.getInt("procedure_id"));
                row.put("statutCode", rs.getString("statut_code"));
                row.put("incidentId", rs.getInt("incident_id"));
                row.put("employeId", rs.getInt("employe_id"));
                return row;
            }, demandeId);
        if (demande == null) throw notFound("Demande d’explication introuvable.");
        if (!employeId.equals(demande.get("employeId"))) throw notFound("Demande d’explication introuvable.");
        String statut = (String) demande.get("statutCode");
        if (!List.of("EXPLICATION_DEMANDEE", "OUVERTE").contains(statut)) {
            throw conflict("La procédure n’attend plus de réponse.");
        }
        Boolean hasReponse = jdbc.queryForObject(
            "SELECT EXISTS(SELECT 1 FROM reponses_explication WHERE demande_id = ?)", Boolean.class, demandeId);
        if (Boolean.TRUE.equals(hasReponse)) throw conflict("Une réponse a déjà été enregistrée.");
        if (texte == null || texte.isBlank()) throw bad("Le texte de la réponse est obligatoire.");
        Integer reponseId = jdbc.queryForObject("""
            INSERT INTO reponses_explication (demande_id, texte, repondue_par_id)
            VALUES (?, ?, ?) RETURNING id
            """, Integer.class, demandeId, texte.trim(), userId);
        Integer statutId = lookupCode("statuts_procedure_disciplinaire", "EXPLICATION_RECUE", "Statut");
        jdbc.update("UPDATE procedures_disciplinaires SET statut_id = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
            statutId, demande.get("procedureId"));
        Integer incidentId = (Integer) demande.get("incidentId");
        int stored = 0;
        if (pieces != null) {
            for (MultipartFile file : pieces) {
                if (file == null || file.isEmpty()) continue;
                DisciplineStorageService.StoredDocument document = storage.store(incidentId, file);
                Integer pieceId = jdbc.queryForObject("""
                    INSERT INTO pieces_disciplinaires
                        (incident_id, reponse_id, cle_stockage, nom_fichier, type_mime, taille_octets, ajoutee_par_id)
                    VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id
                    """, Integer.class, incidentId, reponseId, document.key(), document.fileName(),
                    document.mimeType(), document.size(), userId);
                journal(userId, "CREATION", "PIECE", pieceId, employeId, "Pièce jointe : " + document.fileName());
                stored++;
            }
        }
        journal(userId, "CREATION", "REPONSE", reponseId, employeId, "Réponse à la demande d’explication");
        notifications.notifyRole("rh", "DISCIPLINE_REPONSE_RECUE", "Réponse reçue",
            "L’employé a répondu à une demande d’explication disciplinaire.", RH_LINK);
        return Map.of("demandeId", demandeId, "reponseId", reponseId, "pieces", stored);
    }

    // ------------------------------------------------------------------
    // Pièces (#21, #22)
    // ------------------------------------------------------------------

    @Transactional
    public Document downloadPiece(Integer userId, Integer pieceId, boolean rhScope) {
        Integer employeId = null;
        if (rhScope) requireRole(userId, "rh");
        else employeId = activeEmployeeId(userId);
        List<Map<String, Object>> rows = jdbc.queryForList("""
            SELECT pc.id, pc.incident_id, pc.cle_stockage, pc.nom_fichier, pc.type_mime,
                   pc.reponse_id, pc.ajoutee_par_id, i.employe_id
            FROM pieces_disciplinaires pc JOIN incidents i ON i.id = pc.incident_id
            WHERE pc.id = ?
            """, pieceId);
        if (rows.isEmpty()) throw notFound("Pièce introuvable.");
        Map<String, Object> piece = rows.getFirst();
        Integer pieceEmployeId = (Integer) piece.get("employe_id");
        if (!rhScope && (!pieceEmployeId.equals(employeId) || piece.get("reponse_id") == null)) {
            throw notFound("Pièce introuvable.");
        }
        Integer incidentId = (Integer) piece.get("incident_id");
        byte[] bytes = storage.read((String) piece.get("cle_stockage"), incidentId);
        journal(userId, "TELECHARGEMENT", "PIECE", pieceId, pieceEmployeId,
            "Téléchargement : " + piece.get("nom_fichier"));
        return new Document(bytes, (String) piece.get("nom_fichier"), (String) piece.get("type_mime"));
    }

    // ------------------------------------------------------------------
    // Tâche planifiée (#23)
    // ------------------------------------------------------------------

    @Transactional
    public int notifyExpiredDeadlines() {
        List<Map<String, Object>> expired = jdbc.queryForList("""
            SELECT d.id AS "demandeId", d.date_limite AS "dateLimite",
                   pr.id AS "procedureId", i.employe_id AS "employeId", i.titre
            FROM demandes_explication d
            JOIN procedures_disciplinaires pr ON pr.id = d.procedure_id
            JOIN statuts_procedure_disciplinaire sp ON sp.id = pr.statut_id AND sp.code = 'EXPLICATION_DEMANDEE'
            JOIN incidents i ON i.id = pr.incident_id
            LEFT JOIN reponses_explication r ON r.demande_id = d.id
            WHERE r.id IS NULL AND d.date_limite < ?
            """, today());
        int count = 0;
        for (Map<String, Object> row : expired) {
            Integer demandeId = (Integer) row.get("demandeId");
            Boolean already = jdbc.queryForObject("""
                SELECT EXISTS(SELECT 1 FROM journal_acces_discipline
                    WHERE objet_type = 'DEMANDE_EXPLICATION' AND objet_id = ? AND action = 'MODIFICATION'
                      AND detail LIKE 'Délai expiré%')
                """, Boolean.class, demandeId);
            if (Boolean.TRUE.equals(already)) continue;
            notifications.notifyRole("rh", "DISCIPLINE_DELAI_EXPIRE", "Délai de réponse expiré",
                "Le délai de réponse à la demande d’explication de l’incident « " + row.get("titre")
                    + " » est expiré.", RH_LINK);
            journalSystem("MODIFICATION", "DEMANDE_EXPLICATION", demandeId,
                (Integer) row.get("employeId"), "Délai expiré - RH notifiée");
            count++;
        }
        return count;
    }

    /**
     * Clôture les procédures décidées dont la sanction est purgée (période terminée).
     * Les décisions sans période (avertissement, blâme) sont déjà clôturées à la décision.
     */
    @Transactional
    public int closeCompletedProcedures() {
        List<Map<String, Object>> rows = jdbc.queryForList("""
            SELECT pr.id, i.employe_id AS "employeId"
            FROM procedures_disciplinaires pr
            JOIN statuts_procedure_disciplinaire sp ON sp.id = pr.statut_id AND sp.code = 'DECIDEE'
            JOIN incidents i ON i.id = pr.incident_id
            LEFT JOIN sanctions s ON s.procedure_id = pr.id
            WHERE s.id IS NULL OR s.date_fin IS NULL OR s.date_fin < ?
            ORDER BY pr.id
            """, today());
        if (rows.isEmpty()) return 0;
        Integer statutId = lookupCode("statuts_procedure_disciplinaire", "CLOTUREE", "Statut");
        for (Map<String, Object> row : rows) {
            Integer procedureId = ((Number) row.get("id")).intValue();
            Integer employeId = row.get("employeId") == null ? null : ((Number) row.get("employeId")).intValue();
            jdbc.update("""
                UPDATE procedures_disciplinaires
                SET statut_id = ?, cloturee_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """, statutId, procedureId);
            journalSystem("MODIFICATION", "PROCEDURE", procedureId, employeId,
                "Procédure clôturée automatiquement (sanction purgée)");
        }
        return rows.size();
    }

    // ------------------------------------------------------------------
    // Retenues de sanction
    // ------------------------------------------------------------------

    private record RetenuesResult(int created, List<String> avertissements) {}

    private RetenuesResult createRetenues(Integer sanctionId, Integer employeId, LocalDate dateDebut,
            LocalDate dateFin, Integer rhId) {
        BigDecimal salary = activeSalary(employeId);
        if (salary == null) throw conflict("Aucun contrat actif avec salaire n’est rattaché à cet employé.");
        BigDecimal diviseur = paramDecimal("DIVISEUR_TAUX_JOURNALIER", new BigDecimal("30"));
        if (diviseur.signum() <= 0) diviseur = new BigDecimal("30");
        BigDecimal taux = salary.divide(diviseur, 2, RoundingMode.HALF_UP);
        String mode = paramString("MODE_COMPTAGE_RETENUE", "CALENDAIRE");
        int created = 0;
        List<String> avertissements = new ArrayList<>();
        LocalDate cursor = dateDebut;
        while (!cursor.isAfter(dateFin)) {
            LocalDate monthEnd = cursor.withDayOfMonth(cursor.lengthOfMonth());
            LocalDate sliceStart = cursor;
            LocalDate sliceEnd = monthEnd.isAfter(dateFin) ? dateFin : monthEnd;
            int jours = "OUVRE".equalsIgnoreCase(mode)
                ? workingDaysBetween(sliceStart, sliceEnd)
                : (int) (ChronoUnit.DAYS.between(sliceStart, sliceEnd) + 1);
            if (jours > 0) {
                BigDecimal montant = taux.multiply(BigDecimal.valueOf(jours)).setScale(2, RoundingMode.HALF_UP);
                Integer retenueId = jdbc.queryForObject("""
                    INSERT INTO retenues_sanction
                        (sanction_id, date_debut, date_fin, nombre_jours, taux_journalier, montant)
                    VALUES (?, ?, ?, ?, ?, ?) RETURNING id
                    """, Integer.class, sanctionId, sliceStart, sliceEnd, jours, taux, montant);
                created++;
                String source = "Mise à pied #" + sanctionId + " du " + sliceStart + " au " + sliceEnd
                    + " : " + jours + " j × " + taux.toPlainString() + " Ar (salaire "
                    + salary.toPlainString() + " / " + diviseur.toPlainString() + ")";
                Integer periodId = openPeriodCovering(sliceStart, sliceEnd);
                if (periodId != null) {
                    Integer variableId = jdbc.queryForObject("""
                        INSERT INTO variables_paie
                            (periode_id, employe_id, rubrique_id, quantite, montant_unitaire, montant,
                             source_detail, statut_id, saisi_par_id, valide_par_id, valide_at,
                             motif_decision, sanction_id)
                        SELECT ?, ?, r.id, ?, ?, ?, ?, sv.id, ?, ?, CURRENT_TIMESTAMP, ?, ?
                        FROM rubriques_paie r, statuts_variable_paie sv
                        WHERE r.code = 'RETENUE_MISE_A_PIED' AND sv.code = 'VALIDEE'
                        RETURNING id
                        """, Integer.class, periodId, employeId, jours, taux, montant, source, rhId, rhId,
                        "Retenue automatique - sanction #" + sanctionId, sanctionId);
                    jdbc.update("UPDATE retenues_sanction SET variable_paie_id = ? WHERE id = ?",
                        variableId, retenueId);
                } else if (periodExistsCovering(sliceStart, sliceEnd)) {
                    avertissements.add("La période de paie couvrant le " + sliceStart + " au " + sliceEnd
                        + " n’est plus en brouillon : la retenue reste en attente d’intégration.");
                }
            }
            cursor = sliceEnd.plusDays(1);
        }
        return new RetenuesResult(created, avertissements);
    }

    private Integer openPeriodCovering(LocalDate start, LocalDate end) {
        return jdbc.query("""
            SELECT p.id FROM periodes_paie p
            JOIN statuts_periode_paie sp ON sp.id = p.statut_id AND sp.code = 'BROUILLON'
            WHERE p.date_debut <= ? AND p.date_fin >= ?
            ORDER BY p.id DESC LIMIT 1
            """, rs -> rs.next() ? rs.getInt(1) : null, start, end);
    }

    private boolean periodExistsCovering(LocalDate start, LocalDate end) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
            SELECT EXISTS(SELECT 1 FROM periodes_paie p WHERE p.date_debut <= ? AND p.date_fin >= ?)
            """, Boolean.class, start, end));
    }

    private BigDecimal activeSalary(Integer employeId) {
        return jdbc.query("""
            SELECT c.salaire_base_mensuel FROM employes e
            JOIN candidatures ca ON ca.candidat_id = e.user_id
            JOIN contrats c ON c.candidature_id = ca.id
            JOIN statuts_contrat sc ON sc.id = c.statut_id AND sc.code = 'ACTIF'
            WHERE e.id = ? ORDER BY c.date_debut DESC, c.id DESC LIMIT 1
            """, rs -> rs.next() ? rs.getBigDecimal(1) : null, employeId);
    }

    private int workingDaysBetween(LocalDate start, LocalDate end) {
        int count = 0;
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            if (isWorkday(date)) count++;
        }
        return count;
    }

    private boolean isWorkday(LocalDate date) {
        Boolean holiday = jdbc.queryForObject(
            "SELECT EXISTS(SELECT 1 FROM jours_feries WHERE date_ferie = ? AND actif = TRUE)", Boolean.class, date);
        if (Boolean.TRUE.equals(holiday)) return false;
        Boolean open = jdbc.queryForObject(
            "SELECT ouvrable_par_defaut FROM jours_semaine WHERE numero_iso = ? AND actif = TRUE",
            Boolean.class, date.getDayOfWeek().getValue());
        return Boolean.TRUE.equals(open);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private Map<String, Object> incidentSummary(Integer incidentId) {
        return jdbc.queryForMap("""
            SELECT i.id, i.titre, i.description, i.date_faits AS "dateFaits", i.created_at AS "createdAt",
                   i.employe_id AS "employeId", i.declarant_id AS "declarantId",
                   g.code AS "graviteCode", g.libelle AS gravite
            FROM incidents i JOIN gravites_incident g ON g.id = i.gravite_id
            WHERE i.id = ?
            """, incidentId);
    }

    private Map<String, Object> procedureSummary(Integer procedureId) {
        return jdbc.queryForMap("""
            SELECT pr.id, pr.incident_id AS "incidentId", pr.ouverte_at AS "ouverteAt",
                   pr.cloturee_at AS "clotureeAt", sp.code AS "statutCode", sp.libelle AS statut,
                   i.employe_id AS "employeId", i.titre, i.date_faits AS "dateFaits"
            FROM procedures_disciplinaires pr
            JOIN statuts_procedure_disciplinaire sp ON sp.id = pr.statut_id
            JOIN incidents i ON i.id = pr.incident_id
            WHERE pr.id = ?
            """, procedureId);
    }

    private void requireRole(Integer userId, String role) {
        Boolean allowed = jdbc.queryForObject("""
            SELECT EXISTS (SELECT 1 FROM users u
              JOIN statuts_validation_compte s ON s.id = u.administrative_validation_status_id
              JOIN user_roles ur ON ur.user_id = u.id JOIN roles r ON r.id = ur.role_id
              WHERE u.id = ? AND u.is_suspended = FALSE AND s.code = 'APPROVED' AND LOWER(r.role_name) = LOWER(?))
            """, Boolean.class, userId, role);
        if (!Boolean.TRUE.equals(allowed)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Un compte " + role + " validé est requis.");
        }
    }

    private Integer activeEmployeeId(Integer userId) {
        Integer id = jdbc.query("""
            SELECT e.id FROM employes e
            JOIN users u ON u.id = e.user_id AND u.is_suspended = FALSE
            LEFT JOIN situation_administrative_employe sa ON sa.employe_id = e.id
            LEFT JOIN statuts_administratifs_employe se ON se.id = sa.statut_id
            WHERE e.user_id = ? AND COALESCE(se.code, 'ACTIF') = 'ACTIF'
            """, rs -> rs.next() ? rs.getInt(1) : null, userId);
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Aucun dossier salarié actif n’est lié à ce compte.");
        }
        return id;
    }

    private Integer employeeUserId(Integer employeId) {
        return jdbc.query("SELECT user_id FROM employes WHERE id = ?",
            rs -> rs.next() ? rs.getInt(1) : null, employeId);
    }

    private Integer employeeDepartment(Integer employeId) {
        return jdbc.query("""
            SELECT p.departement_id FROM employes e JOIN postes p ON p.id = e.poste_id WHERE e.id = ?
            """, rs -> rs.next() ? rs.getInt(1) : null, employeId);
    }

    private boolean employeeExists(Integer employeId) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
            "SELECT EXISTS(SELECT 1 FROM employes WHERE id = ?)", Boolean.class, employeId));
    }

    private void assertNoApprovedLeaveOverlap(Integer employeId, LocalDate dateDebut, LocalDate dateFin) {
        List<Map<String, Object>> leaves = jdbc.queryForList("""
            SELECT d.date_debut, d.date_fin
            FROM demandes_conge d JOIN statuts_demande_conge s ON s.id = d.statut_id
            WHERE d.employe_id = ? AND s.code = 'APPROUVEE'
              AND d.date_debut <= ? AND d.date_fin >= ?
            ORDER BY d.date_debut LIMIT 1
            """, employeId, dateFin, dateDebut);
        if (leaves.isEmpty()) return;
        LocalDate leaveStart = toLocalDate(leaves.getFirst().get("date_debut"));
        LocalDate leaveEnd = toLocalDate(leaves.getFirst().get("date_fin"));
        throw conflict("Impossible de prononcer une mise à pied du " + dateDebut + " au " + dateFin
            + " : cette période chevauche un congé approuvé du " + leaveStart + " au " + leaveEnd + ".");
    }

    private int sanctionDayCount(LocalDate start, LocalDate end) {
        String mode = paramString("MODE_COMPTAGE_RETENUE", "CALENDAIRE");
        if ("OUVRE".equalsIgnoreCase(mode)) return workingDaysBetween(start, end);
        return (int) (ChronoUnit.DAYS.between(start, end) + 1);
    }

    private void assertNoRetroactivePunch(Integer employeId, LocalDate dateDebut, LocalDate dateFin) {
        if (dateDebut.isBefore(today())) {
            throw conflict("Impossible de prononcer une mise à pied rétroactive : la date de début ("
                + dateDebut + ") est antérieure à aujourd’hui.");
        }
        List<java.sql.Date> punched = jdbc.queryForList("""
            SELECT DISTINCT sp.date_service
            FROM sessions_pointage sp
            JOIN evenements_pointage ep ON ep.session_id = sp.id
            WHERE sp.employe_id = ? AND sp.date_service BETWEEN ? AND ?
            ORDER BY sp.date_service
            """, java.sql.Date.class, employeId, dateDebut, dateFin);
        if (!punched.isEmpty()) {
            throw conflict("Impossible de prononcer une mise à pied : la journée du "
                + punched.getFirst().toLocalDate() + " a déjà été pointée par l’employé.");
        }
    }

    private static LocalDate toLocalDate(Object value) {
        if (value instanceof LocalDate localDate) return localDate;
        if (value instanceof java.sql.Date sqlDate) return sqlDate.toLocalDate();
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Date de congé illisible.");
    }

    private Integer lookupCode(String table, String code, String label) {
        if (!List.of("gravites_incident", "statuts_procedure_disciplinaire", "types_sanction").contains(table)) {
            throw new IllegalArgumentException("Référentiel non autorisé.");
        }
        Integer id = jdbc.query("SELECT id FROM " + table + " WHERE code = ?",
            rs -> rs.next() ? rs.getInt(1) : null, code.trim().toUpperCase(Locale.ROOT));
        if (id == null) throw bad(label + " inconnu : " + code);
        return id;
    }

    private void journal(Integer userId, String action, String objetType, Integer objetId,
            Integer employeId, String detail) {
        String safeDetail = detail == null ? null : (detail.length() > 300 ? detail.substring(0, 300) : detail);
        jdbc.update("""
            INSERT INTO journal_acces_discipline
                (utilisateur_id, action, objet_type, objet_id, employe_id, detail)
            VALUES (?, ?, ?, ?, ?, ?)
            """, userId, action, objetType, objetId, employeId, safeDetail);
    }

    private void journalSystem(String action, String objetType, Integer objetId,
            Integer employeId, String detail) {
        String safeDetail = detail == null ? null : (detail.length() > 300 ? detail.substring(0, 300) : detail);
        jdbc.update("""
            INSERT INTO journal_acces_discipline
                (utilisateur_id, origine, action, objet_type, objet_id, employe_id, detail)
            VALUES (NULL, 'SYSTEME', ?, ?, ?, ?, ?)
            """, action, objetType, objetId, employeId, safeDetail);
    }

    private int paramInt(String code, int def) {
        String value = paramString(code, null);
        if (value == null) return def;
        try { return Integer.parseInt(value.trim()); } catch (NumberFormatException exception) { return def; }
    }

    private BigDecimal paramDecimal(String code, BigDecimal def) {
        String value = paramString(code, null);
        if (value == null) return def;
        try { return new BigDecimal(value.trim()); } catch (NumberFormatException exception) { return def; }
    }

    private String paramString(String code, String def) {
        return jdbc.query("SELECT valeur FROM parametres_discipline WHERE code = ?",
            rs -> rs.next() ? rs.getString(1) : def, code);
    }

    private LocalDate today() { return LocalDate.now(HR_ZONE); }

    private ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }

    private ResponseStatusException forbidden(String message) {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, message);
    }

    private ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }

    public record Document(byte[] bytes, String fileName, String mimeType) { }
}
