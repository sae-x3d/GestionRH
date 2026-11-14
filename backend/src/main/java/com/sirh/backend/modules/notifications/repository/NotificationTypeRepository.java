package com.sirh.backend.modules.notifications.repository;

import java.util.Optional;
import com.sirh.backend.modules.notifications.entity.NotificationTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationTypeRepository extends JpaRepository<NotificationTypeEntity, Integer> {
    Optional<NotificationTypeEntity> findByCode(String code);
}
