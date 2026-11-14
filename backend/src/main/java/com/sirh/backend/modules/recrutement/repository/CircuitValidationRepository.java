package com.sirh.backend.modules.recrutement.repository;

import java.util.Optional;
import com.sirh.backend.modules.recrutement.entity.CircuitValidationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CircuitValidationRepository extends JpaRepository<CircuitValidationEntity, Integer> {
    Optional<CircuitValidationEntity> findFirstByActifTrue();
}
