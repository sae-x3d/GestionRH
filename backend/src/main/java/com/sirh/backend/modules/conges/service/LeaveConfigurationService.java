package com.sirh.backend.modules.conges.service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.sirh.backend.modules.notifications.service.NotificationCenterService;

@Service
public class LeaveConfigurationService {
    private final JdbcTemplate jdbc;
    private final NotificationCenterService notifications;

    public LeaveConfigurationService(JdbcTemplate jdbc, NotificationCenterService notifications) { this.jdbc = jdbc; this.notifications = notifications; }

    @Transactional(readOnly = true)
    public Map<String, Object> configuration(Integer actorId) {
        requireRh(actorId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("effectifs", jdbc.queryForList("""
            SELECT p.id AS "posteId", d.nom AS departement, d.code AS "codeDepartement",
                   p.intitule AS poste, p.effectif_autorise AS "effectifAutorise",
                   (SELECT COUNT(*) FROM employes e
                    LEFT JOIN situation_administrative_employe sa ON sa.employe_id=e.id
                    LEFT JOIN statuts_administratifs_employe se ON se.id=sa.statut_id
                    WHERE e.poste_id=p.id AND e.date_embauche<=CURRENT_DATE
                      AND COALESCE(se.code,'ACTIF') NOT IN ('SORTI','EN_PREPARATION')) AS "effectifActif",
                   COALESCE(regle.minimum_presence, 0) AS "effectifMinimal",
                   regle.date_debut AS "minimumDateDebut",
                   GREATEST(0, p.effectif_autorise - COALESCE((SELECT COUNT(*) FROM employes e
                    LEFT JOIN situation_administrative_employe sa ON sa.employe_id=e.id
                    LEFT JOIN statuts_administratifs_employe se ON se.id=sa.statut_id
                    WHERE e.poste_id=p.id AND e.date_embauche<=CURRENT_DATE
                      AND COALESCE(se.code,'ACTIF') NOT IN ('SORTI','EN_PREPARATION')), 0)
                    - COALESCE((SELECT SUM(GREATEST(dr.effectif-dr.effectif_recrute,0))
                      FROM demandes_recrutement dr JOIN motifs_recrutement mr ON mr.id=dr.motif_id
                      JOIN statuts_demande_recrutement sd ON sd.id=dr.statut_id
                      WHERE dr.poste_id=p.id AND dr.date_souhaitee<=CURRENT_DATE
                        AND sd.code NOT IN ('REFUSEE','ANNULEE')
                        AND (mr.code='POSTE_VACANT' OR (mr.code='AUGMENT_EFFECTIF' AND sd.code='OFFRE_CREEE'))),0),0) AS "postesARecruter"
            FROM postes p JOIN departements d ON d.id=p.departement_id
            LEFT JOIN LATERAL (
                SELECT r.minimum_presence, r.date_debut FROM regles_effectif_minimal r
                WHERE r.poste_id=p.id AND r.actif=TRUE AND r.date_debut<=CURRENT_DATE
                  AND (r.date_fin IS NULL OR r.date_fin>=CURRENT_DATE)
                ORDER BY r.date_debut DESC LIMIT 1
            ) regle ON TRUE
            WHERE p.actif=TRUE AND d.actif=TRUE
            ORDER BY d.nom,p.intitule
            """));
        result.put("typesConge", jdbc.queryForList("""
            SELECT t.id AS "typeId", t.code, t.libelle, t.actif, t.justificatif_requis AS "justificatifRequis",
                   t.acquisition_active AS "acquisitionActive", r.jours_par_mois AS "joursParMois",
                   r.date_debut AS "dateDebut"
            FROM types_conge t
            LEFT JOIN LATERAL (
                SELECT a.jours_par_mois, a.date_debut FROM regles_acquisition_conge a
                WHERE a.type_conge_id=t.id AND a.actif=TRUE AND a.date_debut<=CURRENT_DATE
                  AND (a.date_fin IS NULL OR a.date_fin>=CURRENT_DATE)
                ORDER BY a.date_debut DESC LIMIT 1
            ) r ON TRUE
            ORDER BY t.libelle
            """));
        return result;
    }

    @Transactional
    public Map<String, Object> setMinimum(Integer actorId, Integer posteId, Integer minimum, LocalDate dateDebut, String motif) {
        requireRh(actorId);
        if (minimum == null || minimum < 0 || dateDebut == null || dateDebut.isBefore(LocalDate.now())) {
            throw bad("Le minimum doit être positif ou nul et sa date d’effet ne peut pas être passée.");
        }
        if (motif == null || motif.isBlank() || motif.trim().length() > 500) {
            throw bad("Un motif de 1 à 500 caractères est obligatoire pour tracer le changement.");
        }
        if (!Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM postes WHERE id=? AND actif=TRUE)", Boolean.class, posteId))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Poste actif introuvable.");
        }
        closeCurrentRule("regles_effectif_minimal", "poste_id", posteId, dateDebut);
        jdbc.update("""
            INSERT INTO regles_effectif_minimal(poste_id,minimum_presence,date_debut,actif,modifie_par_id,motif)
            VALUES (?,?,?,TRUE,?,?)
            """, posteId, minimum, dateDebut, actorId, motif.trim());
        Integer active = jdbc.queryForObject("""
            SELECT COUNT(*) FROM employes e LEFT JOIN situation_administrative_employe sa ON sa.employe_id=e.id
            LEFT JOIN statuts_administratifs_employe se ON se.id=sa.statut_id
            WHERE e.poste_id=? AND e.date_embauche<=CURRENT_DATE
              AND COALESCE(se.code,'ACTIF') NOT IN ('SORTI','EN_PREPARATION')
            """, Integer.class, posteId);
        if (active != null && active < minimum) {
            String dept = jdbc.queryForObject("SELECT d.nom FROM postes p JOIN departements d ON d.id=p.departement_id WHERE p.id=?", String.class, posteId);
            String title = jdbc.queryForObject("SELECT intitule FROM postes WHERE id=?", String.class, posteId);
            String message = "Effectif actif sous le seuil du poste " + title + " (« " + dept + " ») : " + active + "/" + minimum + ". Vérifiez les postes vacants et une demande de recrutement.";
            List<Integer> managers = jdbc.queryForList("""
                SELECT DISTINCT e.user_id FROM employes e JOIN postes p ON p.id=e.poste_id
                JOIN user_roles ur ON ur.user_id=e.user_id JOIN roles r ON r.id=ur.role_id
                JOIN users u ON u.id=e.user_id AND u.is_suspended=FALSE
                WHERE p.departement_id=(SELECT departement_id FROM postes WHERE id=?) AND LOWER(r.role_name)='manager'
                """, Integer.class, posteId);
            for (Integer managerId : managers) notifications.notifyUser(managerId, "EFFECTIF_SOUS_MINIMUM", "Alerte de sous-effectif", message, "/espace/manager/demandes/nouvelle");
            notifications.notifyRole("rh", "EFFECTIF_SOUS_MINIMUM", "Alerte de sous-effectif", message, "/espace/rh/conges/parametres");
        }
        return configuration(actorId);
    }

    @Transactional
    public Map<String, Object> setAccrual(Integer actorId, Integer typeId, java.math.BigDecimal joursParMois,
            LocalDate dateDebut) {
        requireRh(actorId);
        if (joursParMois == null || joursParMois.signum() < 0 || dateDebut == null || dateDebut.isBefore(LocalDate.now())) {
            throw bad("Le taux mensuel doit être positif ou nul et sa date d’effet ne peut pas être passée.");
        }
        if (!Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM types_conge WHERE id=? AND actif=TRUE AND acquisition_active=TRUE)",
                Boolean.class, typeId))) throw bad("Ce type de congé n’acquiert pas de solde.");
        closeCurrentRule("regles_acquisition_conge", "type_conge_id", typeId, dateDebut);
        jdbc.update("""
            INSERT INTO regles_acquisition_conge(type_conge_id,jours_par_mois,date_debut,actif,modifie_par_id)
            VALUES (?,?,?,TRUE,?)
            """, typeId, joursParMois, dateDebut, actorId);
        return configuration(actorId);
    }

    @Transactional
    public Map<String, Object> setTypePolicy(Integer actorId, Integer typeId, boolean active, boolean justificationRequired) {
        requireRh(actorId);
        if (!Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM types_conge WHERE id=?)", Boolean.class, typeId)))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Type de congé introuvable.");
        if (!active && Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS(SELECT 1 FROM demandes_conge d JOIN statuts_demande_conge s ON s.id=d.statut_id
                  WHERE d.type_conge_id=? AND s.code IN ('A_VALIDER_MANAGER','A_VALIDER_RH'))
                """, Boolean.class, typeId))) throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ce type ne peut pas être désactivé tant que des demandes sont en cours.");
        jdbc.update("UPDATE types_conge SET actif=?,justificatif_requis=? WHERE id=?",
            active, justificationRequired, typeId);
        return configuration(actorId);
    }

    private void closeCurrentRule(String table, String key, Integer id, LocalDate dateDebut) {
        // table/key proviennent uniquement de ces deux appels internes, jamais d'une entrée utilisateur.
        LocalDate currentStart = jdbc.query("SELECT date_debut FROM " + table + " WHERE " + key + "=? AND actif=TRUE AND date_fin IS NULL FOR UPDATE",
            rs -> rs.next() ? rs.getDate(1).toLocalDate() : null, id);
        if (currentStart != null) {
            if (!dateDebut.isAfter(currentStart)) throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Une règle existe déjà à partir de cette date. Choisissez une date ultérieure.");
            jdbc.update("UPDATE " + table + " SET actif=FALSE,date_fin=? WHERE " + key + "=? AND actif=TRUE AND date_fin IS NULL",
                dateDebut.minusDays(1), id);
        }
    }

    private void requireRh(Integer actorId) {
        boolean allowed = Boolean.TRUE.equals(jdbc.queryForObject("""
            SELECT EXISTS (SELECT 1 FROM users u
              JOIN statuts_validation_compte s ON s.id=u.administrative_validation_status_id AND s.code='APPROVED'
              JOIN user_roles ur ON ur.user_id=u.id JOIN roles r ON r.id=ur.role_id
              WHERE u.id=? AND u.is_suspended=FALSE AND LOWER(r.role_name)='rh')
            """, Boolean.class, actorId));
        if (!allowed) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Un compte RH validé est requis.");
    }

    private ResponseStatusException bad(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
}
