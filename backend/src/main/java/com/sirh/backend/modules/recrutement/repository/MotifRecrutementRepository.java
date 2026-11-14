package com.sirh.backend.modules.recrutement.repository;

import java.util.List;
import com.sirh.backend.modules.recrutement.entity.MotifRecrutementEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MotifRecrutementRepository extends JpaRepository<MotifRecrutementEntity, Integer> {
    List<MotifRecrutementEntity> findAllByOrderByLibelleAsc();
}
