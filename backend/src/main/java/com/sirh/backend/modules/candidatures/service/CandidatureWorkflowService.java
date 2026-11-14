package com.sirh.backend.modules.candidatures.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import com.sirh.backend.modules.candidatures.dto.CandidatureCandidateResponseRequest;
import com.sirh.backend.modules.candidatures.dto.CandidatureWorkflowActionRequest;
import com.sirh.backend.modules.candidatures.dto.RhOfferCandidateListResponse;
import com.sirh.backend.modules.notifications.service.NotificationCenterService;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CandidatureWorkflowService {
    private final JdbcTemplate jdbc;
    private final NotificationCenterService notifications;
    private final ContractDocumentService contractDocuments;

    public CandidatureWorkflowService(JdbcTemplate jdbc, NotificationCenterService notifications,
            ContractDocumentService contractDocuments) {
        this.jdbc = jdbc;
        this.notifications = notifications;
        this.contractDocuments = contractDocuments;
    }

    @Transactional(readOnly = true)
    public List<RhOfferCandidateListResponse> offersForRh(Integer rhId) {
        requireRh(rhId);
        return jdbc.query("""
            SELECT o.id, o.titre, p.intitule AS poste, d.nom AS departement,
                   dr.effectif, dr.effectif_recrute,
                   GREATEST(dr.effectif - dr.effectif_recrute, 0)::integer AS effectif_restant,
                   COUNT(DISTINCT c.id)::integer AS nombre_candidatures,
                   CASE WHEN so.code = 'PUBLIEE' AND (o.date_cloture < CURRENT_DATE OR dr.effectif_recrute >= dr.effectif)
                        THEN 'CLOTUREE' ELSE so.code END AS statut_code,
                   CASE WHEN so.code = 'PUBLIEE' AND (o.date_cloture < CURRENT_DATE OR dr.effectif_recrute >= dr.effectif)
                        THEN 'Clôturée' ELSE so.libelle END AS statut,
                   o.date_cloture
            FROM offres_emploi o
            JOIN demandes_recrutement dr ON dr.id = o.demande_id
            JOIN postes p ON p.id = dr.poste_id
            JOIN departements d ON d.id = dr.departement_id
            JOIN statuts_offre_emploi so ON so.id = o.statut_id
            LEFT JOIN candidatures c ON c.offre_id = o.id
            GROUP BY o.id, p.intitule, d.nom, dr.effectif, dr.effectif_recrute, so.code, so.libelle
            ORDER BY o.created_at DESC, o.id DESC
            """, (row, index) -> new RhOfferCandidateListResponse(row.getInt("id"), row.getString("titre"),
                row.getString("poste"), row.getString("departement"), row.getInt("effectif"),
                row.getInt("effectif_recrute"), row.getInt("effectif_restant"), row.getInt("nombre_candidatures"),
                row.getString("statut_code"), row.getString("statut"), row.getObject("date_cloture", LocalDate.class)));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> rhDetail(Integer rhId, Integer candidatureId) {
        requireRh(rhId);
        Map<String, Object> detail = one("""
            SELECT c.id, o.id AS offre_id, c.candidat_id, COALESCE(u.full_name, u.username) AS candidat,
                   u.username, u.email, u.photo_profil_path, o.titre AS offre, p.intitule AS poste,
                   d.nom AS departement, dr.budget_salaire_base, dr.date_souhaitee, sc.code AS statut_code,
                   sc.libelle AS statut, c.envoyee_at, c.cv_path,
                   ct.numero AS numero_contrat, ct.salaire_base_mensuel, ct.date_debut AS contrat_date_debut,
                   ct.genere_at, tc.libelle AS type_contrat
            FROM candidatures c JOIN users u ON u.id = c.candidat_id
            JOIN offres_emploi o ON o.id = c.offre_id
            JOIN demandes_recrutement dr ON dr.id = o.demande_id
            JOIN postes p ON p.id = dr.poste_id JOIN departements d ON d.id = dr.departement_id
            JOIN statuts_candidature sc ON sc.id = c.statut_id
            LEFT JOIN contrats ct ON ct.candidature_id = c.id LEFT JOIN types_contrat tc ON tc.id = ct.type_contrat_id
            WHERE c.id = ?
            """, candidatureId);
        detail.put("photoUrl", detail.get("photo_profil_path") == null ? null : "/api/rh/candidatures/" + candidatureId + "/photo");
        detail.remove("photo_profil_path");
        detail.remove("cv_path");
        detail.remove("candidat_id");
        detail.put("parcours", jdbc.queryForList("""
            SELECT tp.libelle AS type, p.intitule, p.organisme, p.description,
                   ne.libelle AS niveau, du.date_debut, du.date_fin, du.en_cours
            FROM parcours p JOIN types_parcours tp ON tp.id = p.type_parcours_id
            LEFT JOIN niveaux_etude ne ON ne.id = p.niveau_etude_id
            JOIN durees du ON du.parcours_id = p.id
            JOIN candidatures c ON c.id = p.candidature_id WHERE c.id = ? ORDER BY p.ordre
            """, candidatureId));
        detail.put("entretien", jdbc.queryForList("""
            SELECT te.libelle AS type, se.code AS statut_code, se.libelle AS statut,
                   ec.planifie_at, ec.note
            FROM entretiens_candidature ec JOIN types_entretien te ON te.id = ec.type_id
            JOIN statuts_entretien se ON se.id = ec.statut_id
            WHERE ec.candidature_id = ? ORDER BY ec.id DESC LIMIT 1
            """, candidatureId).stream().findFirst().orElse(null));
        detail.put("questionnaire", jdbc.queryForList("""
            SELECT qe.regles_acceptees, qe.date_naissance, qe.date_debut_souhaitee,
                   qe.salaire_base_souhaite, tcs.libelle AS type_contrat_souhaite,
                   qe.salaire_base_final, qe.date_debut_finale, tcf.libelle AS type_contrat_final,
                   qe.salaire_base_propose_candidat, qe.date_debut_proposee_candidat,
                   tcp.libelle AS type_contrat_propose_candidat, qe.commentaire_candidat
            FROM questionnaires_embauche qe
            JOIN types_contrat tcs ON tcs.id = qe.type_contrat_souhaite_id
            LEFT JOIN types_contrat tcf ON tcf.id = qe.type_contrat_final_id
            LEFT JOIN types_contrat tcp ON tcp.id = qe.type_contrat_propose_candidat_id
            WHERE qe.candidature_id = ?
            """, candidatureId).stream().findFirst().orElse(null));
        detail.put("historique", jdbc.queryForList("""
            SELECT sc.libelle AS statut, hc.commentaire, hc.created_at,
                   COALESCE(u.full_name, u.username, 'Système') AS acteur
            FROM historique_candidatures hc JOIN statuts_candidature sc ON sc.id = hc.statut_id
            LEFT JOIN users u ON u.id = hc.acteur_id WHERE hc.candidature_id = ?
            ORDER BY hc.created_at DESC, hc.id DESC
            """, candidatureId));
        return detail;
    }

    @Transactional
    public Map<String, Object> actAsRh(Integer rhId, Integer candidatureId, CandidatureWorkflowActionRequest request) {
        requireRh(rhId);
        Map<String, Object> application = lockedApplication(candidatureId);
        String current = (String) application.get("statut_code");
        String action = required(request.action(), "Action RH manquante.").toUpperCase(Locale.ROOT);
        String next;
        String summary;
        switch (action) {
            case "PRESELECTIONNER" -> {
                requireState(current, Set.of("ENVOYEE_RH"));
                next = "PRESELECTIONNEE"; summary = "Votre candidature a été présélectionnée.";
            }
            case "REJETER" -> {
                if (Set.of("EMBAUCHEE", "REFUSEE", "CONTRAT_REFUSE").contains(current)) throw conflict("Cette candidature est déjà terminée.");
                next = "REFUSEE"; summary = truncate(clean(request.commentaire(), "Votre candidature n’a pas été retenue."), 480);
            }
            case "PLANIFIER_ENTRETIEN" -> {
                requireState(current, Set.of("PRESELECTIONNEE", "ENTRETIEN_REFUSE"));
                if (request.entretienAt() == null || !request.entretienAt().isAfter(LocalDateTime.now())) throw bad("Choisissez une date et une heure d’entretien à venir.");
                Integer type = lookupId("types_entretien", required(request.typeEntretien(), "Choisissez le type d’entretien."));
                Integer proposedId = lookupId("statuts_entretien", "PROPOSE");
                jdbc.update("INSERT INTO entretiens_candidature (candidature_id, type_id, statut_id, planifie_at, note, cree_par_id) VALUES (?, ?, ?, ?, ?, ?)",
                    candidatureId, type, proposedId, request.entretienAt(), clean(request.commentaire(), null), rhId);
                next = "ENTRETIEN_PROPOSE"; summary = "Un entretien vous est proposé le " + request.entretienAt().toString().replace('T', ' ') + ".";
            }
            case "MARQUER_ENTRETIEN_EFFECTUE" -> {
                requireState(current, Set.of("ENTRETIEN_ACCEPTE"));
                updateLatestInterview(candidatureId, "EFFECTUE", request.commentaire());
                next = "ENTRETIEN_EFFECTUE"; summary = "L’entretien a été marqué comme effectué.";
            }
            case "RETENIR_APRES_ENTRETIEN" -> {
                requireState(current, Set.of("ENTRETIEN_EFFECTUE"));
                next = "QUESTIONNAIRE_A_REMPLIR"; summary = "Vous êtes retenu après l’entretien. Complétez le questionnaire d’embauche.";
            }
            case "PROPOSER_CONTRAT" -> {
                requireState(current, Set.of("QUESTIONNAIRE_REMPLI"));
                applyContractProposal(candidatureId, application, request.salaireBase(), request.dateDebut(), request.typeContrat());
                next = "CONTRAT_PROPOSE"; summary = "Une proposition de contrat vous attend dans votre espace candidat.";
            }
            case "ACCEPTER_MODIFICATION" -> {
                requireState(current, Set.of("CANDIDAT_PROPOSE_MODIFICATION"));
                Map<String, Object> q = one("SELECT salaire_base_propose_candidat AS salaire, date_debut_proposee_candidat AS date_debut, type_contrat_propose_candidat_id AS type_id FROM questionnaires_embauche WHERE candidature_id = ?", candidatureId);
                validateSalary(application, (BigDecimal) q.get("salaire"));
                jdbc.update("UPDATE questionnaires_embauche SET salaire_base_final = ?, date_debut_finale = ?, type_contrat_final_id = ?, updated_at = CURRENT_TIMESTAMP WHERE candidature_id = ?",
                    q.get("salaire"), q.get("date_debut"), q.get("type_id"), candidatureId);
                next = "CONTRAT_PROPOSE"; summary = "Les modifications proposées sont acceptées. Votre contrat peut être accepté dans votre espace.";
            }
            case "CONTRE_PROPOSER" -> {
                requireState(current, Set.of("CANDIDAT_PROPOSE_MODIFICATION", "CONTRAT_PROPOSE"));
                applyContractProposal(candidatureId, application, request.salaireBase(), request.dateDebut(), request.typeContrat());
                next = "CONTRAT_PROPOSE"; summary = "Les conditions de votre proposition de contrat ont été mises à jour.";
            }
            default -> throw bad("Action RH inconnue.");
        }
        if ("RETENIR_APRES_ENTRETIEN".equals(action)) {
            setStatus(candidatureId, "RETENUE_ENTRETIEN", rhId, clean(request.commentaire(), "Candidat retenu après l’entretien."));
        }
        setStatus(candidatureId, next, rhId, clean(request.commentaire(), "Action RH : " + action));
        notifications.notifyUser((Integer) application.get("candidat_id"), "CANDIDATURE_STATUT", "Mise à jour de votre candidature", summary, "/espace/candidatures");
        return Map.of("id", candidatureId, "statut", next);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> candidateApplications(Integer candidateId) {
        return jdbc.queryForList("""
            SELECT c.id, o.id AS offre_id, o.titre AS offre, p.intitule AS poste, d.nom AS departement,
                   sc.code AS statut_code, sc.libelle AS statut, c.envoyee_at,
                   ec.planifie_at AS entretien_at, te.libelle AS type_entretien,
                   (sc.code = 'QUESTIONNAIRE_A_REMPLIR') AS questionnaire_a_remplir,
                   ct.numero AS numero_contrat, ct.salaire_base_mensuel, ct.date_debut AS contrat_date_debut,
                   tc.libelle AS type_contrat
            FROM candidatures c JOIN offres_emploi o ON o.id = c.offre_id
            JOIN demandes_recrutement dr ON dr.id = o.demande_id
            JOIN postes p ON p.id = dr.poste_id JOIN departements d ON d.id = dr.departement_id
            JOIN statuts_candidature sc ON sc.id = c.statut_id
            LEFT JOIN LATERAL (SELECT planifie_at, type_id FROM entretiens_candidature WHERE candidature_id = c.id ORDER BY id DESC LIMIT 1) ec ON TRUE
            LEFT JOIN types_entretien te ON te.id = ec.type_id
            LEFT JOIN contrats ct ON ct.candidature_id = c.id
            LEFT JOIN types_contrat tc ON tc.id = ct.type_contrat_id
            WHERE c.candidat_id = ? ORDER BY c.envoyee_at DESC, c.id DESC
            """, candidateId);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> employeeContract(Integer employeeId) {
        boolean employee = Boolean.TRUE.equals(jdbc.queryForObject("""
            SELECT EXISTS (SELECT 1 FROM users u JOIN statuts_validation_compte s ON s.id = u.administrative_validation_status_id
                JOIN user_roles ur ON ur.user_id = u.id JOIN roles r ON r.id = ur.role_id
                JOIN employes e ON e.user_id = u.id
                WHERE u.id = ? AND u.is_suspended = FALSE AND s.code = 'APPROVED' AND LOWER(r.role_name) = 'employe')
            """, Boolean.class, employeeId));
        if (!employee) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Un compte employé actif est requis.");
        return one("""
            SELECT ct.id AS contrat_id, ct.candidature_id, ct.numero, ct.salaire_base_mensuel,
                   ct.date_debut, ct.date_fin, ct.genere_at,
                   st.code AS statut_code, st.libelle AS statut_contrat, ct.document_path,
                   tc.libelle AS type_contrat, COALESCE(u.full_name, u.username) AS salarie,
                   u.email, e.date_naissance, e.matricule, p.intitule AS poste, d.nom AS departement,
                   o.titre AS offre, qe.salaire_base_souhaite, qe.date_debut_souhaitee,
                   tcs.libelle AS type_contrat_souhaite, qe.salaire_base_propose_candidat,
                   qe.date_debut_proposee_candidat, tcp.libelle AS type_contrat_propose_candidat,
                   qe.commentaire_candidat, qe.regles_acceptees
            FROM contrats ct JOIN candidatures c ON c.id = ct.candidature_id
            JOIN users u ON u.id = c.candidat_id JOIN employes e ON e.user_id = u.id
            JOIN types_contrat tc ON tc.id = ct.type_contrat_id JOIN offres_emploi o ON o.id = c.offre_id
            JOIN demandes_recrutement dr ON dr.id = o.demande_id JOIN postes p ON p.id = dr.poste_id
            JOIN departements d ON d.id = dr.departement_id JOIN statuts_contrat st ON st.id = ct.statut_id
            JOIN questionnaires_embauche qe ON qe.candidature_id = c.id
            JOIN types_contrat tcs ON tcs.id = qe.type_contrat_souhaite_id
            LEFT JOIN types_contrat tcp ON tcp.id = qe.type_contrat_propose_candidat_id
            WHERE u.id = ? ORDER BY ct.genere_at DESC LIMIT 1
            """, employeeId);
    }

    @Transactional
    public byte[] employeeContractDocument(Integer employeeId) {
        Map<String, Object> contract = employeeContract(employeeId);
        return contractPdf(employeeId, (Integer) contract.get("contrat_id"));
    }

    @Transactional
    public byte[] employeeContractDocumentForDossier(Integer actorId, Integer employeeId, Integer contractId) {
        boolean rh = Boolean.TRUE.equals(jdbc.queryForObject("""
            SELECT EXISTS (SELECT 1 FROM users u JOIN user_roles ur ON ur.user_id = u.id JOIN roles r ON r.id = ur.role_id
                JOIN statuts_validation_compte s ON s.id = u.administrative_validation_status_id
            WHERE u.id = ? AND u.is_suspended = FALSE AND s.code = 'APPROVED' AND LOWER(r.role_name) = 'rh')
            """, Boolean.class, actorId));
        if (!rh) {
            boolean owner = Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM users u JOIN employes e ON e.user_id = u.id
                    JOIN user_roles ur ON ur.user_id = u.id JOIN roles r ON r.id = ur.role_id
                    JOIN statuts_validation_compte s ON s.id = u.administrative_validation_status_id
                WHERE u.id = ? AND e.id = ? AND u.is_suspended = FALSE AND s.code = 'APPROVED'
                    AND LOWER(r.role_name) IN ('employe', 'manager'))
                """, Boolean.class, actorId, employeeId));
            if (!owner) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Le contrat est réservé au salarié concerné et aux RH.");
        }
        return contractPdf(employeeId, contractId);
    }

    private byte[] contractPdf(Integer employeeId, Integer contractId) {
        Map<String, Object> contract = one("""
            SELECT ct.id AS contrat_id, ct.candidature_id, ct.numero, ct.document_path, ct.salaire_base_mensuel,
                   ct.date_debut, ct.date_fin, ct.genere_at, tc.libelle AS type_contrat,
                   COALESCE(u.full_name, u.username) AS salarie, u.email, e.date_naissance, e.matricule,
                   p.intitule AS poste, d.nom AS departement, o.titre AS offre,
                   qe.salaire_base_souhaite, qe.date_debut_souhaitee, tcs.libelle AS type_contrat_souhaite,
                   qe.salaire_base_propose_candidat, qe.date_debut_proposee_candidat,
                   tcp.libelle AS type_contrat_propose_candidat, qe.commentaire_candidat,
                   CASE WHEN qe.regles_acceptees THEN 'Oui' ELSE 'Non' END AS regles_acceptees
            FROM contrats ct JOIN candidatures c ON c.id = ct.candidature_id
            JOIN employes e ON e.user_id = c.candidat_id JOIN users u ON u.id = c.candidat_id
            JOIN types_contrat tc ON tc.id = ct.type_contrat_id JOIN offres_emploi o ON o.id = c.offre_id
            JOIN demandes_recrutement dr ON dr.id = o.demande_id JOIN postes p ON p.id = dr.poste_id
            JOIN departements d ON d.id = dr.departement_id JOIN questionnaires_embauche qe ON qe.candidature_id = c.id
            JOIN types_contrat tcs ON tcs.id = qe.type_contrat_souhaite_id
            LEFT JOIN types_contrat tcp ON tcp.id = qe.type_contrat_propose_candidat_id
            WHERE e.id = ? AND ct.id = ?
            """, employeeId, contractId);
        String path = (String) contract.get("document_path");
        if (path == null || !path.matches("contrats/contrat_[A-Za-z0-9._-]+[.]pdf")) {
            contract.put("salaire_base", contract.get("salaire_base_mensuel"));
            contract.put("genere_le", contract.get("genere_at"));
            contract.put("periode_essai", "CDI avec période d’essai".equals(contract.get("type_contrat"))
                ? "Le contrat débute par une période d’essai dans les conditions prévues par les textes applicables. Sa durée et ses modalités sont communiquées par les ressources humaines."
                : "Aucune période d’essai n’est prévue dans les conditions acceptées pour ce contrat.");
            path = contractDocuments.generate(contract);
            jdbc.update("UPDATE contrats SET document_path = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?", path, contractId);
        }
        return contractDocuments.read(path);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> candidateDetail(Integer candidateId, Integer candidatureId) {
        requireCandidateOwner(candidateId, candidatureId);
        Map<String, Object> detail = one("""
            SELECT c.id, COALESCE(u.full_name, u.username) AS candidat, u.email,
                   o.titre AS offre, p.intitule AS poste, d.nom AS departement,
                   sc.code AS statut_code, sc.libelle AS statut, dr.budget_salaire_base, dr.date_souhaitee,
                   c.envoyee_at, ct.numero AS numero_contrat, ct.salaire_base_mensuel,
                   ct.date_debut AS contrat_date_debut, ct.genere_at, tc.libelle AS type_contrat,
                   qe.regles_acceptees, qe.date_naissance, qe.date_debut_souhaitee,
                   qe.salaire_base_souhaite, tcs.libelle AS type_contrat_souhaite,
                   qe.salaire_base_final, qe.date_debut_finale, tcf.libelle AS type_contrat_final,
                   qe.salaire_base_propose_candidat, qe.date_debut_proposee_candidat,
                   tcp.libelle AS type_contrat_propose_candidat, qe.commentaire_candidat,
                   ec.planifie_at AS entretien_at, te.libelle AS type_entretien, se.code AS entretien_statut
            FROM candidatures c JOIN users u ON u.id = c.candidat_id JOIN offres_emploi o ON o.id = c.offre_id
            JOIN demandes_recrutement dr ON dr.id = o.demande_id JOIN postes p ON p.id = dr.poste_id
            JOIN departements d ON d.id = dr.departement_id JOIN statuts_candidature sc ON sc.id = c.statut_id
            LEFT JOIN questionnaires_embauche qe ON qe.candidature_id = c.id
            LEFT JOIN types_contrat tcs ON tcs.id = qe.type_contrat_souhaite_id
            LEFT JOIN types_contrat tcf ON tcf.id = qe.type_contrat_final_id
            LEFT JOIN types_contrat tcp ON tcp.id = qe.type_contrat_propose_candidat_id
            LEFT JOIN contrats ct ON ct.candidature_id = c.id LEFT JOIN types_contrat tc ON tc.id = ct.type_contrat_id
            LEFT JOIN LATERAL (SELECT * FROM entretiens_candidature WHERE candidature_id = c.id ORDER BY id DESC LIMIT 1) ec ON TRUE
            LEFT JOIN types_entretien te ON te.id = ec.type_id LEFT JOIN statuts_entretien se ON se.id = ec.statut_id
            WHERE c.id = ?
            """, candidatureId);
        detail.put("parcours", jdbc.queryForList("""
            SELECT tp.libelle AS type, pa.intitule, pa.organisme, pa.description, ne.libelle AS niveau,
                   du.date_debut, du.date_fin, du.en_cours FROM parcours pa
            JOIN types_parcours tp ON tp.id = pa.type_parcours_id LEFT JOIN niveaux_etude ne ON ne.id = pa.niveau_etude_id
            JOIN durees du ON du.parcours_id = pa.id WHERE pa.candidature_id = ? ORDER BY pa.ordre
            """, candidatureId));
        return detail;
    }

    @Transactional
    public Map<String, Object> respondAsCandidate(Integer candidateId, Integer candidatureId, CandidatureCandidateResponseRequest request) {
        requireCandidateOwner(candidateId, candidatureId);
        Map<String, Object> app = lockedApplication(candidatureId);
        String current = (String) app.get("statut_code");
        String action = required(request.action(), "Réponse manquante.").toUpperCase(Locale.ROOT);
        String next;
        switch (action) {
            case "ACCEPTER_ENTRETIEN", "REFUSER_ENTRETIEN" -> {
                requireState(current, Set.of("ENTRETIEN_PROPOSE"));
                String interviewStatus = action.equals("ACCEPTER_ENTRETIEN") ? "ACCEPTE" : "REFUSE";
                updateLatestInterview(candidatureId, interviewStatus, request.commentaire());
                next = action.equals("ACCEPTER_ENTRETIEN") ? "ENTRETIEN_ACCEPTE" : "ENTRETIEN_REFUSE";
            }
            case "SOUMETTRE_QUESTIONNAIRE" -> {
                requireState(current, Set.of("QUESTIONNAIRE_A_REMPLIR"));
                if (!Boolean.TRUE.equals(request.reglesAcceptees())) throw bad("Vous devez confirmer avoir pris connaissance des règles de l’entreprise.");
                if (request.dateNaissance() == null || !request.dateNaissance().isBefore(LocalDate.now())) throw bad("Indiquez une date de naissance valide.");
                if (request.dateDebut() == null) throw bad("Indiquez votre date de début souhaitée.");
                if (request.salaireBase() == null || request.salaireBase().signum() <= 0) throw bad("Indiquez une prétention salariale valide.");
                Integer contractTypeId = lookupId("types_contrat", required(request.typeContrat(), "Choisissez un type de contrat."));
                jdbc.update("""
                    INSERT INTO questionnaires_embauche (candidature_id, regles_acceptees, date_naissance, date_debut_souhaitee,
                        salaire_base_souhaite, type_contrat_souhaite_id, repondu_at)
                    VALUES (?, TRUE, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                    """, candidatureId, request.dateNaissance(), request.dateDebut(), request.salaireBase(), contractTypeId);
                next = "QUESTIONNAIRE_REMPLI";
            }
            case "ACCEPTER_CONTRAT" -> {
                requireState(current, Set.of("CONTRAT_PROPOSE"));
                hire(candidateId, candidatureId, app);
                next = "EMBAUCHEE";
            }
            case "PROPOSER_MODIFICATION" -> {
                requireState(current, Set.of("CONTRAT_PROPOSE"));
                if (request.salaireBase() == null || request.salaireBase().signum() <= 0 || request.dateDebut() == null) throw bad("Indiquez un salaire et une date de début pour votre proposition.");
                Integer typeId = lookupId("types_contrat", required(request.typeContrat(), "Choisissez le type de contrat souhaité."));
                jdbc.update("""
                    UPDATE questionnaires_embauche SET salaire_base_propose_candidat = ?, date_debut_proposee_candidat = ?,
                        type_contrat_propose_candidat_id = ?, commentaire_candidat = ?, updated_at = CURRENT_TIMESTAMP
                    WHERE candidature_id = ?
                    """, request.salaireBase(), request.dateDebut(), typeId, clean(request.commentaire(), null), candidatureId);
                next = "CANDIDAT_PROPOSE_MODIFICATION";
            }
            case "REFUSER_CONTRAT" -> {
                requireState(current, Set.of("CONTRAT_PROPOSE"));
                next = "CONTRAT_REFUSE";
            }
            default -> throw bad("Réponse candidat inconnue.");
        }
        setStatus(candidatureId, next, candidateId, "Réponse candidat : " + action);
        notifications.notifyRole("rh", "CANDIDATURE_A_TRAITER", "Réponse reçue d’un candidat",
            "Un candidat a répondu à une étape de recrutement.", "/espace/rh/recrutement/candidatures");
        return Map.of("id", candidatureId, "statut", next);
    }

    private void applyContractProposal(Integer candidatureId, Map<String, Object> application, BigDecimal salary,
            LocalDate startDate, String contractType) {
        if (salary == null || salary.signum() <= 0 || startDate == null) throw bad("Renseignez le salaire, la date de début et le type de contrat.");
        validateSalary(application, salary);
        Integer typeId = lookupId("types_contrat", required(contractType, "Choisissez le type de contrat."));
        jdbc.update("UPDATE questionnaires_embauche SET salaire_base_final = ?, date_debut_finale = ?, type_contrat_final_id = ?, updated_at = CURRENT_TIMESTAMP WHERE candidature_id = ?",
            salary, startDate, typeId, candidatureId);
    }

    private void validateSalary(Map<String, Object> application, BigDecimal salary) {
        BigDecimal budget = (BigDecimal) application.get("budget_salaire_base");
        BigDecimal minimum = (BigDecimal) application.get("salaire_base_min");
        BigDecimal maximum = (BigDecimal) application.get("salaire_base_max");
        if (salary == null || salary.signum() <= 0 || budget == null || minimum == null || maximum == null
                || salary.compareTo(minimum) < 0 || salary.compareTo(budget) > 0 || salary.compareTo(maximum) > 0) {
            throw bad("Le salaire proposé doit respecter la fourchette du poste et le budget accepté par personne.");
        }
    }

    private void hire(Integer candidateId, Integer candidatureId, Map<String, Object> application) {
        Map<String, Object> questionnaire = one("""
            SELECT qe.date_naissance, qe.date_debut_finale, qe.salaire_base_final, qe.type_contrat_final_id,
                   dr.poste_id, dr.effectif, dr.effectif_recrute, o.id AS offre_id
            FROM questionnaires_embauche qe JOIN candidatures c ON c.id = qe.candidature_id
            JOIN offres_emploi o ON o.id = c.offre_id JOIN demandes_recrutement dr ON dr.id = o.demande_id
            WHERE qe.candidature_id = ? FOR UPDATE OF qe
            """, candidatureId);
        jdbc.queryForObject("SELECT id FROM offres_emploi WHERE id = ? FOR UPDATE", Integer.class, questionnaire.get("offre_id"));
        if (questionnaire.get("date_debut_finale") == null || questionnaire.get("salaire_base_final") == null || questionnaire.get("type_contrat_final_id") == null) {
            throw conflict("La proposition de contrat n’est pas complète.");
        }
        if ((Integer) questionnaire.get("effectif_recrute") >= (Integer) questionnaire.get("effectif")) {
            throw conflict("Tous les postes de cette offre sont déjà pourvus.");
        }
        Integer authorized = jdbc.queryForObject("SELECT effectif_autorise FROM postes WHERE id = ? FOR UPDATE",
            Integer.class, questionnaire.get("poste_id"));
        Integer occupiedOrScheduled = jdbc.queryForObject("""
            SELECT COUNT(*) FROM employes e
            LEFT JOIN situation_administrative_employe sa ON sa.employe_id = e.id
            LEFT JOIN statuts_administratifs_employe statut ON statut.id = sa.statut_id
            WHERE e.poste_id = ? AND COALESCE(statut.code, 'ACTIF') NOT IN ('SORTI', 'EN_PREPARATION')
            """, Integer.class, questionnaire.get("poste_id"));
        if (occupiedOrScheduled != null && occupiedOrScheduled >= authorized) {
            throw conflict("La cible autorisÃ©e de ce poste est dÃ©jÃ  atteinte. Les RH doivent vÃ©rifier l’effectif avant toute embauche supplÃ©mentaire.");
        }
        int reservedForDemand = jdbc.update("""
            UPDATE demandes_recrutement SET effectif_recrute = effectif_recrute + 1, updated_at = CURRENT_TIMESTAMP
            WHERE id = (SELECT demande_id FROM offres_emploi WHERE id = ?)
              AND effectif_recrute < effectif
            """, questionnaire.get("offre_id"));
        if (reservedForDemand != 1) throw conflict("Toutes les places autorisÃ©es par cette demande sont dÃ©jÃ  pourvues.");
        Integer recruited = jdbc.queryForObject("""
            SELECT dr.effectif_recrute FROM demandes_recrutement dr
            JOIN offres_emploi o ON o.demande_id = dr.id WHERE o.id = ?
            """, Integer.class, questionnaire.get("offre_id"));
        if (recruited != null && recruited >= (Integer) questionnaire.get("effectif")) {
            jdbc.update("""
                UPDATE offres_emploi
                SET statut_id = (SELECT id FROM statuts_offre_emploi WHERE code = 'CLOTUREE'),
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND statut_id = (SELECT id FROM statuts_offre_emploi WHERE code = 'PUBLIEE')
                """, questionnaire.get("offre_id"));
        }
        jdbc.update("""
            INSERT INTO employes (user_id, poste_id, date_embauche, date_naissance)
            VALUES (?, ?, ?, ?)
            """, candidateId, questionnaire.get("poste_id"), questionnaire.get("date_debut_finale"), questionnaire.get("date_naissance"));
        jdbc.update("DELETE FROM user_roles WHERE user_id = ? AND role_id = (SELECT id FROM roles WHERE LOWER(role_name) = 'simple_user')", candidateId);
        jdbc.update("INSERT INTO user_roles (user_id, role_id) SELECT ?, id FROM roles WHERE LOWER(role_name) = 'employe' ON CONFLICT DO NOTHING", candidateId);
        String contractNumber = "CTR-" + LocalDate.now().getYear() + "-" + String.format(Locale.ROOT, "%06d", candidatureId);
        Map<String, Object> document = one("""
            SELECT ? AS numero, COALESCE(u.full_name, u.username) AS salarie, u.email, e.date_naissance,
                   e.matricule, p.intitule AS poste, d.nom AS departement, o.titre AS offre,
                   tc.libelle AS type_contrat, qe.date_debut_finale AS date_debut,
                   qe.salaire_base_final AS salaire_base, qe.salaire_base_souhaite,
                   qe.date_debut_souhaitee, tcs.libelle AS type_contrat_souhaite,
                   qe.salaire_base_propose_candidat, qe.date_debut_proposee_candidat,
                   tcp.libelle AS type_contrat_propose_candidat, qe.commentaire_candidat,
                   CASE WHEN qe.regles_acceptees THEN 'Oui' ELSE 'Non' END AS regles_acceptees
            FROM candidatures c JOIN users u ON u.id = c.candidat_id JOIN employes e ON e.user_id = u.id
            JOIN offres_emploi o ON o.id = c.offre_id JOIN demandes_recrutement dr ON dr.id = o.demande_id
            JOIN postes p ON p.id = dr.poste_id JOIN departements d ON d.id = dr.departement_id
            JOIN questionnaires_embauche qe ON qe.candidature_id = c.id
            JOIN types_contrat tc ON tc.id = qe.type_contrat_final_id
            JOIN types_contrat tcs ON tcs.id = qe.type_contrat_souhaite_id
            LEFT JOIN types_contrat tcp ON tcp.id = qe.type_contrat_propose_candidat_id WHERE c.id = ?
            """, contractNumber, candidatureId);
        document.put("genere_le", LocalDate.now());
        document.put("periode_essai", "CDI avec période d’essai".equals(document.get("type_contrat"))
            ? "Le contrat débute par une période d’essai dans les conditions prévues par les textes applicables. Sa durée et ses modalités sont communiquées par les ressources humaines."
            : "Aucune période d’essai n’est prévue dans les conditions acceptées pour ce contrat.");
        String documentPath = contractDocuments.generate(document);
        Integer activeStatusId = lookupId("statuts_contrat", "ACTIF");
        jdbc.update("""
            INSERT INTO contrats (candidature_id, numero, type_contrat_id, statut_id, salaire_base_mensuel,
                date_debut, document_path)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """, candidatureId, contractNumber, questionnaire.get("type_contrat_final_id"), activeStatusId,
                questionnaire.get("salaire_base_final"), questionnaire.get("date_debut_finale"), documentPath);
    }

    private void updateLatestInterview(Integer candidatureId, String statusCode, String note) {
        int changed = jdbc.update("""
            UPDATE entretiens_candidature SET statut_id = (SELECT id FROM statuts_entretien WHERE code = ?),
                note = COALESCE(?, note), updated_at = CURRENT_TIMESTAMP
            WHERE id = (SELECT id FROM entretiens_candidature WHERE candidature_id = ? ORDER BY id DESC LIMIT 1)
            """, statusCode, clean(note, null), candidatureId);
        if (changed != 1) throw conflict("Aucun entretien n’est associé à cette candidature.");
    }

    private void setStatus(Integer candidatureId, String statusCode, Integer actorId, String comment) {
        Integer statusId = lookupId("statuts_candidature", statusCode);
        jdbc.update("UPDATE candidatures SET statut_id = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?", statusId, candidatureId);
        jdbc.update("INSERT INTO historique_candidatures (candidature_id, statut_id, acteur_id, commentaire) VALUES (?, ?, ?, ?)", candidatureId, statusId, actorId, comment);
    }

    private Map<String, Object> lockedApplication(Integer candidatureId) {
        return one("""
            SELECT c.id, c.candidat_id, c.offre_id, sc.code AS statut_code, dr.budget_salaire_base,
                   p.salaire_base_min, p.salaire_base_max
            FROM candidatures c JOIN statuts_candidature sc ON sc.id = c.statut_id
            JOIN offres_emploi o ON o.id = c.offre_id JOIN demandes_recrutement dr ON dr.id = o.demande_id
            JOIN postes p ON p.id = dr.poste_id
            WHERE c.id = ? FOR UPDATE OF c
            """, candidatureId);
    }

    private void requireCandidateOwner(Integer userId, Integer candidatureId) {
        boolean accountActive = Boolean.TRUE.equals(jdbc.queryForObject("""
            SELECT EXISTS (SELECT 1 FROM users u JOIN statuts_validation_compte s ON s.id = u.administrative_validation_status_id
                WHERE u.id = ? AND u.is_suspended = FALSE AND s.code = 'APPROVED')
            """, Boolean.class, userId));
        if (!accountActive) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Votre compte doit être actif et validé pour répondre à cette candidature.");
        if (!Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM candidatures WHERE id = ? AND candidat_id = ?)", Boolean.class, candidatureId, userId))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Candidature introuvable.");
        }
    }

    private void requireRh(Integer userId) {
        boolean allowed = Boolean.TRUE.equals(jdbc.queryForObject("""
            SELECT EXISTS (SELECT 1 FROM users u JOIN statuts_validation_compte s ON s.id = u.administrative_validation_status_id
                JOIN user_roles ur ON ur.user_id = u.id JOIN roles r ON r.id = ur.role_id
                WHERE u.id = ? AND u.is_suspended = FALSE AND s.code = 'APPROVED' AND LOWER(r.role_name) = 'rh')
            """, Boolean.class, userId));
        if (!allowed) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Un compte RH validé est requis.");
    }

    private Integer lookupId(String table, String code) {
        if (!Set.of("statuts_candidature", "statuts_entretien", "types_entretien", "types_contrat", "statuts_contrat").contains(table)) throw new IllegalArgumentException("Reference table not allowed");
        return jdbc.query("SELECT id FROM " + table + " WHERE UPPER(code) = UPPER(?)", (row, index) -> row.getInt("id"), code)
            .stream().findFirst().orElseThrow(() -> bad("Valeur de référence invalide."));
    }

    private Map<String, Object> one(String sql, Object... args) {
        return jdbc.query(sql, args, row -> { if (!row.next()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Candidature introuvable.");
            var map = new java.util.LinkedHashMap<String, Object>();
            var metadata = row.getMetaData();
            for (int i = 1; i <= metadata.getColumnCount(); i++) map.put(metadata.getColumnLabel(i), row.getObject(i));
            return map; });
    }

    private void requireState(String current, Set<String> allowed) { if (!allowed.contains(current)) throw conflict("Cette action n’est pas disponible à l’étape « " + current + " »."); }
    private String required(String value, String message) { if (value == null || value.isBlank()) throw bad(message); return value.trim(); }
    private String clean(String value, String fallback) { return value == null || value.isBlank() ? fallback : value.trim(); }
    private String truncate(String value, int max) { return value.length() <= max ? value : value.substring(0, max - 1) + "…"; }
    private ResponseStatusException bad(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
    private ResponseStatusException conflict(String message) { return new ResponseStatusException(HttpStatus.CONFLICT, message); }
}
