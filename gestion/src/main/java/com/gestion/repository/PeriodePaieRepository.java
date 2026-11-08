package com.gestion.repository;

import com.gestion.model.PeriodePaie;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PeriodePaieRepository extends JpaRepository<PeriodePaie, Integer> {

    Optional<PeriodePaie> findByAnneeAndMois(Integer annee, Integer mois);

    List<PeriodePaie> findAllByOrderByAnneeDescMoisDesc();

    List<PeriodePaie> findByStatutOrderByAnneeDescMoisDesc(
            com.gestion.enums.StatutPeriodePaie statut);
}
