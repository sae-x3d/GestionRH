package com.sirh.backend.modules.recrutement.repository;

import java.util.List;
import java.util.Optional;
import java.util.Collection;
import com.sirh.backend.modules.recrutement.entity.DemandeRecrutementEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface DemandeRecrutementRepository extends JpaRepository<DemandeRecrutementEntity, Integer> {
    List<DemandeRecrutementEntity> findAllByDepartementIdOrderByCreatedAtDesc(Integer departementId);
    Optional<DemandeRecrutementEntity> findByIdAndDepartementId(Integer id, Integer departementId);
    List<DemandeRecrutementEntity> findAllByStatutIdOrderByCreatedAtDesc(Integer statutId);
    List<DemandeRecrutementEntity> findAllByStatutIdInOrderByCreatedAtDesc(Collection<Integer> statutIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from DemandeRecrutementEntity d where d.id = :id")
    Optional<DemandeRecrutementEntity> findByIdForUpdate(@Param("id") Integer id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from DemandeRecrutementEntity d where d.id = :id and d.departementId = :departementId")
    Optional<DemandeRecrutementEntity> findByIdAndDepartementIdForUpdate(
        @Param("id") Integer id, @Param("departementId") Integer departementId);
}
