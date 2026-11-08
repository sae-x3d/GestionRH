package com.gestion.repository;

import com.gestion.enums.StatutPlanning;
import com.gestion.model.Planning;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PlanningRepository extends JpaRepository<Planning, Integer> {

    Optional<Planning> findByEmployeIdEmpAndDateDebut(Integer idEmp, LocalDate dateDebut);

    List<Planning> findByEmployeIdEmpAndDateDebutBetweenOrderByDateDebutAsc(Integer idEmp,
                                                                           LocalDate debut,
                                                                           LocalDate fin);

    List<Planning> findByDateDebutAndStatutOrderByEmployeNomEmpAsc(LocalDate dateDebut,
                                                                   StatutPlanning statut);

    List<Planning> findByDateDebutBetweenOrderByDateDebutAscEmployeNomEmpAsc(LocalDate debut,
                                                                            LocalDate fin);

    List<Planning> findByEmployeDepartementIdDeptAndDateDebutOrderByEmployeNomEmpAsc(
            Integer idDept, LocalDate dateDebut);
}
