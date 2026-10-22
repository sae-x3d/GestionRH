package com.gestion.repository;

import com.gestion.model.Demande;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DemandeRepository extends JpaRepository<Demande, Integer> {

    List<Demande> findByDemandeurIdUserOrderByDateCreationDesc(Integer idUser);

    @Query("""
            SELECT d FROM Demande d
            WHERE d.demandeStatus.idDemandeStatus = :idStatut
              AND NOT EXISTS (SELECT o FROM OffreEmploi o WHERE o.demande = d)
            ORDER BY d.dateCreation DESC
            """)
    List<Demande> findSansOffrePourStatut(@Param("idStatut") Integer idStatut);
}
