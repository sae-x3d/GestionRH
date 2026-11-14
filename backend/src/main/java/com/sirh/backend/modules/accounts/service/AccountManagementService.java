package com.sirh.backend.modules.accounts.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import com.sirh.backend.modules.accounts.dto.CreateManagedAccountRequest;
import com.sirh.backend.modules.accounts.dto.RegistrationDecisionRequest;
import com.sirh.backend.modules.accounts.dto.SetUserRolesRequest;
import com.sirh.backend.modules.accounts.dto.SetEmployeePostRequest;
import com.sirh.backend.modules.accounts.dto.SuspendAccountRequest;
import com.sirh.backend.modules.candidatures.service.CandidateFileStorageService;
import com.sirh.backend.modules.notifications.service.NotificationCenterService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AccountManagementService {
    private static final Set<String> KNOWN_ROLES = Set.of("simple_user", "employe", "manager", "rh", "administration", "gestionnaire");
    private static final String ACCOUNT_QUERY = """
        SELECT u.id, u.username, u.email, COALESCE(u.full_name, u.username) AS full_name, u.phone_number, u.region, u.photo_profil_path,
               svc.code AS validation_status, svc.libelle AS validation_label,
               u.verification_requested_at, u.is_suspended, u.created_at, e.matricule, e.date_embauche, e.date_naissance,
               p.id AS poste_id, p.intitule AS poste, d.id AS departement_id, d.nom AS departement,
               provenance.libelle AS provenance
        FROM users u
        JOIN statuts_validation_compte svc ON svc.id = u.administrative_validation_status_id
        LEFT JOIN employes e ON e.user_id = u.id
        LEFT JOIN postes p ON p.id = e.poste_id
        LEFT JOIN departements d ON d.id = p.departement_id
        LEFT JOIN provenances_employe provenance ON provenance.id = e.provenance_id
        """;

    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;
    private final NotificationCenterService notifications;
    private final CandidateFileStorageService files;

    public AccountManagementService(JdbcTemplate jdbc, PasswordEncoder passwordEncoder,
            NotificationCenterService notifications, CandidateFileStorageService files) {
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
        this.notifications = notifications;
        this.files = files;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> users(Integer managerId) {
        requireManager(managerId);
        return withRoles(jdbc.queryForList(ACCOUNT_QUERY + " ORDER BY u.created_at DESC, u.id DESC"));
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> employees(Integer managerId) {
        requireManager(managerId);
        return withRoles(jdbc.queryForList(ACCOUNT_QUERY + " WHERE e.id IS NOT NULL ORDER BY d.nom, p.intitule, u.full_name, u.username"));
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> pendingRegistrations(Integer managerId) {
        requireManager(managerId);
        return withRoles(jdbc.queryForList(ACCOUNT_QUERY + " WHERE svc.code = 'PENDING' AND u.verification_requested_at IS NOT NULL ORDER BY u.verification_requested_at, u.id"));
    }

    @Transactional(readOnly = true)
    public byte[] verificationPhoto(Integer managerId, Integer userId) {
        requireManager(managerId);
        Map<String, Object> target = userById(userId);
        String path = jdbc.query("SELECT u.photo_profil_path FROM users u JOIN statuts_validation_compte s ON s.id = u.administrative_validation_status_id WHERE u.id = ? AND s.code = 'PENDING'",
            rs -> rs.next() ? rs.getString(1) : null, userId);
        if (!"PENDING".equals(target.get("validation_status")) || path == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Photo de vérification introuvable.");
        }
        return files.readPhoto(path);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> options(Integer managerId) {
        requireManager(managerId);
        Map<String, Object> options = new LinkedHashMap<>();
        options.put("roles", jdbc.queryForList("SELECT role_name AS code FROM roles ORDER BY role_name"));
        options.put("postes", jdbc.queryForList("""
            SELECT p.id, p.intitule, p.code_matricule AS code_poste,
                   d.nom AS departement, d.code_matricule AS code_departement
            FROM postes p
            JOIN departements d ON d.id = p.departement_id WHERE p.actif = TRUE ORDER BY d.nom, p.intitule
            """));
        return options;
    }

    @Transactional
    public Map<String, Object> create(Integer managerId, CreateManagedAccountRequest request) {
        requireManager(managerId);
        String role = normalizedRole(request.role());
        String username = request.username().trim();
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        boolean hasEmploymentData = request.posteId() != null || request.dateEmbauche() != null
            || request.dateNaissance() != null;
        boolean needsEmploymentData = Set.of("manager", "employe").contains(role);
        if ((needsEmploymentData || hasEmploymentData) && (request.posteId() == null
                || request.dateEmbauche() == null || request.dateNaissance() == null)) {
            throw bad("Pour créer un employé ou un manager, indiquez le poste, la date d’embauche et la date de naissance. Le matricule sera généré automatiquement.");
        }
        if (request.dateNaissance() != null && !request.dateNaissance().isBefore(java.time.LocalDate.now())) {
            throw bad("La date de naissance doit être antérieure à aujourd’hui.");
        }
        if (jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM roles WHERE role_name = ?)", Boolean.class, role) != Boolean.TRUE) {
            throw bad("Le rôle sélectionné n’existe pas.");
        }
        if (request.posteId() != null && !Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM postes WHERE id = ? AND actif = TRUE)", Boolean.class, request.posteId()))) {
            throw bad("Le poste sélectionné est introuvable ou inactif.");
        }
        if (Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM users WHERE LOWER(username) = LOWER(?) OR LOWER(email) = LOWER(?))", Boolean.class, username, email))) {
            throw conflict("Cet identifiant ou cette adresse e-mail est déjà utilisé.");
        }
        try {
            Integer userId = jdbc.queryForObject("""
                INSERT INTO users (username, email, full_name, password_hash, administrative_validation_status_id)
                SELECT ?, ?, ?, ?, id FROM statuts_validation_compte WHERE code = 'APPROVED'
                RETURNING id
                """, Integer.class, username, email, request.fullName().trim(), passwordEncoder.encode(request.password()));
            if (userId == null) throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Le compte n’a pas pu être créé.");
            jdbc.update("INSERT INTO user_roles (user_id, role_id) SELECT ?, id FROM roles WHERE role_name = ?", userId, role);
            if (request.posteId() != null) {
                jdbc.update("""
                    INSERT INTO employes (user_id, poste_id, date_embauche, date_naissance)
                    VALUES (?, ?, ?, ?)
                    """, userId, request.posteId(), request.dateEmbauche(), request.dateNaissance());
            }
            return userById(userId);
        } catch (DuplicateKeyException exception) {
            throw conflict("Cet identifiant, cette adresse e-mail ou ce matricule est déjà utilisé.");
        }
    }

    @Transactional
    public Map<String, Object> setSuspended(Integer managerId, Integer userId, SuspendAccountRequest request) {
        requireManager(managerId);
        if (managerId.equals(userId)) throw bad("Vous ne pouvez pas désactiver votre propre compte.");
        Map<String, Object> target = userById(userId);
        boolean suspended = Boolean.TRUE.equals(request.suspended());
        if (suspended && hasRole(target, "gestionnaire") && isLastActiveManager(userId)) {
            throw conflict("Le dernier compte gestionnaire actif ne peut pas être désactivé.");
        }
        jdbc.update("UPDATE users SET is_suspended = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?", suspended, userId);
        return userById(userId);
    }

    @Transactional
    public Map<String, Object> setRoles(Integer managerId, Integer userId, SetUserRolesRequest request) {
        requireManager(managerId);
        if (managerId.equals(userId)) throw bad("Vous ne pouvez pas modifier vos propres rôles.");
        Map<String, Object> target = userById(userId);
        List<String> roles = request.roles().stream().map(this::normalizedRole).distinct().toList();
        if (roles.isEmpty()) throw bad("Attribuez au moins un rôle au compte.");
        if (roles.stream().anyMatch(role -> !KNOWN_ROLES.contains(role)) || roles.stream().anyMatch(role ->
                !Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM roles WHERE role_name = ?)", Boolean.class, role)))) {
            throw bad("Un des rôles demandés n’existe pas.");
        }
        if (roles.stream().anyMatch(Set.of("manager", "employe")::contains) && target.get("matricule") == null) {
            throw bad("Un rôle manager ou employé exige d’abord une fiche employé avec poste. Le matricule est généré automatiquement.");
        }
        if (hasRole(target, "gestionnaire") && !roles.contains("gestionnaire") && isLastActiveManager(userId)) {
            throw conflict("Le dernier compte gestionnaire actif doit conserver son rôle.");
        }
        jdbc.update("DELETE FROM user_roles WHERE user_id = ?", userId);
        for (String role : roles) jdbc.update("INSERT INTO user_roles (user_id, role_id) SELECT ?, id FROM roles WHERE role_name = ?", userId, role);
        jdbc.update("UPDATE users SET updated_at = CURRENT_TIMESTAMP WHERE id = ?", userId);
        return userById(userId);
    }

    @Transactional
    public Map<String, Object> setEmployeePost(Integer managerId, Integer userId, SetEmployeePostRequest request) {
        requireManager(managerId);
        if (managerId.equals(userId)) throw bad("Vous ne pouvez pas modifier votre propre rattachement.");
        userById(userId);
        if (!Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM postes WHERE id = ? AND actif = TRUE)", Boolean.class, request.posteId()))) {
            throw bad("Le poste sélectionné est introuvable ou inactif.");
        }
        Map<String, Object> employee = jdbc.queryForList("SELECT id, poste_id FROM employes WHERE user_id = ? FOR UPDATE", userId)
            .stream().findFirst().orElseThrow(() -> conflict("Ce compte n’a pas encore de fiche employé. Créez la fiche avant de le rattacher à un département."));
        Integer employeeId = ((Number) employee.get("id")).intValue();
        Integer oldPostId = ((Number) employee.get("poste_id")).intValue();
        if (!oldPostId.equals(request.posteId())) {
            String provenanceCode = jdbc.queryForObject("""
                SELECT CASE WHEN ancien.departement_id = nouveau.departement_id THEN 'PROMOTION' ELSE 'TRANSFERT' END
                FROM postes ancien JOIN postes nouveau ON nouveau.id = ? WHERE ancien.id = ?
                """, String.class, request.posteId(), oldPostId);
            Integer provenanceId = jdbc.queryForObject("SELECT id FROM provenances_employe WHERE code = ?", Integer.class, provenanceCode);
            jdbc.update("""
                INSERT INTO historique_affectation_employe (employe_id, ancien_poste_id, nouveau_poste_id, provenance_id, date_effet, motif, acteur_id)
                VALUES (?, ?, ?, ?, CURRENT_DATE, 'Affectation mise a jour par le gestionnaire des comptes', ?)
                """, employeeId, oldPostId, request.posteId(), provenanceId, managerId);
            jdbc.update("UPDATE employes SET poste_id = ?, provenance_id = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                request.posteId(), provenanceId, employeeId);
        }
        return userById(userId);
    }

    @Transactional
    public Map<String, Object> decideRegistration(Integer managerId, Integer userId, RegistrationDecisionRequest request) {
        requireManager(managerId);
        String status = request.status().trim().toUpperCase(Locale.ROOT);
        if (!Set.of("APPROVED", "REJECTED").contains(status)) throw bad("Décision d’inscription invalide.");
        Map<String, Object> target = userById(userId);
        if (!"PENDING".equals(target.get("validation_status")) || target.get("verification_requested_at") == null) throw conflict("Ce compte n’a pas encore demandé sa vérification ou a déjà été traité.");
        int changed = jdbc.update("""
            UPDATE users SET administrative_validation_status_id = (SELECT id FROM statuts_validation_compte WHERE code = ?),
                is_suspended = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?
                AND administrative_validation_status_id = (SELECT id FROM statuts_validation_compte WHERE code = 'PENDING')
                AND verification_requested_at IS NOT NULL
            """, status, "REJECTED".equals(status), userId);
        if (changed != 1) throw conflict("Cette inscription a déjà été traitée.");
        String summary = "APPROVED".equals(status) ? "Votre compte est validé. Vous pouvez maintenant postuler aux offres." :
            blank(request.comment()) ? "Votre inscription n’a pas été validée." : truncate(request.comment().trim(), 480);
        notifications.notifyUser(userId, "COMPTE_STATUT", "Mise à jour de votre compte", summary, "/connexion");
        return userById(userId);
    }

    private List<Map<String, Object>> withRoles(List<Map<String, Object>> rows) {
        List<Map<String, Object>> result = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            Integer id = ((Number) row.get("id")).intValue();
            Map<String, Object> item = new LinkedHashMap<>(row);
            Object photoPath = item.remove("photo_profil_path");
            item.put("photo_url", photoPath == null ? null : "/api/gestionnaire/comptes/" + id + "/photo");
            item.put("roles", jdbc.queryForList("SELECT r.role_name FROM roles r JOIN user_roles ur ON ur.role_id = r.id WHERE ur.user_id = ? ORDER BY r.role_name", String.class, id));
            result.add(item);
        }
        return result;
    }

    private Map<String, Object> userById(Integer userId) {
        List<Map<String, Object>> rows = jdbc.queryForList(ACCOUNT_QUERY + " WHERE u.id = ?", userId);
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Compte introuvable.");
        return withRoles(rows).getFirst();
    }

    private boolean isLastActiveManager(Integer userId) {
        Integer count = jdbc.queryForObject("""
            SELECT COUNT(*) FROM users u JOIN user_roles ur ON ur.user_id = u.id JOIN roles r ON r.id = ur.role_id
            JOIN statuts_validation_compte s ON s.id = u.administrative_validation_status_id
            WHERE LOWER(r.role_name) = 'gestionnaire' AND u.is_suspended = FALSE AND s.code = 'APPROVED'
            """, Integer.class);
        return count != null && count <= 1;
    }

    private boolean hasRole(Map<String, Object> target, String role) {
        @SuppressWarnings("unchecked") List<String> roles = (List<String>) target.get("roles");
        return roles != null && roles.contains(role);
    }

    private void requireManager(Integer userId) {
        boolean allowed = Boolean.TRUE.equals(jdbc.queryForObject("""
            SELECT EXISTS (SELECT 1 FROM users u JOIN statuts_validation_compte s ON s.id = u.administrative_validation_status_id
                JOIN user_roles ur ON ur.user_id = u.id JOIN roles r ON r.id = ur.role_id
                WHERE u.id = ? AND u.is_suspended = FALSE AND s.code = 'APPROVED' AND LOWER(r.role_name) = 'gestionnaire')
            """, Boolean.class, userId));
        if (!allowed) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Un compte gestionnaire actif est requis.");
    }

    private String normalizedRole(String role) { return role == null ? "" : role.trim().toLowerCase(Locale.ROOT); }
    private boolean blank(String value) { return value == null || value.isBlank(); }
    private String truncate(String value, int max) { return value.length() <= max ? value : value.substring(0, max - 1) + "…"; }
    private ResponseStatusException bad(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
    private ResponseStatusException conflict(String message) { return new ResponseStatusException(HttpStatus.CONFLICT, message); }
}
