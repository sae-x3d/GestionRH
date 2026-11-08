package com.gestion.repository;

import com.gestion.model.Contrat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ContratRepository extends JpaRepository<Contrat, Integer> {

    Optional<Contrat> findByCandidatureIdCandidature(Integer idCandidature);

    List<Contrat> findByCandidatureIdCandidatureOrderByDateGenerationDesc(Integer idCandidature);

    Optional<Contrat> findFirstByCandidatureIdCandidatureOrderByDateGenerationDesc(Integer idCandidature);

    List<Contrat> findByCandidatureUtilisateurIdUserOrderByDateGenerationDesc(Integer idUser);

    List<Contrat> findByEmployeIdEmpOrderByDateGenerationDesc(Integer idEmp);

    long countByCandidatureIdCandidature(Integer idCandidature);

    List<Contrat> findByStatutOrderByDateGenerationDesc(String statut);

    List<Contrat> findByStatutAndDateFinEssaiBefore(String statut, LocalDate date);

    @Query("select count(c) from Contrat c where c.statut in :statuts")
    long compterParStatuts(@Param("statuts") List<String> statuts);

    @Query("select c from Contrat c where c.statut in :statuts order by c.dateGeneration desc")
    List<Contrat> trouverParStatuts(@Param("statuts") List<String> statuts);

    List<Contrat> findByStatutInAndDateFinBetweenAndAlerteFinEnvoyeeFalse(
            List<String> statuts, LocalDate debut, LocalDate fin);

    List<Contrat> findByStatutInAndDateFinEssaiBetweenAndAlerteEssaiEnvoyeeFalse(
            List<String> statuts, LocalDate debut, LocalDate fin);
}
