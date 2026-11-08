package com.gestion.repository;

import com.gestion.model.Employe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.List;

@Repository
public interface EmployeRepository extends JpaRepository<Employe, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Employe e WHERE e.idEmp = :idEmp")
    Employe verrouillerPourDemande(@Param("idEmp") Integer idEmp);

    List<Employe> findByDateDepartIsNullOrderByNomEmpAsc();

    java.util.Optional<Employe> findByMatricule(String matricule);

    java.util.Optional<Employe> findByCandidatureIdCandidature(Integer idCandidature);

    boolean existsByMatricule(String matricule);

    long countByDateDepartIsNull();

    long countByDepartementIdDeptAndDateDepartIsNull(Integer idDept);

    long countByPosteIdPosteAndDateDepartIsNull(Integer idPoste);
}
