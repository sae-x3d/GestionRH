package com.gestion.repository;

import com.gestion.model.RegimeTravail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RegimeTravailRepository extends JpaRepository<RegimeTravail, Integer> {

    List<RegimeTravail> findByActifTrueOrderByLibelleAsc();

    List<RegimeTravail> findAllByOrderByLibelleAsc();
}
