package com.gestion.repository;

import com.gestion.model.Motif;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MotifRepository extends JpaRepository<Motif, Integer> {

    List<Motif> findAllByOrderByLibelleAsc();
}
