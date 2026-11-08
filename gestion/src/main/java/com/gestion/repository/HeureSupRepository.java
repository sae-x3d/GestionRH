package com.gestion.repository;

import com.gestion.enums.StatutHeureSup;
import com.gestion.model.HeureSup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface HeureSupRepository extends JpaRepository<HeureSup, Integer> {

    List<HeureSup> findByEmployeIdEmpAndDateHeureSupBetweenOrderByDateHeureSupAsc(
            Integer idEmp, LocalDate debut, LocalDate fin);

    List<HeureSup> findByStatutOrderByDateHeureSupAsc(StatutHeureSup statut);

    List<HeureSup> findByStatutAndPeriodePaieIsNullOrderByDateHeureSupAsc(StatutHeureSup statut);

    List<HeureSup> findByPeriodePaieIdPeriodePaieOrderByEmployeNomEmpAsc(Integer idPeriodePaie);

    @Query("""
            SELECT COALESCE(SUM(h.minutes), 0) FROM HeureSup h
            WHERE h.employe.idEmp = :idEmp
              AND h.dateHeureSup BETWEEN :debut AND :fin
              AND h.statut <> com.gestion.enums.StatutHeureSup.REFUSEE
            """)
    long totalMinutes(@Param("idEmp") Integer idEmp,
                      @Param("debut") LocalDate debut,
                      @Param("fin") LocalDate fin);
}
