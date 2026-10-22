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
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DemandeService {

    public static final String STATUT_ATTENTE_RH = "EN_ATTENTE_VALIDATION_RH";
    public static final String STATUT_ATTENTE_BUDGET = "EN_ATTENTE_VALIDATION_BUDGET";
    public static final String STATUT_ATTENTE_DIRECTION = "EN_ATTENTE_VALIDATION_DIRECTION";
    public static final String STATUT_APPROUVE = "APPROUVE";
    public static final String STATUT_REFUSE = "REFUSE";

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

    @Transactional(readOnly = true)
    public List<Demande> listerParStatut(String libelleStatut) {
        return demandeRepository.findByDemandeStatusLibelleStatusOrderByDateCreationAsc(libelleStatut);
    }

    @Transactional
    public Demande validerRh(Integer idDemande) {
        return transitionner(idDemande, STATUT_ATTENTE_RH, STATUT_ATTENTE_BUDGET);
    }

    @Transactional
    public Demande refuserRh(Integer idDemande) {
        return transitionner(idDemande, STATUT_ATTENTE_RH, STATUT_REFUSE);
    }

    @Transactional
    public Demande validerFinance(Integer idDemande) {
        return transitionner(idDemande, STATUT_ATTENTE_BUDGET, STATUT_ATTENTE_DIRECTION);
    }

    @Transactional
    public Demande refuserFinance(Integer idDemande) {
        return transitionner(idDemande, STATUT_ATTENTE_BUDGET, STATUT_REFUSE);
    }

    @Transactional
    public Demande validerDirection(Integer idDemande) {
        return transitionner(idDemande, STATUT_ATTENTE_DIRECTION, STATUT_APPROUVE);
    }

    @Transactional
    public Demande refuserDirection(Integer idDemande) {
        return transitionner(idDemande, STATUT_ATTENTE_DIRECTION, STATUT_REFUSE);
    }

    private Demande transitionner(Integer idDemande, String statutAttendu, String statutCible) {
        Demande demande = demandeRepository.findById(idDemande)
                .orElseThrow(() -> new RessourceIntrouvableException("Demande introuvable : " + idDemande));

        String statutActuel = demande.getDemandeStatus() == null
                ? ""
                : demande.getDemandeStatus().getLibelleStatus();

        if (statutActuel == null || !statutAttendu.equalsIgnoreCase(statutActuel)) {
            throw new IllegalStateException("La demande n'est pas au statut attendu : \""
                    + statutAttendu + "\" (statut actuel : \"" + statutActuel + "\").");
        }

        DemandeStatus cible = demandeStatusService.trouverParLibelle(statutCible)
                .orElseThrow(() -> new RessourceIntrouvableException(
                        "Statut de demande introuvable : \"" + statutCible + "\""));

        demande.setDemandeStatus(cible);
        demande.setDateReponse(LocalDateTime.now());
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
