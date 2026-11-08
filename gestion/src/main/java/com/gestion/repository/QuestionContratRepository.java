package com.gestion.repository;

import com.gestion.model.QuestionContrat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionContratRepository extends JpaRepository<QuestionContrat, Integer> {

    List<QuestionContrat> findByActifTrueOrderByOrdreAscIdQuestionContratAsc();

    List<QuestionContrat> findAllByOrderByOrdreAscIdQuestionContratAsc();
}
