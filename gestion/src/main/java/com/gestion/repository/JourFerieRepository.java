package com.gestion.repository;

import com.gestion.model.JourFerie;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface JourFerieRepository extends JpaRepository<JourFerie, Integer> {

    List<JourFerie> findByDateJourFerieBetween(LocalDate debut, LocalDate fin);

    List<JourFerie> findAllByOrderByDateJourFerieAsc();

    boolean existsByDateJourFerie(LocalDate date);
}
