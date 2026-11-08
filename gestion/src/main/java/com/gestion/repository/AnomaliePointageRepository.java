package com.gestion.repository;

import com.gestion.enums.StatutAnomalie;
import com.gestion.enums.TypeAnomaliePointage;
import com.gestion.model.AnomaliePointage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface AnomaliePointageRepository extends JpaRepository<AnomaliePointage, Integer> {

    List<AnomaliePointage> findByStatutOrderByDateAnomalieAsc(StatutAnomalie statut);

    List<AnomaliePointage> findByEmployeIdEmpAndDateAnomalieBetweenOrderByDateAnomalieAsc(
            Integer idEmp, LocalDate debut, LocalDate fin);

    List<AnomaliePointage> findByEmployeDepartementIdDeptAndDateAnomalieBetweenOrderByDateAnomalieAsc(
            Integer idDept, LocalDate debut, LocalDate fin);

    boolean existsByPointageIdPointageAndTypeAnomalie(Integer idPointage,
                                                     TypeAnomaliePointage typeAnomalie);

    boolean existsByEmployeIdEmpAndDateAnomalieAndTypeAnomalie(Integer idEmp, LocalDate date,
                                                               TypeAnomaliePointage typeAnomalie);
}
