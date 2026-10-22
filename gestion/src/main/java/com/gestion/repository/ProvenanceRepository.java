package com.gestion.repository;

import com.gestion.model.Provenance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProvenanceRepository extends JpaRepository<Provenance, Integer> {

    List<Provenance> findAllByOrderByLibelleAsc();
}
