package com.gestion.repository;

import com.gestion.model.Entretien;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EntretienRepository extends JpaRepository<Entretien, Integer> {

    List<Entretien> findByCandidatureIdCandidatureOrderByDateHeureAsc(Integer idCandidature);

    long countByCandidatureIdCandidatureAndResultatIsNotNull(Integer idCandidature);
}
