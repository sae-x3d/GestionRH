package com.gestion.repository;
import com.gestion.model.Contrat;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface ContratRepository extends JpaRepository<Contrat, Integer> {
    Optional<Contrat> findByCandidatureIdCandidature(Integer idCandidature);
    List<Contrat> findByCandidatureUtilisateurIdUserOrderByDateGenerationDesc(Integer idUser);
}
