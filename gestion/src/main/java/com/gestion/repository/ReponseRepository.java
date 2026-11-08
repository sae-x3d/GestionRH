package com.gestion.repository;

import com.gestion.model.Reponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReponseRepository extends JpaRepository<Reponse, Integer> {

    List<Reponse> findByCandidatureIdCandidature(Integer idCandidature);
}
