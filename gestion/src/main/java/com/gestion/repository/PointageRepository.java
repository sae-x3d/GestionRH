package com.gestion.repository;

import com.gestion.model.Pointage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PointageRepository extends JpaRepository<Pointage, Integer> {

    Optional<Pointage> findByEmployeIdEmpAndDatePointage(Integer idEmp, LocalDate datePointage);

    List<Pointage> findByEmployeIdEmpAndDatePointageBetweenOrderByDatePointageAsc(
            Integer idEmp, LocalDate debut, LocalDate fin);

    List<Pointage> findByDatePointageBetweenOrderByEmployeNomEmpAsc(LocalDate debut, LocalDate fin);

    List<Pointage> findByEmployeDepartementIdDeptAndDatePointageBetweenOrderByDatePointageAscEmployeNomEmpAsc(
            Integer idDept, LocalDate debut, LocalDate fin);
}
