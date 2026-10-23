package com.gestion.repository;

import com.gestion.model.PropositionEmbauche;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PropositionEmbaucheRepository extends JpaRepository<PropositionEmbauche, Integer> {

    @Query("""
            SELECT p FROM PropositionEmbauche p
            JOIN FETCH p.candidature c
            JOIN FETCH c.offre o
            JOIN FETCH p.candidat cand
            LEFT JOIN FETCH p.rh r
            WHERE p.candidature.idCandidature = :idCandidature
            """)
    Optional<PropositionEmbauche> findByCandidatureIdAvecDetails(@Param("idCandidature") Integer idCandidature);

    @Query("""
            SELECT p FROM PropositionEmbauche p
            JOIN FETCH p.candidature c
            JOIN FETCH c.offre o
            JOIN FETCH p.candidat cand
            LEFT JOIN FETCH p.rh r
            WHERE p.candidat.idUser = :idUser
            """)
    Optional<PropositionEmbauche> findByCandidatIdUserAvecDetails(@Param("idUser") Integer idUser);

    boolean existsByCandidatureIdCandidature(Integer idCandidature);
}
