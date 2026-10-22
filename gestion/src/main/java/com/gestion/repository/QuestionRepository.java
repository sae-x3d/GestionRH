package com.gestion.repository;

import com.gestion.model.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Integer> {

    List<Question> findByOffreIdOffreOrderByOrdreAscIdQuestionAsc(Integer idOffre);

    List<Question> findAllByOrderByOffreIdOffreAscOrdreAscIdQuestionAsc();

    long countByOffreIdOffre(Integer idOffre);
}
