package com.gestion.repository;

import com.gestion.model.DiffusionOffre;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DiffusionOffreRepository extends JpaRepository<DiffusionOffre, Integer> {

    List<DiffusionOffre> findByOffreIdOffreOrderByDateDiffusionDesc(Integer idOffre);
}
