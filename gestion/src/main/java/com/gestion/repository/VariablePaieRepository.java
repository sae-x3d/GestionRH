package com.gestion.repository;

import com.gestion.model.VariablePaie;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VariablePaieRepository extends JpaRepository<VariablePaie, Integer> {

    List<VariablePaie> findByPeriodePaieIdPeriodePaieOrderByEmployeNomEmpAscTypeVariableAsc(
            Integer idPeriodePaie);

    void deleteByPeriodePaieIdPeriodePaie(Integer idPeriodePaie);

    boolean existsByPeriodePaieIdPeriodePaieAndReference(Integer idPeriodePaie, String reference);
}
