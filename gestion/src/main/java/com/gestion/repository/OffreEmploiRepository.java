package com.gestion.repository;

import com.gestion.enums.StatutOffre;
import com.gestion.model.OffreEmploi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OffreEmploiRepository extends JpaRepository<OffreEmploi, Integer> {

    List<OffreEmploi> findByStatutOrderByDatePublicationDesc(StatutOffre statut);

    List<OffreEmploi> findAllByOrderByDateCreationDesc();

    boolean existsByDemandeIdDemande(Integer idDemande);

    Optional<OffreEmploi> findByDemandeIdDemande(Integer idDemande);
}
