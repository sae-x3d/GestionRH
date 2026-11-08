package com.gestion.repository;

import com.gestion.model.ValidationEtape;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ValidationEtapeRepository extends JpaRepository<ValidationEtape, Integer> {

    List<ValidationEtape> findByDossierIdDossierOrderByOrdreAsc(Integer idDossier);
}
