package com.sirh.backend.modules.candidatures.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import com.sirh.backend.modules.candidatures.dto.CandidatureCreatedResponse;
import com.sirh.backend.modules.candidatures.dto.CandidatureOptionsResponse;
import com.sirh.backend.modules.candidatures.dto.CandidatureOptionsResponse.Option;
import com.sirh.backend.modules.candidatures.dto.CandidatureRhResponse;
import com.sirh.backend.modules.candidatures.dto.CreateCandidatureRequest;
import com.sirh.backend.modules.candidatures.dto.ParcoursCandidatRequest;
import com.sirh.backend.modules.notifications.service.NotificationCenterService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CandidatureService {
    private final JdbcTemplate jdbc;
    private final CandidateFileStorageService files;
    private final NotificationCenterService notifications;

    public CandidatureService(JdbcTemplate jdbc, CandidateFileStorageService files,
            NotificationCenterService notifications) {
        this.jdbc = jdbc;
        this.files = files;
        this.notifications = notifications;
    }

    @Transactional(readOnly = true)
    public CandidatureOptionsResponse options(Integer userId) {
        requireApprovedCandidate(userId, false);
        List<Option> types = jdbc.query("SELECT code, libelle FROM types_parcours ORDER BY id",
            (row, index) -> new Option(row.getString("code"), row.getString("libelle")));
        List<Option> levels = jdbc.query("SELECT code, libelle FROM niveaux_etude ORDER BY ordre",
            (row, index) -> new Option(row.getString("code"), row.getString("libelle")));
        return new CandidatureOptionsResponse(types, levels);
    }

    @Transactional
    public CandidatureCreatedResponse submit(Integer userId, CreateCandidatureRequest request,
            MultipartFile cv, MultipartFile photo) {
        CandidateAccount candidate = requireApprovedCandidate(userId, true);
        boolean openOffer = !jdbc.query("""
            SELECT o.id FROM offres_emploi o
            JOIN statuts_offre_emploi so ON so.id = o.statut_id
            JOIN demandes_recrutement dr ON dr.id = o.demande_id
            WHERE o.id = ? AND so.code = 'PUBLIEE' AND o.date_cloture >= CURRENT_DATE
              AND dr.effectif > dr.effectif_recrute
            FOR UPDATE OF o
            """, (row, index) -> row.getInt("id"), request.offreId()).isEmpty();
        if (!openOffer) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cette offre est clôturée ou n’est plus disponible.");

        boolean alreadyApplied = Boolean.TRUE.equals(jdbc.queryForObject(
            "SELECT EXISTS (SELECT 1 FROM candidatures WHERE candidat_id = ? AND offre_id = ?)",
            Boolean.class, userId, request.offreId()));
        if (alreadyApplied) throw new ResponseStatusException(HttpStatus.CONFLICT, "Vous avez déjà envoyé une candidature pour cette offre.");

        List<ResolvedPath> parcours = request.parcours().stream().map(this::validateAndResolve).toList();
        CandidateFileStorageService.PreparedFile cvFile = files.prepareCv(candidate.username(), cv);
        CandidateFileStorageService.PreparedFile photoFile = photo == null || photo.isEmpty()
            ? null : files.preparePhoto(candidate.username(), photo);
        int statusId = jdbc.queryForObject("SELECT id FROM statuts_candidature WHERE code = 'ENVOYEE_RH'", Integer.class);
        OffsetDateTime now = OffsetDateTime.now();
        try {
            Integer applicationId = jdbc.queryForObject("""
                INSERT INTO candidatures (offre_id, candidat_id, statut_id, cv_path, envoyee_at)
                VALUES (?, ?, ?, ?, ?)
                RETURNING id
                """, Integer.class, request.offreId(), userId, statusId, cvFile.relativePath(), now);
            if (applicationId == null) throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "La candidature n’a pas pu être enregistrée.");
            jdbc.update("INSERT INTO historique_candidatures (candidature_id, statut_id, acteur_id, commentaire) VALUES (?, ?, ?, ?)",
                applicationId, statusId, userId, "Candidature envoyée.");
            saveParcours(applicationId, parcours);
            files.save(cvFile);
            if (photoFile != null) {
                files.save(photoFile);
                jdbc.update("UPDATE users SET photo_profil_path = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?", photoFile.relativePath(), userId);
            }
            notifications.notifyRole("rh", "CANDIDATURE_A_TRAITER", "Nouvelle candidature reçue",
                candidate.username() + " a candidaté à une offre de recrutement.", "/espace/rh/recrutement/candidatures");
            return new CandidatureCreatedResponse(applicationId, "ENVOYEE_RH", now);
        } catch (DuplicateKeyException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Vous avez déjà envoyé une candidature pour cette offre.");
        }
    }

    @Transactional(readOnly = true)
    public List<CandidatureRhResponse> receivedByRh(Integer rhUserId) {
        requireActiveRh(rhUserId);
        return jdbc.query("""
            SELECT c.id, o.id AS offre_id, COALESCE(u.full_name, u.username) AS candidat, u.email, u.photo_profil_path,
                   o.titre AS titre_offre, p.intitule AS poste, d.nom AS departement,
                   sc.code AS statut_code, sc.libelle AS statut, c.envoyee_at
            FROM candidatures c
            JOIN users u ON u.id = c.candidat_id
            JOIN offres_emploi o ON o.id = c.offre_id
            JOIN demandes_recrutement dr ON dr.id = o.demande_id
            JOIN postes p ON p.id = dr.poste_id
            JOIN departements d ON d.id = dr.departement_id
            JOIN statuts_candidature sc ON sc.id = c.statut_id
            ORDER BY o.id, c.envoyee_at DESC, c.id DESC
            """, (row, index) -> new CandidatureRhResponse(row.getInt("id"), row.getInt("offre_id"), row.getString("candidat"),
                row.getString("email"), row.getString("photo_profil_path") == null ? null : "/api/rh/candidatures/" + row.getInt("id") + "/photo", row.getString("titre_offre"), row.getString("poste"),
                row.getString("departement"), row.getString("statut_code"), row.getString("statut"), row.getObject("envoyee_at", OffsetDateTime.class)));
    }

    @Transactional(readOnly = true)
    public byte[] cvForRh(Integer rhUserId, Integer candidatureId) {
        requireActiveRh(rhUserId);
        String path = jdbc.query("SELECT cv_path FROM candidatures WHERE id = ?", (row, index) -> row.getString("cv_path"), candidatureId)
            .stream().findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Candidature introuvable."));
        return files.readCv(path);
    }

    @Transactional(readOnly = true)
    public byte[] photoForRh(Integer rhUserId, Integer candidatureId) {
        requireActiveRh(rhUserId);
        String path = jdbc.query("SELECT COALESCE(u.photo_profil_path, '') AS photo_profil_path FROM candidatures c JOIN users u ON u.id = c.candidat_id WHERE c.id = ?",
            (row, index) -> row.getString("photo_profil_path"), candidatureId)
            .stream().findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Candidature introuvable."));
        return files.readPhoto(path);
    }

    @Transactional(readOnly = true)
    public byte[] ownProfilePhoto(Integer userId) {
        String path = jdbc.query("SELECT photo_profil_path FROM users WHERE id = ?",
            result -> result.next() ? result.getString("photo_profil_path") : null, userId);
        if (path == null || path.isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Aucune photo de profil n’est enregistrée.");
        }
        return files.readPhoto(path);
    }

    private CandidateAccount requireApprovedCandidate(Integer userId, boolean lock) {
        CandidateAccount account = jdbc.query("""
            SELECT u.username, u.is_suspended, svc.code AS status
            FROM users u JOIN statuts_validation_compte svc ON svc.id = u.administrative_validation_status_id
            WHERE u.id = ?
            """ + (lock ? " FOR UPDATE OF u" : ""), (row, index) -> new CandidateAccount(
                row.getString("username"), row.getBoolean("is_suspended"), row.getString("status")), userId)
            .stream().findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Session utilisateur invalide."));
        if (account.suspended()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ce compte est suspendu et ne peut pas postuler.");
        if (!"APPROVED".equalsIgnoreCase(account.status())) {
            String message = "REJECTED".equalsIgnoreCase(account.status())
                ? "Votre compte n’a pas été validé. Contactez l’administration pour obtenir de l’aide."
                : "NON_VERIFIE".equalsIgnoreCase(account.status())
                    ? "Complétez votre profil et demandez sa vérification avant de postuler."
                    : "Votre compte est en attente de validation. Seuls les comptes validés peuvent envoyer une candidature.";
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, message);
        }
        boolean simpleUser = Boolean.TRUE.equals(jdbc.queryForObject("""
            SELECT EXISTS (
                SELECT 1 FROM user_roles ur JOIN roles r ON r.id = ur.role_id
                WHERE ur.user_id = ? AND LOWER(r.role_name) = 'simple_user'
            )
            """, Boolean.class, userId));
        if (!simpleUser) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "La candidature est réservée aux comptes candidat.");
        return account;
    }

    private void requireActiveRh(Integer userId) {
        boolean authorized = Boolean.TRUE.equals(jdbc.queryForObject("""
            SELECT EXISTS (
                SELECT 1 FROM users u
                JOIN statuts_validation_compte svc ON svc.id = u.administrative_validation_status_id
                JOIN user_roles ur ON ur.user_id = u.id
                JOIN roles r ON r.id = ur.role_id
                WHERE u.id = ? AND u.is_suspended = FALSE
                  AND svc.code = 'APPROVED' AND LOWER(r.role_name) = 'rh'
            )
            """, Boolean.class, userId));
        if (!authorized) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Un compte RH validé est requis pour consulter les candidatures.");
    }

    private ResolvedPath validateAndResolve(ParcoursCandidatRequest item) {
        if (item == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Une ligne du parcours est incomplète.");
        String typeCode = item.typeCode().trim().toUpperCase(Locale.ROOT);
        Integer typeId = lookup("types_parcours", typeCode, "Type de parcours invalide.");
        boolean study = "ETUDE".equals(typeCode);
        Integer levelId = null;
        if (study) {
            if (item.niveauEtudeCode() == null || item.niveauEtudeCode().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le niveau d’étude est obligatoire pour un parcours d’études.");
            }
            levelId = lookup("niveaux_etude", item.niveauEtudeCode().trim().toUpperCase(Locale.ROOT), "Niveau d’étude invalide.");
        } else if (item.niveauEtudeCode() != null && !item.niveauEtudeCode().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le niveau d’étude ne s’applique qu’aux parcours d’études.");
        }
        boolean ongoing = Boolean.TRUE.equals(item.enCours());
        if (ongoing == (item.dateFin() != null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pour chaque parcours, indiquez une date de fin ou cochez « En cours ».");
        }
        if (item.dateFin() != null && item.dateFin().isBefore(item.dateDebut())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La date de fin ne peut pas précéder la date de début.");
        }
        return new ResolvedPath(typeId, levelId, item, ongoing);
    }

    private Integer lookup(String table, String code, String error) {
        if (!List.of("types_parcours", "niveaux_etude").contains(table)) {
            throw new IllegalArgumentException("Reference table not allowed");
        }
        return jdbc.query("SELECT id FROM " + table + " WHERE UPPER(code) = UPPER(?)", (row, index) -> row.getInt("id"), code)
            .stream().findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, error));
    }

    private void saveParcours(Integer candidatureId, List<ResolvedPath> items) {
        int order = 1;
        for (ResolvedPath resolved : items) {
            ParcoursCandidatRequest item = resolved.item();
            Integer parcoursId = jdbc.queryForObject("""
                INSERT INTO parcours (candidature_id, type_parcours_id, niveau_etude_id, intitule, organisme, description, ordre)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                RETURNING id
                """, Integer.class, candidatureId, resolved.typeId(), resolved.levelId(), item.intitule().trim(),
                item.organisme().trim(), clean(item.description()), order++);
            jdbc.update("INSERT INTO durees (parcours_id, date_debut, date_fin, en_cours) VALUES (?, ?, ?, ?)",
                parcoursId, item.dateDebut(), item.dateFin(), resolved.ongoing());
        }
    }

    private String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private record CandidateAccount(String username, boolean suspended, String status) { }
    private record ResolvedPath(Integer typeId, Integer levelId, ParcoursCandidatRequest item, boolean ongoing) { }
}
