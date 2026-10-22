package com.gestion.repository;

import com.gestion.model.CandidatureStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CandidatureStatusRepository extends JpaRepository<CandidatureStatus, Integer> {

    Optional<CandidatureStatus> findByLibelleIgnoreCase(String libelle);

    List<CandidatureStatus> findAllByOrderByLibelleAsc();
}
