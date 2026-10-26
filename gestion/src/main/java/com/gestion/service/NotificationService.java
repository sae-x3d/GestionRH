package com.gestion.service;

import com.gestion.model.Notification;
import com.gestion.model.Utilisateur;
import com.gestion.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional
    public Notification envoyer(Utilisateur destinataire, String type, String message, String lien) {
        if (destinataire == null) {
            return null;
        }
        Notification notification = Notification.builder()
                .utilisateur(destinataire)
                .type(type)
                .message(message)
                .lien(lien)
                .dateCreation(LocalDateTime.now())
                .lu(false)
                .build();
        return notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public List<Notification> lister(Integer idUser) {
        return notificationRepository.findByUtilisateurIdUserOrderByDateCreationDesc(idUser);
    }

    @Transactional(readOnly = true)
    public long compterNonLues(Integer idUser) {
        return notificationRepository.countByUtilisateurIdUserAndLuFalse(idUser);
    }

    @Transactional
    public boolean marquerLue(Integer idNotification, Integer idUser) {
        return notificationRepository
                .findByIdNotificationAndUtilisateurIdUser(idNotification, idUser)
                .map(notification -> {
                    if (!notification.isLu()) {
                        notification.setLu(true);
                        notification.setDateLecture(LocalDateTime.now());
                        notificationRepository.save(notification);
                    }
                    return true;
                })
                .orElse(false);
    }

    @Transactional
    public void marquerToutesLues(Integer idUser) {
        List<Notification> nonLues = notificationRepository
                .findByUtilisateurIdUserAndLuFalseOrderByDateCreationDesc(idUser);
        LocalDateTime maintenant = LocalDateTime.now();
        for (Notification notification : nonLues) {
            notification.setLu(true);
            notification.setDateLecture(maintenant);
        }
        notificationRepository.saveAll(nonLues);
    }
}
