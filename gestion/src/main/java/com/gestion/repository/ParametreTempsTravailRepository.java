package com.gestion.repository;

import com.gestion.model.ParametreTempsTravail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ParametreTempsTravailRepository extends JpaRepository<ParametreTempsTravail, Integer> {

    Optional<ParametreTempsTravail> findFirstByActifTrueOrderByIdParametreAsc();
}
