package com.sirh.backend.modules.auth.service;

import java.util.LinkedHashMap;
import java.util.Map;
import com.sirh.backend.modules.candidatures.service.CandidateFileStorageService;
import com.sirh.backend.modules.notifications.service.NotificationCenterService;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserProfileService {
    private final JdbcTemplate jdbc;
    private final CandidateFileStorageService files;
    private final NotificationCenterService notifications;

    public UserProfileService(JdbcTemplate jdbc, CandidateFileStorageService files, NotificationCenterService notifications) {
        this.jdbc = jdbc;
        this.files = files;
        this.notifications = notifications;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> profile(Integer userId) {
        return jdbc.query("""
            SELECT u.id, u.username, u.email, u.full_name, u.phone_number, u.region,
                   u.photo_profil_path, s.code AS status, s.libelle AS status_label,
                   u.verification_requested_at
            FROM users u JOIN statuts_validation_compte s ON s.id = u.administrative_validation_status_id
            WHERE u.id = ?
            """, rows -> {
                if (!rows.next()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Compte introuvable.");
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("id", rows.getInt("id"));
                result.put("username", rows.getString("username"));
                result.put("email", rows.getString("email"));
                result.put("fullName", rows.getString("full_name"));
                result.put("phoneNumber", rows.getString("phone_number"));
                result.put("region", rows.getString("region"));
                result.put("photoUrl", rows.getString("photo_profil_path") == null ? null : "/api/candidatures/me/photo?v=" + System.currentTimeMillis());
                result.put("status", rows.getString("status"));
                result.put("statusLabel", rows.getString("status_label"));
                result.put("verificationRequestedAt", rows.getObject("verification_requested_at"));
                result.put("editable", rows.getString("status").equals("NON_VERIFIE") || rows.getString("status").equals("REJECTED"));
                return result;
            }, userId);
    }

    @Transactional
    public Map<String, Object> update(Integer userId, String fullName, String phoneNumber, String region, MultipartFile photo) {
        String name = clean(fullName);
        String phone = clean(phoneNumber);
        String area = clean(region);
        if (name == null || name.length() > 150) throw bad("Saisissez votre nom complet (150 caractères maximum).");
        if (phone == null || !phone.matches("[+0-9() .-]{7,30}")) throw bad("Saisissez un numéro de téléphone valide (7 à 30 caractères).");
        if (area == null || area.length() > 120) throw bad("Saisissez votre région (120 caractères maximum).");
        Account account = accountForUpdate(userId);
        if (!account.editable()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Le profil est verrouillé après l’envoi de la demande de vérification.");
        CandidateFileStorageService.PreparedFile prepared = photo == null || photo.isEmpty()
            ? null : files.preparePhoto(account.username(), photo);
        if (prepared != null) files.save(prepared);
        jdbc.update("""
            UPDATE users SET full_name = ?, phone_number = ?, region = ?,
                photo_profil_path = COALESCE(?, photo_profil_path), updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """, name, phone, area, prepared == null ? null : prepared.relativePath(), userId);
        return profile(userId);
    }

    @Transactional
    public Map<String, Object> requestVerification(Integer userId) {
        Account account = accountForUpdate(userId);
        if (!account.simpleUser()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "La vérification de profil est réservée aux comptes candidats.");
        if (!account.editable()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Une demande de vérification a déjà été envoyée ou le compte est déjà validé.");
        if (account.fullName() == null || account.fullName().isBlank()) throw bad("Complétez votre nom complet avant de demander la vérification.");
        if (account.phone() == null || account.phone().isBlank()) throw bad("Complétez votre numéro de téléphone avant de demander la vérification.");
        if (account.region() == null || account.region().isBlank()) throw bad("Sélectionnez votre région avant de demander la vérification.");
        if (account.photo() == null || account.photo().isBlank()) throw bad("Ajoutez votre photo de profil avant de demander la vérification.");
        int updated = jdbc.update("""
            UPDATE users SET administrative_validation_status_id = (SELECT id FROM statuts_validation_compte WHERE code = 'PENDING'),
                verification_requested_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
            WHERE id = ? AND EXISTS (
                SELECT 1 FROM statuts_validation_compte status
                WHERE status.id = users.administrative_validation_status_id
                  AND status.code IN ('NON_VERIFIE', 'REJECTED')
            )
            """, userId);
        if (updated != 1) throw new ResponseStatusException(HttpStatus.CONFLICT, "Le statut du compte a changé. Actualisez la page.");
        notifications.notifyRole("gestionnaire", "INSCRIPTION_A_TRAITER", "Profil candidat à vérifier",
            account.fullName() + " a complété son profil et demande la validation de son compte.", "/espace/gestionnaire/comptes/inscriptions");
        return profile(userId);
    }

    private Account accountForUpdate(Integer userId) {
        return jdbc.query("""
            SELECT u.username, u.full_name, u.phone_number, u.region, u.photo_profil_path, s.code AS status,
                   EXISTS (SELECT 1 FROM user_roles ur JOIN roles r ON r.id = ur.role_id WHERE ur.user_id = u.id AND r.role_name = 'simple_user') AS simple_user
            FROM users u JOIN statuts_validation_compte s ON s.id = u.administrative_validation_status_id
            WHERE u.id = ? FOR UPDATE OF u
            """, rows -> {
                if (!rows.next()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Compte introuvable.");
                String status = rows.getString("status");
                return new Account(rows.getString("username"), rows.getString("full_name"), rows.getString("phone_number"),
                    rows.getString("region"), rows.getString("photo_profil_path"), status,
                    status.equals("NON_VERIFIE") || status.equals("REJECTED"), rows.getBoolean("simple_user"));
            }, userId);
    }

    private String clean(String value) { return value == null ? null : value.trim(); }
    private ResponseStatusException bad(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
    private record Account(String username, String fullName, String phone, String region, String photo, String status, boolean editable, boolean simpleUser) { }
}
