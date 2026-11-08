package com.gestion.repository;

import com.gestion.enums.StatutDossierValidation;
import com.gestion.enums.TypeObjetValidation;
import com.gestion.model.ValidationDossier;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ValidationDossierRepository extends JpaRepository<ValidationDossier, Integer> {

    @EntityGraph(attributePaths = {"etapes", "createur", "etapes.decideur"})
    Optional<ValidationDossier> findFirstByTypeObjetAndIdObjetOrderByNumeroTentativeDesc(
            TypeObjetValidation typeObjet, Integer idObjet);

    @EntityGraph(attributePaths = {"etapes", "createur", "etapes.decideur"})
    Optional<ValidationDossier> findByIdDossier(Integer idDossier);

    @EntityGraph(attributePaths = {"etapes", "createur", "etapes.decideur"})
    List<ValidationDossier> findByStatutOrderByDateCreationDesc(StatutDossierValidation statut);

    @EntityGraph(attributePaths = {"etapes", "createur", "etapes.decideur"})
    List<ValidationDossier> findByTypeObjetAndStatutOrderByDateCreationDesc(
            TypeObjetValidation typeObjet, StatutDossierValidation statut);

    @EntityGraph(attributePaths = {"etapes", "createur", "etapes.decideur"})
    List<ValidationDossier> findByTypeObjetAndIdObjetOrderByNumeroTentativeDesc(
            TypeObjetValidation typeObjet, Integer idObjet);

    @EntityGraph(attributePaths = {"etapes", "createur", "etapes.decideur"})
    List<ValidationDossier> findByTypeObjetOrderByDateCreationDesc(TypeObjetValidation typeObjet);
}
