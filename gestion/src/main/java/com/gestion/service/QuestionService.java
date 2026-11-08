package com.gestion.service;

import com.gestion.exception.RessourceIntrouvableException;
import com.gestion.model.OffreEmploi;
import com.gestion.model.Question;
import com.gestion.repository.OffreEmploiRepository;
import com.gestion.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final OffreEmploiRepository offreEmploiRepository;

    @Transactional(readOnly = true)
    public List<Question> listerParOffre(Integer idOffre) {
        return questionRepository.findByOffreIdOffreOrderByOrdreAscIdQuestionAsc(idOffre);
    }

    @Transactional(readOnly = true)
    public List<Question> listerToutes() {
        return questionRepository.findAllByOrderByOffreIdOffreAscOrdreAscIdQuestionAsc();
    }

    @Transactional
    public Question ajouter(Integer idOffre, String libelle, Integer ordre) {
        OffreEmploi offre = offreEmploiRepository.findById(idOffre)
                .orElseThrow(() -> new RessourceIntrouvableException("Offre introuvable : " + idOffre));

        if (libelle == null || libelle.isBlank()) {
            throw new IllegalStateException("Le libelle de la question est obligatoire.");
        }

        Question question = Question.builder()
                .offre(offre)
                .libelle(libelle.trim())
                .ordre(ordre == null ? 1 : ordre)
                .build();
        return questionRepository.save(question);
    }

    @Transactional
    public void supprimer(Integer idQuestion) {
        Question question = questionRepository.findById(idQuestion)
                .orElseThrow(() -> new RessourceIntrouvableException("Question introuvable : " + idQuestion));
        questionRepository.delete(question);
    }
}
