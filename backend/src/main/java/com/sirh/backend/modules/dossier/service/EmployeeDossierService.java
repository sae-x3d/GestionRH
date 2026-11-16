package com.sirh.backend.modules.dossier.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import com.sirh.backend.modules.dossier.dto.CorrectionRequest;
import com.sirh.backend.modules.candidatures.service.CandidateFileStorageService;
import com.sirh.backend.modules.notifications.service.NotificationCenterService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EmployeeDossierService {
    private static final String DIRECTORY_SELECT = """
        SELECT e.id, e.matricule, u.full_name AS nom, u.email, u.phone_number, u.region,
               e.date_embauche, d.id AS departement_id, d.nom AS departement, p.id AS poste_id, p.intitule AS poste,
               provenance.code AS provenance_code, provenance.libelle AS provenance,
               COALESCE(sa.code, 'ACTIF') AS statut_code, COALESCE(sa.libelle, 'Actif') AS statut,
               (SELECT string_agg(DISTINCT COALESCE(manager_user.full_name, manager_user.username), ', ' ORDER BY COALESCE(manager_user.full_name, manager_user.username))
                FROM employes manager_employee JOIN postes manager_post ON manager_post.id = manager_employee.poste_id
                JOIN users manager_user ON manager_user.id = manager_employee.user_id
                JOIN user_roles manager_role ON manager_role.user_id = manager_user.id
                JOIN roles manager_role_type ON manager_role_type.id = manager_role.role_id
                JOIN statuts_validation_compte manager_status ON manager_status.id = manager_user.administrative_validation_status_id
                WHERE manager_post.departement_id = d.id AND manager_role_type.role_name = 'manager'
                  AND manager_status.code = 'APPROVED' AND manager_user.is_suspended = FALSE) AS responsables_departement,
               CASE WHEN u.photo_profil_path IS NULL THEN NULL ELSE '/api/dossiers/employes/' || e.id || '/photo' END AS photo_url
        FROM employes e JOIN users u ON u.id = e.user_id
        JOIN postes p ON p.id = e.poste_id JOIN departements d ON d.id = p.departement_id
        JOIN provenances_employe provenance ON provenance.id = e.provenance_id
        LEFT JOIN situation_administrative_employe situation ON situation.employe_id = e.id
        LEFT JOIN statuts_administratifs_employe sa ON sa.id = situation.statut_id
        """;

    private final JdbcTemplate jdbc;
    private final EmployeeDocumentStorageService storage;
    private final CandidateFileStorageService profilePhotos;
    private final NotificationCenterService notifications;
    private static final Map<String, String> DIRECTORY_SORTS = Map.of(
        "nom", "u.full_name", "matricule", "e.matricule", "poste", "p.intitule",
        "departement", "d.nom", "statut", "sa.code", "dateEmbauche", "e.date_embauche");

    public EmployeeDossierService(JdbcTemplate jdbc, EmployeeDocumentStorageService storage,
            CandidateFileStorageService profilePhotos, NotificationCenterService notifications) {
        this.jdbc = jdbc; this.storage = storage; this.profilePhotos = profilePhotos; this.notifications = notifications;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> directory(Integer actorId, boolean manager, String query, Integer departmentId,
            Integer postId, String status, int requestedPage, int requestedSize, String sortBy, String sortDirection) {
        requireActiveRole(actorId, manager ? "manager" : "rh");
        int page = Math.min(100000, Math.max(0, requestedPage)); int size = Math.min(100, Math.max(1, requestedSize));
        StringBuilder where = new StringBuilder(" WHERE TRUE "); List<Object> args = new ArrayList<>();
        if (manager) { where.append(" AND d.id = ? "); args.add(managerDepartment(actorId)); }
        if (query != null && !query.isBlank()) {
            where.append(" AND (LOWER(u.full_name) LIKE ? OR LOWER(u.username) LIKE ? OR LOWER(e.matricule) LIKE ?) ");
            String term = "%" + query.trim().toLowerCase(java.util.Locale.ROOT) + "%"; args.add(term); args.add(term); args.add(term);
        }
        if (departmentId != null && !manager) { where.append(" AND d.id = ? "); args.add(departmentId); }
        if (postId != null) { where.append(" AND p.id = ? "); args.add(postId); }
        if (status != null && !status.isBlank()) { where.append(" AND COALESCE(sa.code, 'ACTIF') = ? "); args.add(status.trim().toUpperCase(java.util.Locale.ROOT)); }
        Integer total = jdbc.queryForObject("SELECT COUNT(*) FROM (" + DIRECTORY_SELECT + where + ") employees", Integer.class, args.toArray());
        args.add(size); args.add(page * size);
        String sortColumn = DIRECTORY_SORTS.getOrDefault(sortBy, "nom".equals(sortBy) ? "u.full_name" : "u.full_name");
        String direction = "desc".equalsIgnoreCase(sortDirection) ? "DESC" : "ASC";
        List<Map<String, Object>> content = jdbc.queryForList(DIRECTORY_SELECT + where + " ORDER BY " + sortColumn + " " + direction + " NULLS LAST, e.matricule ASC LIMIT ? OFFSET ?", args.toArray());
        if (manager) for (Map<String, Object> row : content) {
            row.remove("email"); row.remove("phone_number"); row.remove("region"); row.remove("photo_url");
        }
        Map<String, Object> result = new LinkedHashMap<>(); result.put("content", content); result.put("page", page);
        result.put("size", size); result.put("totalElements", total == null ? 0 : total);
        result.put("totalPages", total == null || total == 0 ? 0 : (int) Math.ceil(total / (double) size));
        if (!manager) {
            result.put("departements", jdbc.queryForList("SELECT id, nom FROM departements WHERE actif=TRUE ORDER BY nom"));
            result.put("postes", jdbc.queryForList("SELECT id, intitule, departement_id AS departementId FROM postes WHERE actif=TRUE ORDER BY intitule"));
            result.put("statuts", jdbc.queryForList("SELECT code, libelle FROM statuts_administratifs_employe ORDER BY id"));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> ownDossier(Integer actorId) {
        Integer employeeId = employeeIdForUser(actorId);
        return dossier(actorId, employeeId);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> dossier(Integer actorId, Integer employeeId) {
        authorizeEmployee(actorId, employeeId);
        Map<String, Object> result = one(DIRECTORY_SELECT + " WHERE e.id = ?", employeeId);
        result.put("anciennete", seniority(result.get("date_embauche")));
        if (isRh(actorId)) result.put("statutsDisponibles", jdbc.queryForList("SELECT code, libelle FROM statuts_administratifs_employe ORDER BY id"));
        result.put("historiqueStatuts", jdbc.queryForList("""
            SELECT s.code AS statutCode, s.libelle AS statut, h.date_effet AS dateEffet, h.motif,
                   COALESCE(u.full_name, u.username) AS acteur
            FROM historique_statut_employe h JOIN statuts_administratifs_employe s ON s.id = h.statut_id
            LEFT JOIN users u ON u.id = h.acteur_id WHERE h.employe_id = ? ORDER BY h.date_effet DESC, h.id DESC
            """, employeeId));
        result.put("documentsManquants", jdbc.queryForList("""
            SELECT t.code, t.libelle FROM types_documents_employe t
            WHERE t.actif = TRUE AND t.obligatoire = TRUE AND NOT EXISTS (
                SELECT 1 FROM documents_employe doc WHERE doc.employe_id = ? AND doc.type_document_id = t.id AND doc.archive_at IS NULL
            ) ORDER BY t.libelle
            """, employeeId));
        result.put("documentsExpirants", jdbc.queryForList("""
            SELECT doc.id, t.libelle, latest.date_expiration AS dateExpiration
            FROM documents_employe doc JOIN types_documents_employe t ON t.id = doc.type_document_id
            JOIN LATERAL (SELECT v.date_expiration FROM versions_documents_employe v WHERE v.document_id = doc.id ORDER BY v.numero_version DESC LIMIT 1) latest ON TRUE
            WHERE doc.employe_id = ? AND doc.archive_at IS NULL AND latest.date_expiration <= CURRENT_DATE + INTERVAL '30 days'
            ORDER BY latest.date_expiration
            """, employeeId));
        result.put("contratActif", jdbc.query("""
            SELECT ct.numero, tc.libelle AS type, sc.libelle AS statut, ct.date_debut AS dateDebut,
                   ct.date_fin AS dateFin, ct.salaire_base_mensuel AS salaireBase
            FROM employes e JOIN candidatures c ON c.candidat_id = e.user_id JOIN contrats ct ON ct.candidature_id = c.id
            JOIN types_contrat tc ON tc.id = ct.type_contrat_id JOIN statuts_contrat sc ON sc.id = ct.statut_id
            WHERE e.id = ? AND sc.code = 'ACTIF' ORDER BY ct.date_debut DESC LIMIT 1
            """, rs -> rs.next() ? Map.of("numero", rs.getString("numero"), "type", rs.getString("type"), "statut", rs.getString("statut"),
                "dateDebut", rs.getObject("dateDebut", LocalDate.class), "dateFin", rs.getObject("dateFin", LocalDate.class) == null ? "" : rs.getObject("dateFin", LocalDate.class).toString(),
                "salaireBase", rs.getBigDecimal("salaireBase")) : null, employeeId));
        if (canSeePersonal(actorId, employeeId)) {
            result.put("contratsHistorique", jdbc.queryForList("""
                SELECT ct.id, ct.numero, tc.libelle AS type, sc.libelle AS statut, ct.date_debut AS dateDebut,
                       ct.date_fin AS dateFin, ct.salaire_base_mensuel AS salaireBase, ct.genere_at AS genereAt
                FROM employes e JOIN candidatures c ON c.candidat_id = e.user_id
                JOIN contrats ct ON ct.candidature_id = c.id
                JOIN types_contrat tc ON tc.id = ct.type_contrat_id
                JOIN statuts_contrat sc ON sc.id = ct.statut_id
                WHERE e.id = ? ORDER BY ct.date_debut DESC, ct.id DESC
                """, employeeId));
        }
        result.put("historiqueAffectations", jdbc.queryForList("""
            SELECT h.date_effet AS dateEffet, h.motif,
                   provenance.libelle AS provenance,
                   ancien.intitule AS ancienPoste, ancien_departement.nom AS ancienDepartement,
                   nouveau.intitule AS nouveauPoste, nouveau_departement.nom AS nouveauDepartement,
                   COALESCE(acteur.full_name, acteur.username) AS acteur
            FROM historique_affectation_employe h
            JOIN provenances_employe provenance ON provenance.id = h.provenance_id
            LEFT JOIN postes ancien ON ancien.id = h.ancien_poste_id
            LEFT JOIN departements ancien_departement ON ancien_departement.id = ancien.departement_id
            JOIN postes nouveau ON nouveau.id = h.nouveau_poste_id
            JOIN departements nouveau_departement ON nouveau_departement.id = nouveau.departement_id
            LEFT JOIN users acteur ON acteur.id = h.acteur_id
            WHERE h.employe_id = ? ORDER BY h.date_effet DESC, h.id DESC
            """, employeeId));
        if (canSeePersonal(actorId, employeeId)) {
            result.put("informationsPersonnelles", personalInfo(employeeId));
            result.put("photoUrl", photoUrl(actorId, employeeId));
            result.put("cvCandidature", recruitmentCv(employeeId));
        } else {
            result.remove("email"); result.remove("phone_number"); result.remove("region"); result.remove("photo_url");
            result.remove("documentsManquants"); result.remove("documentsExpirants"); result.remove("contratActif"); result.remove("contratsHistorique");
            result.remove("cvCandidature");
        }
        result.put("documents", listDocuments(actorId, employeeId));
        result.put("evenements", jdbc.queryForList("""
            SELECT t.libelle AS type, event.date_evenement AS dateEvenement, event.date_effet AS dateEffet,
                   event.motif, event.resume, COALESCE(u.full_name, u.username) AS acteur
            FROM evenements_dossier_employe event JOIN types_evenements_dossier t ON t.id = event.type_evenement_id
            LEFT JOIN users u ON u.id = event.acteur_id
            WHERE event.employe_id = ? AND (? = TRUE OR (
                COALESCE(event.objet_type, '') <> 'DEMANDE_CORRECTION'
                AND (COALESCE(event.objet_type, '') <> 'DOCUMENT' OR EXISTS (
                    SELECT 1 FROM documents_employe doc JOIN types_documents_employe type ON type.id = doc.type_document_id
                    WHERE doc.id = event.objet_id AND type.visible_manager = TRUE
                ))
            ))
            ORDER BY event.date_evenement DESC LIMIT 50
            """, employeeId, canSeePersonal(actorId, employeeId)));
        return result;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> documentTypes(Integer actorId, Integer employeeId) {
        authorizeEmployee(actorId, employeeId);
        String access = canSeePersonal(actorId, employeeId) ? "" : " AND visible_manager = TRUE";
        return jdbc.queryForList("SELECT id, code, libelle, obligatoire FROM types_documents_employe WHERE actif = TRUE" + access + " ORDER BY libelle");
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listDocuments(Integer actorId, Integer employeeId) {
        authorizeEmployee(actorId, employeeId);
        String visibility = canSeePersonal(actorId, employeeId) ? "" : " AND type.visible_manager = TRUE";
        return jdbc.queryForList("""
            SELECT doc.id, type.code AS typeCode, type.libelle AS type, type.obligatoire,
                   latest.id AS versionId,
                   latest.numero_version AS version, latest.nom_fichier AS nomFichier, latest.type_mime AS typeMime,
                   latest.taille_octets AS tailleOctets, latest.date_emission AS dateEmission,
                   latest.date_expiration AS dateExpiration, latest.created_at AS ajouteAt,
                   (latest.date_expiration IS NOT NULL AND latest.date_expiration < CURRENT_DATE) AS expire
            FROM documents_employe doc JOIN types_documents_employe type ON type.id = doc.type_document_id
            JOIN LATERAL (SELECT * FROM versions_documents_employe v WHERE v.document_id = doc.id ORDER BY v.numero_version DESC LIMIT 1) latest ON TRUE
            WHERE doc.employe_id = ? AND doc.archive_at IS NULL
            """ + visibility + " ORDER BY type.libelle, doc.id DESC", employeeId);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> documentVersions(Integer actorId, Integer employeeId, Integer documentId) {
        authorizeEmployee(actorId, employeeId);
        String visibility = canSeePersonal(actorId, employeeId) ? "" : " AND type.visible_manager = TRUE";
        if (!Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM documents_employe WHERE id = ? AND employe_id = ? AND archive_at IS NULL)", Boolean.class, documentId, employeeId))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document introuvable.");
        }
        return jdbc.queryForList("""
            SELECT version.id AS versionId, version.numero_version AS numeroVersion, version.nom_fichier AS nomFichier,
                   version.type_mime AS typeMime, version.taille_octets AS tailleOctets,
                   version.date_emission AS dateEmission, version.date_expiration AS dateExpiration, version.created_at AS ajouteAt
            FROM versions_documents_employe version
            JOIN documents_employe doc ON doc.id = version.document_id
            JOIN types_documents_employe type ON type.id = doc.type_document_id
            WHERE doc.id = ? AND doc.employe_id = ? AND doc.archive_at IS NULL
            """ + visibility + " ORDER BY version.numero_version DESC", documentId, employeeId);
    }

    @Transactional
    public Map<String, Object> uploadDocument(Integer actorId, Integer employeeId, Integer typeId, Integer documentId,
            LocalDate issueDate, LocalDate expiryDate, MultipartFile file) {
        authorizeDocumentWrite(actorId, employeeId);
        if (issueDate != null && issueDate.isAfter(LocalDate.now())) throw bad("La date d’émission ne peut pas être dans le futur.");
        if (expiryDate != null && issueDate != null && expiryDate.isBefore(issueDate)) throw bad("La date d’expiration doit être postérieure à la date d’émission.");
        Integer resolvedType = typeId;
        if (documentId != null) {
            Map<String, Object> existing = jdbc.queryForList("SELECT id, type_document_id FROM documents_employe WHERE id = ? AND employe_id = ? AND archive_at IS NULL FOR UPDATE", documentId, employeeId)
                .stream().findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document introuvable."));
            resolvedType = ((Number) existing.get("type_document_id")).intValue();
        }
        if (resolvedType == null || !Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM types_documents_employe WHERE id = ? AND actif = TRUE)", Boolean.class, resolvedType))) throw bad("La catégorie du document est invalide.");
        String ownerUsername = jdbc.queryForObject("SELECT u.username FROM employes e JOIN users u ON u.id = e.user_id WHERE e.id = ?", String.class, employeeId);
        EmployeeDocumentStorageService.StoredDocument stored = storage.store(employeeId, file);
        Integer id = documentId;
        if (id == null) {
            id = jdbc.queryForObject("INSERT INTO documents_employe (employe_id, type_document_id, ajoute_par_id) VALUES (?, ?, ?) RETURNING id", Integer.class, employeeId, resolvedType, actorId);
        }
        Integer version = jdbc.queryForObject("SELECT COALESCE(MAX(numero_version), 0) + 1 FROM versions_documents_employe WHERE document_id = ?", Integer.class, id);
        jdbc.update("""
            INSERT INTO versions_documents_employe (document_id, numero_version, cle_stockage, nom_fichier, type_mime, taille_octets, date_emission, date_expiration, ajoute_par_id)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """, id, version, stored.key(), stored.fileName(), stored.mimeType(), stored.size(), issueDate, expiryDate, actorId);
        jdbc.update("UPDATE documents_employe SET updated_at = CURRENT_TIMESTAMP WHERE id = ?", id);
        event(employeeId, "DOCUMENT_AJOUTE", actorId, "DOCUMENT", id, null, "Document administratif ajouté ou versionné");
        Integer savedId = id;
        return listDocuments(actorId, employeeId).stream().filter(doc -> ((Number) doc.get("id")).intValue() == savedId).findFirst().orElse(Map.of());
    }

    @Transactional(readOnly = true)
    public Download downloadDocument(Integer actorId, Integer employeeId, Integer documentId) {
        authorizeEmployee(actorId, employeeId);
        Map<String, Object> document = jdbc.queryForList("""
            SELECT v.cle_stockage, v.nom_fichier, v.type_mime, t.visible_manager
            FROM documents_employe d JOIN types_documents_employe t ON t.id = d.type_document_id
            JOIN LATERAL (SELECT * FROM versions_documents_employe v WHERE v.document_id = d.id ORDER BY v.numero_version DESC LIMIT 1) v ON TRUE
            WHERE d.id = ? AND d.employe_id = ? AND d.archive_at IS NULL
            """, documentId, employeeId).stream().findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document introuvable."));
        if (!canSeePersonal(actorId, employeeId) && !Boolean.TRUE.equals(document.get("visible_manager"))) throw forbidden("Ce document est réservé au salarié et aux RH.");
        byte[] bytes = storage.read((String) document.get("cle_stockage"), employeeId);
        return new Download(bytes, (String) document.get("nom_fichier"), (String) document.get("type_mime"));
    }

    @Transactional(readOnly = true)
    public Download downloadDocumentVersion(Integer actorId, Integer employeeId, Integer documentId, Integer versionId) {
        authorizeEmployee(actorId, employeeId);
        Map<String, Object> version = jdbc.queryForList("""
            SELECT v.cle_stockage, v.nom_fichier, v.type_mime, t.visible_manager
            FROM documents_employe d JOIN types_documents_employe t ON t.id = d.type_document_id
            JOIN versions_documents_employe v ON v.document_id = d.id
            WHERE d.id = ? AND d.employe_id = ? AND d.archive_at IS NULL AND v.id = ?
            """, documentId, employeeId, versionId).stream().findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Version de document introuvable."));
        if (!canSeePersonal(actorId, employeeId) && !Boolean.TRUE.equals(version.get("visible_manager"))) throw forbidden("Ce document est réservé au salarié et aux RH.");
        return new Download(storage.read((String) version.get("cle_stockage"), employeeId),
            (String) version.get("nom_fichier"), (String) version.get("type_mime"));
    }

    @Transactional
    public void archiveDocument(Integer actorId, Integer employeeId, Integer documentId) {
        requireActiveRole(actorId, "rh");
        int changed = jdbc.update("UPDATE documents_employe SET archive_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP WHERE id = ? AND employe_id = ? AND archive_at IS NULL", documentId, employeeId);
        if (changed != 1) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document introuvable ou déjà archivé.");
        event(employeeId, "DOCUMENT_ARCHIVE", actorId, "DOCUMENT", documentId, null, "Document administratif archivé");
    }

    @Transactional
    public Map<String, Object> changeAdministrativeStatus(Integer actorId, Integer employeeId, String requestedCode,
            LocalDate effectiveDate, String reason) {
        requireActiveRole(actorId, "rh");
        if (effectiveDate == null || effectiveDate.isAfter(LocalDate.now())) throw bad("La date d'effet doit être aujourd'hui ou dans le passé.");
        if (reason == null || reason.isBlank() || reason.trim().length() > 2000) throw bad("Un motif de 1 à 2000 caractères est obligatoire.");
        String next = requestedCode == null ? "" : requestedCode.trim().toUpperCase(java.util.Locale.ROOT);
        Map<String, Object> current = jdbc.queryForList("""
            SELECT e.user_id AS user_id, e.date_embauche, COALESCE(s.code, 'ACTIF') AS current_code
            FROM employes e LEFT JOIN situation_administrative_employe situation ON situation.employe_id = e.id
            LEFT JOIN statuts_administratifs_employe s ON s.id = situation.statut_id
            WHERE e.id = ? FOR UPDATE OF e
            """, employeeId).stream().findFirst().orElseThrow(this::missingEmployee);
        String previous = (String) current.get("current_code");
        boolean allowed = switch (previous) {
            case "EN_PREPARATION" -> Set.of("ACTIF", "SORTI").contains(next);
            case "ACTIF" -> Set.of("EN_PREAVIS", "SORTI").contains(next);
            case "EN_PREAVIS" -> Set.of("ACTIF", "SORTI").contains(next);
            default -> false;
        };
        if (!allowed) throw new ResponseStatusException(HttpStatus.CONFLICT, "Le changement de statut demandé ne suit pas les transitions autorisées.");
        LocalDate hireDate = asLocalDate(current.get("date_embauche"));
        if ("ACTIF".equals(next) && effectiveDate.isBefore(hireDate)) throw bad("Un salarié ne peut devenir actif avant sa date d'embauche.");
        if ("SORTI".equals(next) && Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM employes e JOIN candidatures c ON c.candidat_id=e.user_id
                JOIN contrats ct ON ct.candidature_id=c.id JOIN statuts_contrat sc ON sc.id=ct.statut_id
                WHERE e.id=? AND sc.code='ACTIF')
                """, Boolean.class, employeeId))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Le contrat actif doit d'abord être terminé dans le module Contrats.");
        }
        Integer nextId = statusId(next);
        if (nextId == null) throw bad("Le statut administratif sélectionné n'existe pas.");
        jdbc.update("""
            INSERT INTO situation_administrative_employe (employe_id, statut_id, date_effet, motif, modifie_par_id, updated_at)
            VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
            ON CONFLICT (employe_id) DO UPDATE SET statut_id=EXCLUDED.statut_id, date_effet=EXCLUDED.date_effet,
                motif=EXCLUDED.motif, modifie_par_id=EXCLUDED.modifie_par_id, updated_at=CURRENT_TIMESTAMP
            """, employeeId, nextId, effectiveDate, reason.trim(), actorId);
        jdbc.update("INSERT INTO historique_statut_employe (employe_id, statut_id, date_effet, motif, acteur_id) VALUES (?, ?, ?, ?, ?)",
            employeeId, nextId, effectiveDate, reason.trim(), actorId);
        event(employeeId, "STATUT_CHANGE", actorId, "STATUT_ADMINISTRATIF", nextId, effectiveDate,
            "Statut administratif modifie : " + previous + " -> " + next);
        Integer employeeUserId = ((Number) current.get("user_id")).intValue();
        notifications.notifyUser(employeeUserId, "DOSSIER_STATUT", "Statut administratif mis à jour",
            "Votre statut administratif est passé à " + next + ". Motif : " + reason.trim(), "/espace/dossiers/me");
        if ("SORTI".equals(next)) notifyStaffingShortage(employeeId);
        return dossier(actorId, employeeId);
    }

    private void notifyStaffingShortage(Integer employeeId) {
        Map<String, Object> employee = jdbc.queryForList("""
            SELECT e.poste_id,p.departement_id,p.intitule,d.nom AS departement
            FROM employes e JOIN postes p ON p.id=e.poste_id JOIN departements d ON d.id=p.departement_id WHERE e.id=?
            """, employeeId).stream().findFirst().orElse(null);
        if (employee == null) return;
        Integer postId = ((Number) employee.get("poste_id")).intValue();
        Integer active = jdbc.queryForObject("""
            SELECT COUNT(*) FROM employes e LEFT JOIN situation_administrative_employe sa ON sa.employe_id=e.id
            LEFT JOIN statuts_administratifs_employe s ON s.id=sa.statut_id
            WHERE e.poste_id=? AND e.date_embauche<=CURRENT_DATE AND COALESCE(s.code,'ACTIF') NOT IN ('SORTI','EN_PREPARATION')
            """, Integer.class, postId);
        Integer minimum = jdbc.queryForObject("""
            SELECT COALESCE((SELECT minimum_presence FROM regles_effectif_minimal
              WHERE poste_id=? AND actif=TRUE AND date_debut<=CURRENT_DATE AND (date_fin IS NULL OR date_fin>=CURRENT_DATE)
              ORDER BY date_debut DESC LIMIT 1),0)
            """, Integer.class, postId);
        if (active == null || minimum == null || minimum == 0 || active >= minimum) return;
        Integer deptId = ((Number) employee.get("departement_id")).intValue();
        String message = "Après un départ, l’effectif actif du poste " + employee.get("intitule") + " (" + employee.get("departement")
            + ") est de " + active + "/" + minimum + ". Vérifiez le besoin et, si nécessaire, ouvrez une demande de recrutement.";
        List<Integer> managers = jdbc.queryForList("""
            SELECT DISTINCT e.user_id FROM employes e JOIN postes p ON p.id=e.poste_id
            JOIN user_roles ur ON ur.user_id=e.user_id JOIN roles r ON r.id=ur.role_id
            JOIN users u ON u.id=e.user_id AND u.is_suspended=FALSE
            WHERE p.departement_id=? AND LOWER(r.role_name)='manager'
            """, Integer.class, deptId);
        for (Integer managerId : managers) notifications.notifyUser(managerId, "EFFECTIF_SOUS_MINIMUM", "Alerte de sous-effectif", message, "/espace/manager/demandes/nouvelle");
        notifications.notifyRole("rh", "EFFECTIF_SOUS_MINIMUM", "Alerte de sous-effectif", message, "/espace/rh/conges/parametres");
    }

    @Transactional(readOnly = true)
    public Map<String, Object> administrativeAlerts(Integer actorId) {
        requireActiveRole(actorId, "rh");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("documentsManquants", jdbc.queryForList("""
            SELECT e.id AS employeeId, e.matricule, u.full_name AS employe, d.nom AS departement,
                   type.code AS typeCode, type.libelle AS type
            FROM employes e JOIN users u ON u.id=e.user_id JOIN postes p ON p.id=e.poste_id JOIN departements d ON d.id=p.departement_id
            LEFT JOIN situation_administrative_employe situation ON situation.employe_id=e.id
            LEFT JOIN statuts_administratifs_employe employee_status ON employee_status.id=situation.statut_id
            CROSS JOIN types_documents_employe type
            WHERE type.actif=TRUE AND type.obligatoire=TRUE AND NOT EXISTS (
                SELECT 1 FROM documents_employe doc WHERE doc.employe_id=e.id AND doc.type_document_id=type.id AND doc.archive_at IS NULL)
              AND COALESCE(employee_status.code, 'ACTIF') <> 'SORTI'
            ORDER BY d.nom, u.full_name, type.libelle
            """));
        result.put("documentsExpirants", jdbc.queryForList("""
            SELECT e.id AS employeeId, e.matricule, u.full_name AS employe, d.nom AS departement,
                   doc.id AS documentId, type.libelle AS type, latest.date_expiration AS dateExpiration
            FROM documents_employe doc JOIN employes e ON e.id=doc.employe_id JOIN users u ON u.id=e.user_id
            JOIN postes p ON p.id=e.poste_id JOIN departements d ON d.id=p.departement_id
            JOIN types_documents_employe type ON type.id=doc.type_document_id
            JOIN LATERAL (SELECT v.date_expiration FROM versions_documents_employe v WHERE v.document_id=doc.id ORDER BY v.numero_version DESC LIMIT 1) latest ON TRUE
            LEFT JOIN situation_administrative_employe situation ON situation.employe_id=e.id
            LEFT JOIN statuts_administratifs_employe employee_status ON employee_status.id=situation.statut_id
            WHERE doc.archive_at IS NULL AND latest.date_expiration <= CURRENT_DATE + INTERVAL '30 days'
              AND COALESCE(employee_status.code, 'ACTIF') <> 'SORTI'
            ORDER BY latest.date_expiration, d.nom, u.full_name
            """));
        result.put("contratsEcheance", jdbc.queryForList("""
            SELECT e.id AS employeeId, e.matricule, u.full_name AS employe, d.nom AS departement,
                   ct.numero AS contrat, ct.date_fin AS dateFin, sc.libelle AS statut
            FROM contrats ct JOIN candidatures c ON c.id=ct.candidature_id JOIN employes e ON e.user_id=c.candidat_id
            JOIN users u ON u.id=e.user_id JOIN postes p ON p.id=e.poste_id JOIN departements d ON d.id=p.departement_id
            JOIN statuts_contrat sc ON sc.id=ct.statut_id
            WHERE sc.code = 'ACTIF' AND ct.date_fin BETWEEN CURRENT_DATE AND CURRENT_DATE + INTERVAL '60 days'
            ORDER BY ct.date_fin, d.nom, u.full_name
            """));
        result.put("informationsIncompletes", jdbc.queryForList("""
            SELECT e.id AS employeeId, e.matricule, u.full_name AS employe, d.nom AS departement,
                   concat_ws(', ', CASE WHEN NULLIF(trim(u.full_name), '') IS NULL THEN 'nom complet' END,
                       CASE WHEN NULLIF(trim(u.phone_number), '') IS NULL THEN 'telephone' END,
                       CASE WHEN NULLIF(trim(u.region), '') IS NULL THEN 'region' END) AS champsManquants
            FROM employes e JOIN users u ON u.id=e.user_id JOIN postes p ON p.id=e.poste_id JOIN departements d ON d.id=p.departement_id
            LEFT JOIN situation_administrative_employe situation ON situation.employe_id=e.id
            LEFT JOIN statuts_administratifs_employe employee_status ON employee_status.id=situation.statut_id
            WHERE (NULLIF(trim(u.full_name), '') IS NULL OR NULLIF(trim(u.phone_number), '') IS NULL OR NULLIF(trim(u.region), '') IS NULL)
              AND COALESCE(employee_status.code, 'ACTIF') <> 'SORTI'
            ORDER BY d.nom, u.full_name
            """));
        result.put("correctionsEnAttente", jdbc.queryForList("""
            SELECT r.id AS demandeId, e.id AS employeeId, e.matricule, u.full_name AS employe,
                   field.libelle AS champ, r.soumise_at AS soumiseAt
            FROM demandes_modification_dossier r JOIN employes e ON e.id=r.employe_id JOIN users u ON u.id=e.user_id
            JOIN champs_dossier_modifiables field ON field.id=r.champ_id JOIN statuts_demande_dossier status ON status.id=r.statut_id
            WHERE r.en_cours=TRUE AND status.code IN ('SOUMISE','EN_VERIFICATION')
            ORDER BY r.soumise_at, r.id
            """));
        return result;
    }

    @Transactional
    public Map<String, Object> submitCorrection(Integer actorId, CorrectionRequest request) {
        return submitCorrection(actorId, request, null);
    }

    @Transactional
    public Map<String, Object> submitCorrection(Integer actorId, CorrectionRequest request, MultipartFile attachment) {
        if (request == null || request.champ() == null || request.champ().isBlank() || request.champ().length() > 40
                || request.nouvelleValeur() == null || request.nouvelleValeur().isBlank() || request.nouvelleValeur().length() > 5000
                || request.motif() == null || request.motif().isBlank() || request.motif().length() > 2000) {
            throw bad("Le champ, la valeur proposée et un motif valide sont obligatoires.");
        }
        Integer employeeId = employeeIdForUser(actorId);
        requireEmployeeOwner(actorId, employeeId);
        String code = request.champ().trim().toUpperCase(java.util.Locale.ROOT);
        Integer champId = jdbc.query("SELECT id FROM champs_dossier_modifiables WHERE code = ? AND actif = TRUE", rs -> rs.next() ? rs.getInt(1) : null, code);
        if (champId == null) throw bad("Le champ sélectionné ne peut pas être modifié par une demande de correction.");
        String proposed = validateCorrectionValue(code, request.nouvelleValeur());
        jdbc.query("SELECT id FROM employes WHERE id = ? FOR UPDATE", rs -> { if (!rs.next()) throw missingEmployee(); return null; }, employeeId);
        String current = currentValue(employeeId, code);
        if (Objects.equals(current, proposed)) throw bad("La valeur proposée est identique à la valeur actuelle.");
        Integer statusId = statusId("SOUMISE"); Integer requestId;
        try {
            requestId = jdbc.queryForObject("""
                INSERT INTO demandes_modification_dossier (employe_id, champ_id, statut_id, ancienne_valeur, nouvelle_valeur, motif)
                VALUES (?, ?, ?, ?, ?, ?) RETURNING id
                """, Integer.class, employeeId, champId, statusId, current, proposed, request.motif().trim());
        } catch (DuplicateKeyException exception) { throw new ResponseStatusException(HttpStatus.CONFLICT, "Une demande est déjà ouverte pour ce champ."); }
        if (attachment != null && !attachment.isEmpty()) {
            EmployeeDocumentStorageService.StoredDocument stored = storage.store(employeeId, attachment);
            jdbc.update("""
                INSERT INTO pieces_justificatives_demande_dossier (demande_id, cle_stockage, nom_fichier, type_mime, taille_octets, ajoute_par_id)
                VALUES (?, ?, ?, ?, ?, ?)
                """, requestId, stored.key(), stored.fileName(), stored.mimeType(), stored.size(), actorId);
        }
        event(employeeId, "DEMANDE_SOUMISE", actorId, "DEMANDE_CORRECTION", requestId, null, "Demande de correction soumise");
        notifications.notifyRole("rh", "DOSSIER_A_VALIDER", "Demande de correction à traiter", "Une demande de correction de dossier attend une décision RH.", "/espace/rh/personnel/demandes");
        return correctionById(actorId, requestId, false);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> corrections(Integer actorId, boolean rh) {
        if (rh) requireActiveRole(actorId, "rh");
        else requireEmployeeOwner(actorId, employeeIdForUser(actorId));
        String filter = rh ? "" : " AND e.user_id = ?";
        String sql = """
            SELECT r.id, r.employe_id AS employeeId, e.matricule, u.full_name AS employe, c.code AS champCode,
                   c.libelle AS champ, r.ancienne_valeur AS ancienneValeur, r.nouvelle_valeur AS nouvelleValeur,
                   r.motif, s.code AS statutCode, s.libelle AS statut, r.commentaire_decision AS commentaireDecision,
                   r.soumise_at AS soumiseAt, r.decidee_at AS decideeAt,
                   attachment.nom_fichier AS pieceJointe, attachment.type_mime AS pieceJointeMime
            FROM demandes_modification_dossier r JOIN employes e ON e.id = r.employe_id JOIN users u ON u.id = e.user_id
            JOIN champs_dossier_modifiables c ON c.id = r.champ_id JOIN statuts_demande_dossier s ON s.id = r.statut_id
            LEFT JOIN pieces_justificatives_demande_dossier attachment ON attachment.demande_id = r.id
            WHERE TRUE
            """ + filter + " ORDER BY r.soumise_at DESC, r.id DESC";
        return rh ? jdbc.queryForList(sql) : jdbc.queryForList(sql, employeeIdForUser(actorId));
    }

    @Transactional
    public Map<String, Object> decideCorrection(Integer actorId, Integer requestId, String decision, String comment) {
        requireActiveRole(actorId, "rh");
        String code = decision == null ? "" : decision.trim().toUpperCase(java.util.Locale.ROOT);
        if (!Set.of("APPROUVEE", "REFUSEE").contains(code)) throw bad("Décision de demande invalide.");
        if ("REFUSEE".equals(code) && (comment == null || comment.isBlank())) throw bad("Un motif de refus est obligatoire.");
        Map<String, Object> request = jdbc.queryForList("""
            SELECT r.*, c.code AS champ_code, s.code AS statut_code, e.user_id AS employee_user_id
            FROM demandes_modification_dossier r JOIN champs_dossier_modifiables c ON c.id = r.champ_id
            JOIN statuts_demande_dossier s ON s.id = r.statut_id JOIN employes e ON e.id = r.employe_id
            WHERE r.id = ? FOR UPDATE OF r
            """, requestId).stream().findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Demande introuvable."));
        if (!Boolean.TRUE.equals(request.get("en_cours")) || !Set.of("SOUMISE", "EN_VERIFICATION").contains(request.get("statut_code"))) throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette demande a déjà été traitée.");
        Integer employeeId = ((Number) request.get("employe_id")).intValue(); String champ = (String) request.get("champ_code");
        if ("APPROUVEE".equals(code)) {
            String actual = currentValue(employeeId, champ);
            if (!Objects.equals(actual, request.get("ancienne_valeur"))) throw new ResponseStatusException(HttpStatus.CONFLICT, "La valeur du dossier a changé depuis la demande; vérifiez le dossier avant de décider.");
            applyCorrection(employeeId, champ, (String) request.get("nouvelle_valeur"));
        }
        jdbc.update("""
            UPDATE demandes_modification_dossier SET statut_id = ?, commentaire_decision = ?, decidee_par_id = ?,
                decidee_at = CURRENT_TIMESTAMP, en_cours = FALSE, version = version + 1, updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """, statusId(code), comment == null || comment.isBlank() ? null : comment.trim(), actorId, requestId);
        event(employeeId, "DEMANDE_DECIDEE", actorId, "DEMANDE_CORRECTION", requestId, null, "Décision RH enregistrée");
        notifications.notifyUser(((Number) request.get("employee_user_id")).intValue(), "DOSSIER_STATUT", "Demande de correction traitée",
            "Votre demande de correction a été " + ("APPROUVEE".equals(code) ? "approuvée." : "refusée."), "/espace/employe/dossier/demandes");
        return correctionById(actorId, requestId, true);
    }

    @Transactional(readOnly = true)
    public Download downloadCorrectionAttachment(Integer actorId, Integer requestId) {
        Map<String, Object> attachment = jdbc.queryForList("""
            SELECT e.id AS employee_id, attachment.cle_stockage, attachment.nom_fichier, attachment.type_mime
            FROM demandes_modification_dossier request
            JOIN employes e ON e.id = request.employe_id
            JOIN pieces_justificatives_demande_dossier attachment ON attachment.demande_id = request.id
            WHERE request.id = ?
            """, requestId).stream().findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pièce justificative introuvable."));
        int employeeId = ((Number) attachment.get("employee_id")).intValue();
        if (!isRh(actorId) && !Objects.equals(employeeIdForUserOrNull(actorId), employeeId)) throw forbidden("Pièce justificative non autorisée.");
        return new Download(storage.read((String) attachment.get("cle_stockage"), employeeId),
            (String) attachment.get("nom_fichier"), (String) attachment.get("type_mime"));
    }

    @Transactional
    public Map<String, Object> cancelCorrection(Integer actorId, Integer requestId) {
        Integer employeeId = employeeIdForUser(actorId);
        requireEmployeeOwner(actorId, employeeId);
        Map<String, Object> request = jdbc.queryForList("""
            SELECT r.en_cours, status.code AS status_code
            FROM demandes_modification_dossier r
            JOIN statuts_demande_dossier status ON status.id = r.statut_id
            WHERE r.id = ? AND r.employe_id = ? FOR UPDATE OF r
            """, requestId, employeeId).stream().findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Demande introuvable."));
        if (!Boolean.TRUE.equals(request.get("en_cours")) || !"SOUMISE".equals(request.get("status_code"))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Seule une demande encore soumise peut être annulée.");
        }
        jdbc.update("""
            UPDATE demandes_modification_dossier SET statut_id = ?, en_cours = FALSE, decidee_par_id = ?,
                decidee_at = CURRENT_TIMESTAMP, commentaire_decision = 'Annulée par le salarié',
                version = version + 1, updated_at = CURRENT_TIMESTAMP WHERE id = ?
            """, statusId("ANNULEE"), actorId, requestId);
        event(employeeId, "DEMANDE_DECIDEE", actorId, "DEMANDE_CORRECTION", requestId, null, "Demande de correction annulée par le salarié");
        notifications.notifyRole("rh", "DOSSIER_A_VALIDER", "Demande de correction annulée", "Une demande de correction a été retirée par le salarié.", "/espace/rh/personnel/demandes");
        return correctionById(actorId, requestId, false);
    }

    private Map<String, Object> correctionById(Integer actorId, Integer requestId, boolean rh) {
        return corrections(actorId, rh).stream().filter(row -> ((Number) row.get("id")).intValue() == requestId).findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Demande introuvable."));
    }

    private String currentValue(Integer employeeId, String field) {
        String sql = switch (field) {
            case "FULL_NAME", "PHONE_NUMBER", "REGION" -> "SELECT u." + switch (field) { case "FULL_NAME" -> "full_name"; case "PHONE_NUMBER" -> "phone_number"; default -> "region"; } + " FROM employes e JOIN users u ON u.id=e.user_id WHERE e.id=?";
            case "ADRESSE_LIGNE", "VILLE", "CODE_POSTAL", "PAYS", "CONTACT_URGENCE_NOM", "CONTACT_URGENCE_LIEN", "CONTACT_URGENCE_TELEPHONE" -> "SELECT " + field.toLowerCase(java.util.Locale.ROOT) + " FROM informations_personnelles_employe WHERE employe_id=?";
            default -> throw bad("Champ de dossier non autorisé.");
        };
        return jdbc.query(sql, rs -> rs.next() ? rs.getString(1) : null, employeeId);
    }

    private void applyCorrection(Integer employeeId, String field, String value) {
        switch (field) {
            case "FULL_NAME" -> jdbc.update("UPDATE users SET full_name=?, updated_at=CURRENT_TIMESTAMP WHERE id=(SELECT user_id FROM employes WHERE id=?)", value, employeeId);
            case "PHONE_NUMBER" -> jdbc.update("UPDATE users SET phone_number=?, updated_at=CURRENT_TIMESTAMP WHERE id=(SELECT user_id FROM employes WHERE id=?)", value, employeeId);
            case "REGION" -> jdbc.update("UPDATE users SET region=?, updated_at=CURRENT_TIMESTAMP WHERE id=(SELECT user_id FROM employes WHERE id=?)", value, employeeId);
            case "ADRESSE_LIGNE", "VILLE", "CODE_POSTAL", "PAYS", "CONTACT_URGENCE_NOM", "CONTACT_URGENCE_LIEN", "CONTACT_URGENCE_TELEPHONE" ->
                jdbc.update("UPDATE informations_personnelles_employe SET " + field.toLowerCase(java.util.Locale.ROOT) + "=?, updated_at=CURRENT_TIMESTAMP WHERE employe_id=?", value, employeeId);
            default -> throw bad("Champ de dossier non autorisé.");
        }
    }

    private String validateCorrectionValue(String code, String raw) {
        String value = raw.trim(); int max = code.equals("FULL_NAME") ? 150 : code.equals("REGION") ? 120 : 250;
        if (value.isBlank() || value.length() > max) throw bad("La nouvelle valeur est vide ou trop longue.");
        if ((code.equals("PHONE_NUMBER") || code.equals("CONTACT_URGENCE_TELEPHONE")) && !value.matches("[+0-9() .-]{7,30}")) throw bad("Le numéro de téléphone est invalide.");
        return value;
    }

    private Map<String, Object> personalInfo(Integer employeeId) {
        Map<String, Object> row = jdbc.queryForList("""
            SELECT adresse_ligne AS adresse, ville, code_postal AS codePostal, pays,
                   contact_urgence_nom AS contactUrgenceNom, contact_urgence_lien AS contactUrgenceLien,
                   contact_urgence_telephone AS contactUrgenceTelephone
            FROM informations_personnelles_employe WHERE employe_id = ?
            """, employeeId).stream().findFirst().orElse(Map.of());
        Map<String, Object> info = new LinkedHashMap<>(row);
        info.put("phoneNumber", jdbc.queryForObject("SELECT phone_number FROM users WHERE id=(SELECT user_id FROM employes WHERE id=?)", String.class, employeeId));
        info.put("region", jdbc.queryForObject("SELECT region FROM users WHERE id=(SELECT user_id FROM employes WHERE id=?)", String.class, employeeId));
        return info;
    }

    private void event(Integer employeeId, String type, Integer actorId, String objectType, Integer objectId, LocalDate effectiveDate, String summary) {
        jdbc.update("""
            INSERT INTO evenements_dossier_employe (employe_id, type_evenement_id, acteur_id, objet_type, objet_id, date_effet, resume)
            SELECT ?, id, ?, ?, ?, ?, ? FROM types_evenements_dossier WHERE code = ?
            """, employeeId, actorId, objectType, objectId, effectiveDate, summary, type);
    }

    private void authorizeDocumentWrite(Integer actorId, Integer employeeId) {
        authorizeEmployee(actorId, employeeId);
        if (!isRh(actorId) && !employeeIdForUser(actorId).equals(employeeId)) throw forbidden("Vous ne pouvez modifier que vos propres documents.");
    }

    private void authorizeEmployee(Integer actorId, Integer employeeId) {
        if (isRh(actorId) || employeeIdForUserOrNull(actorId) != null && employeeIdForUserOrNull(actorId).equals(employeeId)) return;
        if (hasRole(actorId, "manager") && managerDepartment(actorId).equals(employeeDepartment(employeeId))) return;
        throw forbidden("Ce dossier salarié n’est pas dans votre périmètre.");
    }

    private boolean canSeePersonal(Integer actorId, Integer employeeId) { return isRh(actorId) || Objects.equals(employeeIdForUserOrNull(actorId), employeeId); }
    private String photoUrl(Integer actorId, Integer employeeId) {
        String stored = jdbc.queryForObject("SELECT photo_profil_path FROM users WHERE id=(SELECT user_id FROM employes WHERE id=?)", String.class, employeeId);
        return stored == null ? null : (Objects.equals(employeeIdForUserOrNull(actorId), employeeId) ? "/api/candidatures/me/photo" : "/api/dossiers/employes/" + employeeId + "/photo");
    }

    @Transactional(readOnly = true)
    public byte[] profilePhoto(Integer actorId, Integer employeeId) {
        authorizeEmployee(actorId, employeeId);
        String path = jdbc.query("SELECT u.photo_profil_path FROM employes e JOIN users u ON u.id=e.user_id WHERE e.id=?",
            rs -> rs.next() ? rs.getString(1) : null, employeeId);
        if (path == null || path.isBlank()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Photo de profil introuvable.");
        return profilePhotos.readPhoto(path);
    }

    @Transactional(readOnly = true)
    public Download recruitmentCv(Integer actorId, Integer employeeId) {
        authorizeEmployee(actorId, employeeId);
        if (!canSeePersonal(actorId, employeeId)) throw forbidden("Le CV de recrutement est réservé au salarié et aux RH.");
        String path = jdbc.query("""
            SELECT c.cv_path
            FROM employes e JOIN candidatures c ON c.candidat_id = e.user_id
            JOIN contrats ct ON ct.candidature_id = c.id
            WHERE e.id = ? AND c.cv_path IS NOT NULL
            ORDER BY ct.genere_at DESC, ct.id DESC LIMIT 1
            """, rs -> rs.next() ? rs.getString("cv_path") : null, employeeId);
        if (path == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Aucun CV lié à l’embauche n’est disponible.");
        byte[] bytes = profilePhotos.readCv(path);
        String name = path.substring(path.lastIndexOf('/') + 1);
        return new Download(bytes, name, "application/pdf");
    }

    private Map<String, Object> recruitmentCv(Integer employeeId) {
        List<Map<String, Object>> result = jdbc.queryForList("""
            SELECT c.id AS candidatureId, o.titre AS offre, c.envoyee_at AS envoyeeAt,
                   ('cv-candidature-' || c.id || '.pdf') AS nomFichier
            FROM employes e JOIN candidatures c ON c.candidat_id = e.user_id
            JOIN contrats ct ON ct.candidature_id = c.id
            JOIN offres_emploi o ON o.id = c.offre_id
            WHERE e.id = ? AND c.cv_path IS NOT NULL
            ORDER BY ct.genere_at DESC, ct.id DESC LIMIT 1
            """, employeeId);
        return result.isEmpty() ? null : result.get(0);
    }

    private void requireEmployeeOwner(Integer actorId, Integer employeeId) {
        if (!Objects.equals(employeeIdForUserOrNull(actorId), employeeId) || !(hasRole(actorId, "employe") || hasRole(actorId, "manager"))) throw forbidden("La fonction est réservée au salarié concerné.");
    }

    private void requireActiveRole(Integer actorId, String role) {
        boolean active = Boolean.TRUE.equals(jdbc.queryForObject("""
            SELECT EXISTS (SELECT 1 FROM users u JOIN user_roles ur ON ur.user_id=u.id JOIN roles r ON r.id=ur.role_id
                JOIN statuts_validation_compte v ON v.id=u.administrative_validation_status_id
            WHERE u.id=? AND u.is_suspended=FALSE AND v.code='APPROVED' AND LOWER(r.role_name)=?)
            """, Boolean.class, actorId, role));
        if (!active) throw forbidden("Un compte " + role + " actif est requis.");
    }

    private boolean hasRole(Integer userId, String role) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM user_roles ur JOIN roles r ON r.id=ur.role_id WHERE ur.user_id=? AND LOWER(r.role_name)=?)", Boolean.class, userId, role));
    }
    private boolean isRh(Integer userId) { return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM users u JOIN user_roles ur ON ur.user_id=u.id JOIN roles r ON r.id=ur.role_id JOIN statuts_validation_compte s ON s.id=u.administrative_validation_status_id WHERE u.id=? AND u.is_suspended=FALSE AND s.code='APPROVED' AND LOWER(r.role_name)='rh')", Boolean.class, userId)); }
    private Integer employeeIdForUser(Integer userId) { Integer id = employeeIdForUserOrNull(userId); if (id == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Aucune fiche salarié n’est associée à ce compte."); return id; }
    private Integer employeeIdForUserOrNull(Integer userId) { return jdbc.query("SELECT id FROM employes WHERE user_id=?", rs -> rs.next() ? rs.getInt(1) : null, userId); }
    private Integer employeeDepartment(Integer employeeId) { return jdbc.queryForObject("SELECT p.departement_id FROM employes e JOIN postes p ON p.id=e.poste_id WHERE e.id=?", Integer.class, employeeId); }
    private Integer managerDepartment(Integer managerId) {
        requireActiveRole(managerId, "manager");
        List<Integer> departments = jdbc.queryForList("SELECT DISTINCT p.departement_id FROM employes e JOIN postes p ON p.id=e.poste_id JOIN departements d ON d.id=p.departement_id WHERE e.user_id=? AND p.actif=TRUE AND d.actif=TRUE", Integer.class, managerId);
        if (departments.size() != 1) throw new ResponseStatusException(HttpStatus.CONFLICT, "Le compte manager doit être rattaché à un seul département actif.");
        return departments.getFirst();
    }
    private Integer statusId(String code) { return jdbc.query("SELECT id FROM statuts_demande_dossier WHERE code=?", rs -> rs.next() ? rs.getInt(1) : null, code); }
    private Map<String, Object> one(String sql, Object... args) { return jdbc.queryForList(sql, args).stream().findFirst().orElseThrow(this::missingEmployee); }
    private String seniority(Object value) {
        LocalDate hired = value instanceof LocalDate date ? date : value instanceof java.sql.Date date ? date.toLocalDate() : null;
        if (hired == null || hired.isAfter(LocalDate.now())) return "—";
        var period = java.time.Period.between(hired, LocalDate.now());
        return period.getYears() + " an(s), " + period.getMonths() + " mois";
    }
    private LocalDate asLocalDate(Object value) {
        if (value instanceof LocalDate date) return date;
        if (value instanceof java.sql.Date date) return date.toLocalDate();
        throw bad("La date d'embauche du dossier est invalide.");
    }
    private ResponseStatusException bad(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
    private ResponseStatusException forbidden(String message) { return new ResponseStatusException(HttpStatus.FORBIDDEN, message); }
    private ResponseStatusException missingEmployee() { return new ResponseStatusException(HttpStatus.NOT_FOUND, "Fiche salarié introuvable."); }
    public record Download(byte[] bytes, String fileName, String mimeType) { }
}
