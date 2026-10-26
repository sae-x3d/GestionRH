package com.gestion.repository;

import com.gestion.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    List<Notification> findByUtilisateurIdUserOrderByDateCreationDesc(Integer idUser);

    List<Notification> findByUtilisateurIdUserAndLuFalseOrderByDateCreationDesc(Integer idUser);

    long countByUtilisateurIdUserAndLuFalse(Integer idUser);

    Optional<Notification> findByIdNotificationAndUtilisateurIdUser(Integer idNotification, Integer idUser);
}
