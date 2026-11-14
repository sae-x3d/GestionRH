package com.sirh.backend.modules.recrutement.repository;

import java.util.Optional;
import com.sirh.backend.modules.recrutement.entity.StatutOffreEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StatutOffreRepository extends JpaRepository<StatutOffreEntity, Integer> {
    Optional<StatutOffreEntity> findByCode(String code);
}
