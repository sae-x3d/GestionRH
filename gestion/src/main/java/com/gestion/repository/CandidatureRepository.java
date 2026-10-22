package com.gestion.repository;

import com.gestion.model.Candidature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CandidatureRepository extends JpaRepository<Candidature, Integer> {

    @Query("select distinct c from Candidature c " +
            "left join fetch c.offre " +
            "left join fetch c.statut " +
            "left join fetch c.utilisateur " +
            "left join fetch c.parcours p " +
            "left join fetch p.parcours " +
            "left join fetch p.niveau " +
            "where c.utilisateur.idUser = :idUser " +
            "order by c.dateCandidature desc")
    List<Candidature> findByUtilisateurIdUserOrderByDateCandidatureDesc(@Param("idUser") Integer idUser);

    @Query("select distinct c from Candidature c " +
            "left join fetch c.offre " +
            "left join fetch c.statut " +
            "left join fetch c.utilisateur " +
            "left join fetch c.parcours p " +
            "left join fetch p.parcours " +
            "left join fetch p.niveau " +
            "where c.offre.idOffre = :idOffre " +
            "order by c.dateCandidature desc")
    List<Candidature> findByOffreIdOffreOrderByDateCandidatureDesc(@Param("idOffre") Integer idOffre);

    boolean existsByUtilisateurIdUserAndOffreIdOffre(Integer idUser, Integer idOffre);
}

