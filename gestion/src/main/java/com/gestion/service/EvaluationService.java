package com.gestion.service;

import com.gestion.exception.RessourceIntrouvableException;
import com.gestion.model.Entretien;
import com.gestion.model.EvaluationCandidat;
import com.gestion.model.Utilisateur;
import com.gestion.repository.EntretienRepository;
import com.gestion.repository.EvaluationCandidatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EvaluationService {

    private final EvaluationCandidatRepository evaluationRepository;
    private final EntretienRepository entretienRepository;

    @Transactional
    public EvaluationCandidat ajouter(Integer idEntretien, Integer note, String grille,
                                      String commentaire, Utilisateur evaluateur) {
        if (note == null || note < 0 || note > 100) {
            throw new IllegalStateException("La note doit etre comprise entre 0 et 100.");
        }
        Entretien entretien = entretienRepository.findById(idEntretien)
                .orElseThrow(() -> new RessourceIntrouvableException(
                        "Entretien introuvable : " + idEntretien));
        EvaluationCandidat evaluation = EvaluationCandidat.builder()
                .entretien(entretien)
                .note(note)
                .grille(texteOuNull(grille))
                .commentaire(texteOuNull(commentaire))
                .evaluateur(evaluateur)
                .dateEvaluation(LocalDateTime.now())
                .build();
        return evaluationRepository.save(evaluation);
    }

    @Transactional(readOnly = true)
    public List<EvaluationCandidat> listerParEntretien(Integer idEntretien) {
        return evaluationRepository.findByEntretienIdEntretienOrderByDateEvaluationDesc(idEntretien);
    }

    @Transactional(readOnly = true)
    public List<EvaluationCandidat> listerParCandidature(Integer idCandidature) {
        return evaluationRepository.findByEntretienCandidatureIdCandidatureOrderByDateEvaluationDesc(idCandidature);
    }

    @Transactional(readOnly = true)
    public double moyenneParCandidature(Integer idCandidature) {
        List<EvaluationCandidat> evaluations = listerParCandidature(idCandidature);
        if (evaluations.isEmpty()) {
            return 0;
        }
        return evaluations.stream().mapToInt(EvaluationCandidat::getNote).average().orElse(0);
    }

    private String texteOuNull(String valeur) {
        return valeur == null || valeur.isBlank() ? null : valeur.trim();
    }
}
