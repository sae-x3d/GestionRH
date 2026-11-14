package com.sirh.backend.modules.recrutement.repository;

import java.util.Optional;
import com.sirh.backend.modules.recrutement.entity.DecisionValidationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DecisionValidationRepository extends JpaRepository<DecisionValidationEntity, Integer> {
    Optional<DecisionValidationEntity> findByCode(String code);
}
