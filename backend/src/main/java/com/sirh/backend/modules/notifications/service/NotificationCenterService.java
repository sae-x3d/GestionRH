package com.sirh.backend.modules.notifications.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import com.sirh.backend.modules.notifications.dto.NotificationResponse;
import com.sirh.backend.modules.notifications.entity.NotificationEntity;
import com.sirh.backend.modules.notifications.repository.NotificationRepository;
import com.sirh.backend.modules.notifications.repository.NotificationTypeRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class NotificationCenterService {
    private final NotificationRepository notifications;
    private final NotificationTypeRepository notificationTypes;
    private final JdbcTemplate jdbc;

    public NotificationCenterService(NotificationRepository notifications,
            NotificationTypeRepository notificationTypes, JdbcTemplate jdbc) {
        this.notifications = notifications;
        this.notificationTypes = notificationTypes;
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> list(Integer userId) {
        return notifications.findAllByDestinataireIdOrderByCreatedAtDesc(userId).stream().map(this::response).toList();
    }

    @Transactional(readOnly = true)
    public long unreadCount(Integer userId) {
        return notifications.countByDestinataireIdAndLueAtIsNull(userId);
    }

    @Transactional
    public NotificationResponse markRead(Integer userId, Integer notificationId) {
        var notification = ownedNotification(userId, notificationId);
        if (notification.getLueAt() == null) {
            notification.markRead();
            notifications.save(notification);
        }
        return response(notification);
    }

    @Transactional
    public void delete(Integer userId, Integer notificationId) {
        notifications.delete(ownedNotification(userId, notificationId));
    }

    @Transactional
    public void notifyUser(Integer userId, String type, String title, String summary, String link) {
        saveToRecipients(Set.of(userId), type, title, summary, link);
    }

    @Transactional
    public void notifyRole(String role, String type, String title, String summary, String link) {
        List<Integer> userIds = jdbc.queryForList(
            """
            SELECT DISTINCT ur.user_id FROM user_roles ur
            JOIN roles r ON r.id = ur.role_id
            JOIN users u ON u.id = ur.user_id AND u.is_suspended = FALSE
            JOIN statuts_validation_compte svc ON svc.id = u.administrative_validation_status_id
            WHERE LOWER(r.role_name) = LOWER(?) AND svc.code = 'APPROVED'
            """,
            Integer.class, role);
        saveToRecipients(new LinkedHashSet<>(userIds), type, title, summary, link);
    }

    @Transactional
    public void notifyDepartment(String departmentCode, String type, String title, String summary, String link) {
        List<Integer> userIds = jdbc.queryForList("""
            SELECT DISTINCT e.user_id
            FROM employes e
            JOIN postes p ON p.id = e.poste_id AND p.actif = TRUE
            JOIN departements d ON d.id = p.departement_id AND d.actif = TRUE
            WHERE UPPER(d.code) = UPPER(?)
            """, Integer.class, departmentCode);
        saveToRecipients(new LinkedHashSet<>(userIds), type, title, summary, link);
    }

    private void saveToRecipients(Set<Integer> userIds, String type, String title, String summary, String link) {
        if (userIds.isEmpty()) return;
        Integer typeId = notificationTypes.findByCode(type)
            .orElseThrow(() -> new IllegalArgumentException("Type de notification non référencé : " + type)).getId();
        jdbc.batchUpdate("""
            INSERT INTO notifications (destinataire_id, type_id, titre, resume, lien)
            VALUES (?, ?, ?, ?, ?)
            """, userIds, 100, (statement, userId) -> {
                statement.setInt(1, userId);
                statement.setInt(2, typeId);
                statement.setString(3, title);
                statement.setString(4, summary);
                statement.setString(5, link);
            });
    }

    private NotificationEntity ownedNotification(Integer userId, Integer notificationId) {
        return notifications.findByIdAndDestinataireId(notificationId, userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification introuvable."));
    }

    private NotificationResponse response(NotificationEntity notification) {
        return new NotificationResponse(notification.getId(), notification.getType(), notification.getTitre(),
            notification.getResume(), notification.getLien(), notification.getLueAt() != null, notification.getCreatedAt());
    }
}
