package com.gestion.service;

import com.gestion.enums.StatutPaie;
import com.gestion.exception.RessourceIntrouvableException;
import com.gestion.model.Employe;
import com.gestion.model.InscriptionPaie;
import com.gestion.repository.EmployeRepository;
import com.gestion.repository.InscriptionPaieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InscriptionPaieService {

    private final InscriptionPaieRepository inscriptionRepository;
    private final EmployeRepository employeRepository;

    @Transactional
    public InscriptionPaie inscrire(Integer idEmp, String numeroPaie, String organisme,
                                    LocalDate dateDebut, String commentaire) {
        Employe employe = employeRepository.findById(idEmp)
                .orElseThrow(() -> new RessourceIntrouvableException("Employe introuvable : " + idEmp));
        InscriptionPaie inscription = inscriptionRepository.findByEmployeIdEmp(idEmp)
                .orElseGet(() -> InscriptionPaie.builder()
                        .employe(employe)
                        .dateInscription(LocalDateTime.now())
                        .statut(StatutPaie.EN_ATTENTE)
                        .build());
        if (numeroPaie != null && !numeroPaie.isBlank()) {
            inscription.setNumeroPaie(numeroPaie.trim());
        }
        inscription.setOrganisme(texteOuNull(organisme));
        inscription.setDateDebut(dateDebut);
        inscription.setCommentaire(texteOuNull(commentaire));
        inscription.setStatut(StatutPaie.INSCRIT);
        return inscriptionRepository.save(inscription);
    }

    @Transactional
    public void exclure(Integer idInscription) {
        InscriptionPaie inscription = inscriptionRepository.findById(idInscription)
                .orElseThrow(() -> new RessourceIntrouvableException(
                        "Inscription paie introuvable : " + idInscription));
        inscription.setStatut(StatutPaie.NON_INSCRIT);
        inscriptionRepository.save(inscription);
    }

    @Transactional(readOnly = true)
    public List<InscriptionPaie> lister() {
        return inscriptionRepository.findAllByOrderByDateInscriptionDesc();
    }

    @Transactional(readOnly = true)
    public InscriptionPaie trouverParEmploye(Integer idEmp) {
        return inscriptionRepository.findByEmployeIdEmp(idEmp).orElse(null);
    }

    private String texteOuNull(String valeur) {
        return valeur == null || valeur.isBlank() ? null : valeur.trim();
    }
}
