package com.gestion.repository;

import com.gestion.model.EvaluationCandidat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EvaluationCandidatRepository extends JpaRepository<EvaluationCandidat, Integer> {

    List<EvaluationCandidat> findByEntretienIdEntretienOrderByDateEvaluationDesc(Integer idEntretien);

    List<EvaluationCandidat> findByEntretienCandidatureIdCandidatureOrderByDateEvaluationDesc(Integer idCandidature);
}
