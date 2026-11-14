package com.sirh.backend.modules.organisation.repository;

import java.util.List;
import com.sirh.backend.modules.organisation.entity.DepartementEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DepartementRepository extends JpaRepository<DepartementEntity, Integer> {
    @Query(value = """
        SELECT d.* FROM departements d
        JOIN postes p ON p.departement_id = d.id
        JOIN employes e ON e.poste_id = p.id
        WHERE e.user_id = :userId AND p.actif = TRUE AND d.actif = TRUE
        ORDER BY d.id
        """, nativeQuery = true)
    List<DepartementEntity> findActiveForEmployeeUser(@Param("userId") Integer userId);

    @Query(value = """
        SELECT d.* FROM departements d
        WHERE UPPER(d.code) = UPPER(:code) AND d.actif = TRUE
        """, nativeQuery = true)
    List<DepartementEntity> findActiveByCode(@Param("code") String code);
}
