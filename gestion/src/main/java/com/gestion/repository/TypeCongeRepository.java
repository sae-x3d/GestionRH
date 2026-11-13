package com.gestion.repository;

import com.gestion.model.TypeConge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TypeCongeRepository extends JpaRepository<TypeConge, Integer> {

    List<TypeConge> findByActifTrueOrderByLibelleAsc();

    List<TypeConge> findAllByOrderByLibelleAsc();

    Optional<TypeConge> findByCodeIgnoreCase(String code);
}
