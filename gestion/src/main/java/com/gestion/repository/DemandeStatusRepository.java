package com.gestion.repository;

import com.gestion.model.DemandeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DemandeStatusRepository extends JpaRepository<DemandeStatus, Integer> {

    Optional<DemandeStatus> findByLibelleStatusIgnoreCase(String libelleStatus);

    List<DemandeStatus> findAllByOrderByLibelleStatusAsc();
}
