package com.gestion.service;

import com.gestion.dto.CreateDemandeDTO;
import com.gestion.exception.RessourceIntrouvableException;
import com.gestion.model.Demande;
import com.gestion.model.DemandeStatus;
import com.gestion.model.Departement;
import com.gestion.model.Employe;
import com.gestion.model.Motif;
import com.gestion.model.Poste;
import com.gestion.model.Utilisateur;
import com.gestion.repository.DemandeRepository;
import com.gestion.repository.DemandeStatusRepository;
import com.gestion.repository.DepartementRepository;
import com.gestion.repository.EmployeRepository;
import com.gestion.repository.MotifRepository;
import com.gestion.repository.PosteRepository;
import com.gestion.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DemandeService {

    private final DemandeRepository demandeRepository;
    private final PosteRepository posteRepository;
    private final DepartementRepository departementRepository;
    private final MotifRepository motifRepository;
    private final EmployeRepository employeRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final DemandeStatusService demandeStatusService;

    @Transactional
    public Demande creer(CreateDemandeDTO dto, Utilisateur demandeur) {
        Demande demande = Demande.builder()
                .poste(trouverReference(posteRepository.findById(dto.getPosteId()), "poste"))
                .departement(trouverReference(departementRepository.findById(dto.getDepartementId()), "departement"))
                .motif(trouverReference(motifRepository.findById(dto.getMotifId()), "motif"))
                .demandeur(trouverReference(utilisateurRepository.findById(demandeur.getIdUser()), "demandeur"))
                .demandeStatus(trouverStatutInitial())
                .empRemplace(dto.getEmpRemplaceId() == null
                        ? null
                        : trouverReference(employeRepository.findById(dto.getEmpRemplaceId()), "employe remplace"))
                .budget(dto.getBudget())
                .dateSouhaite(dto.getDateSouhaite())
                .dateCreation(LocalDateTime.now())
                .build();

        return demandeRepository.save(demande);
    }

    private DemandeStatus trouverStatutInitial() {
        return demandeStatusService.trouverStatutInitial()
                .orElseThrow(() -> new RessourceIntrouvableException(
                        "Statut initial introuvable : \"" + demandeStatusService.getLibelleStatutInitial() + "\""));
    }

    private <T> T trouverReference(Optional<T> ressource, String nom) {
        return ressource.orElseThrow(() -> new RessourceIntrouvableException("Reference introuvable : " + nom));
    }
}
