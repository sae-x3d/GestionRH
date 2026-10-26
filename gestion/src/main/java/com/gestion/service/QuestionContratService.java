package com.gestion.service;

import com.gestion.exception.RessourceIntrouvableException;
import com.gestion.model.QuestionContrat;
import com.gestion.repository.QuestionContratRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QuestionContratService {

    private final QuestionContratRepository questionContratRepository;

    @Transactional(readOnly = true)
    public List<QuestionContrat> listerActives() {
        return questionContratRepository.findByActifTrueOrderByOrdreAscIdQuestionContratAsc();
    }

    @Transactional(readOnly = true)
    public List<QuestionContrat> listerToutes() {
        return questionContratRepository.findAllByOrderByOrdreAscIdQuestionContratAsc();
    }

    @Transactional
    public QuestionContrat ajouter(String libelle, Integer ordre) {
        String texte = obligatoire(libelle, "Le libelle de la question est obligatoire.");
        int position = ordre == null || ordre < 1
                ? listerToutes().size() + 1
                : ordre;
        return questionContratRepository.save(QuestionContrat.builder()
                .libelle(texte)
                .ordre(position)
                .actif(true)
                .build());
    }

    @Transactional
    public QuestionContrat modifier(Integer id, String libelle, Integer ordre, boolean actif) {
        QuestionContrat question = trouver(id);
        question.setLibelle(obligatoire(libelle, "Le libelle de la question est obligatoire."));
        question.setOrdre(ordre == null || ordre < 1 ? question.getOrdre() : ordre);
        question.setActif(actif);
        return questionContratRepository.save(question);
    }

    @Transactional
    public void desactiver(Integer id) {
        QuestionContrat question = trouver(id);
        question.setActif(false);
        questionContratRepository.save(question);
    }

    @Transactional
    public void reactiver(Integer id) {
        QuestionContrat question = trouver(id);
        question.setActif(true);
        questionContratRepository.save(question);
    }

    private QuestionContrat trouver(Integer id) {
        return questionContratRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Question de contrat introuvable : " + id));
    }

    private String obligatoire(String valeur, String message) {
        if (valeur == null || valeur.isBlank()) {
            throw new IllegalStateException(message);
        }
        return valeur.trim();
    }
}
