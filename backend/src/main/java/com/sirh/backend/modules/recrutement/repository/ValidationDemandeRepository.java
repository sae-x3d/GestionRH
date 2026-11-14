package com.sirh.backend.modules.recrutement.repository;

import com.sirh.backend.modules.recrutement.entity.ValidationDemandeEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ValidationDemandeRepository extends JpaRepository<ValidationDemandeEntity, Integer> {
    boolean existsByDemandeId(Integer demandeId);
    boolean existsByDemandeIdAndEtapeCircuitId(Integer demandeId, Integer etapeCircuitId);
    List<ValidationDemandeEntity> findAllByDemandeIdOrderByDecideeAtAsc(Integer demandeId);
}
