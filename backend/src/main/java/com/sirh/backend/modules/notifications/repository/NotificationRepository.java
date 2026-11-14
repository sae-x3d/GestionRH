package com.sirh.backend.modules.notifications.repository;

import java.util.List;
import java.util.Optional;
import com.sirh.backend.modules.notifications.entity.NotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<NotificationEntity, Integer> {
    List<NotificationEntity> findAllByDestinataireIdOrderByCreatedAtDesc(Integer destinataireId);
    Optional<NotificationEntity> findByIdAndDestinataireId(Integer id, Integer destinataireId);
    long countByDestinataireIdAndLueAtIsNull(Integer destinataireId);
}
