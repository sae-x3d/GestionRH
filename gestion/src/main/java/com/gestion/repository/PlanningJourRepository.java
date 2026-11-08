package com.gestion.repository;

import com.gestion.model.PlanningJour;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PlanningJourRepository extends JpaRepository<PlanningJour, Integer> {

    List<PlanningJour> findByPlanningIdPlanningOrderByDateJourAsc(Integer idPlanning);

    Optional<PlanningJour> findByPlanningEmployeIdEmpAndDateJour(Integer idEmp, LocalDate dateJour);

    void deleteByPlanningIdPlanning(Integer idPlanning);

    long countByPlanningEmployeDepartementIdDeptAndDateJourAndTypeJour(
            Integer idDept, LocalDate dateJour, com.gestion.enums.TypeJourPlanning typeJour);
}
