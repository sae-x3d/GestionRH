package com.sirh.backend.modules.recrutement.repository;

import java.util.Optional;
import com.sirh.backend.modules.recrutement.entity.EtapeCircuitEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EtapeCircuitRepository extends JpaRepository<EtapeCircuitEntity, Integer> {
    Optional<EtapeCircuitEntity> findByCircuitIdAndCode(Integer circuitId, String code);
}
