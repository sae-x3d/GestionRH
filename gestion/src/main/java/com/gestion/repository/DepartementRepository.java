package com.gestion.repository;

import com.gestion.model.Departement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.List;

@Repository
public interface DepartementRepository extends JpaRepository<Departement, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM Departement d WHERE d.idDept = :idDept")
    Departement verrouillerPourReservation(@Param("idDept") Integer idDept);

    List<Departement> findAllByOrderByNomDeptAsc();
}
