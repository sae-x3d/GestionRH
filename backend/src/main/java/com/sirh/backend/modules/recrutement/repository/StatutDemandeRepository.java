package com.sirh.backend.modules.recrutement.repository;

import java.util.Optional;
import com.sirh.backend.modules.recrutement.entity.StatutDemandeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StatutDemandeRepository extends JpaRepository<StatutDemandeEntity, Integer> {
    Optional<StatutDemandeEntity> findByCode(String code);
}
