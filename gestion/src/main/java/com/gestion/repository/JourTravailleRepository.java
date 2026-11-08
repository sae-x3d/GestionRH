package com.gestion.repository;

import com.gestion.model.JourTravaille;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface JourTravailleRepository extends JpaRepository<JourTravaille, Integer> {
    Optional<JourTravaille> findByEmployeIdEmpAndDateJour(Integer idEmp, LocalDate date);
    List<JourTravaille> findByEmployeIdEmpAndDateJourBetweenOrderByDateJourAsc(Integer idEmp, LocalDate debut, LocalDate fin);
}
