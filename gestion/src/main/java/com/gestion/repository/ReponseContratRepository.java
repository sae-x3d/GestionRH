package com.gestion.repository;

import com.gestion.model.ReponseContrat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReponseContratRepository extends JpaRepository<ReponseContrat, Integer> {

    List<ReponseContrat> findByCandidatureIdCandidatureOrderByNumeroTourAscIdReponseContratAsc(
            Integer idCandidature);

    List<ReponseContrat> findByCandidatureIdCandidatureAndNumeroTourOrderByIdReponseContratAsc(
            Integer idCandidature, Integer numeroTour);

    @Query("select coalesce(max(r.numeroTour), 0) from ReponseContrat r "
            + "where r.candidature.idCandidature = :idCandidature")
    int trouverDernierTour(@Param("idCandidature") Integer idCandidature);
}
