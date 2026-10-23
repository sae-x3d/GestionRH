package com.gestion.service;

import com.gestion.dto.CreateDemandeDTO;
import com.gestion.exception.AccesRefuseException;
import com.gestion.exception.DecisionRhImpossibleException;
import com.gestion.exception.RessourceIntrouvableException;
import com.gestion.model.Demande;
import com.gestion.model.DemandeStatus;
import com.gestion.model.Utilisateur;
import com.gestion.repository.DemandeRepository;
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
                .departement(trouverReference(departementRepository.findById(dto.getDepartementId()), "département"))
                .motif(trouverReference(motifRepository.findById(dto.getMotifId()), "motif"))
                .demandeur(trouverReference(utilisateurRepository.findById(demandeur.getIdUser()), "demandeur"))
                .demandeStatus(trouverStatutInitial())
                .empRemplace(dto.getEmpRemplaceId() == null
                        ? null
                        : trouverReference(employeRepository.findById(dto.getEmpRemplaceId()), "employé remplacé"))
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

    @Transactional(readOnly = true)
    public List<Demande> listerPour(Utilisateur utilisateur) {
        if (estAdminOuRh(utilisateur)) {
            return demandeRepository.findAllByOrderByDateCreationDesc();
        }
        if (estManager(utilisateur)) {
            Integer idDepartement = idDepartementUtilisateur(utilisateur);
            if (idDepartement != null) {
                return demandeRepository.findByDepartementIdDeptOrderByDateCreationDesc(idDepartement);
            }
        }
        if (utilisateur != null) {
            return demandeRepository.findByDemandeurIdUserOrderByDateCreationDesc(utilisateur.getIdUser());
        }
        return List.of();
    }

    public boolean peutCreer(Utilisateur utilisateur) {
        return estManager(utilisateur) || estAdmin(utilisateur);
    }

    public boolean peutFiltrerParDepartement(Utilisateur utilisateur) {
        return estAdminOuRh(utilisateur);
    }

    @Transactional(readOnly = true)
    public Demande trouverParId(Integer id) {
        return demandeRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Demande introuvable : " + id));
    }

    @Transactional(readOnly = true)
    public Demande trouverPourUtilisateur(Integer id, Utilisateur utilisateur) {
        Demande demande = demandeRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Demande introuvable : " + id));
        if (!peutConsulter(demande, utilisateur)) {
            throw new RessourceIntrouvableException("Demande introuvable : " + id);
        }
        return demande;
    }

    @Transactional(readOnly = true)
    public List<Demande> listerEnAttenteValidationRh() {
        DemandeStatus statut = demandeStatusService.trouverParLibelle(DemandeStatusService.EN_ATTENTE_VALIDATION_RH)
                .orElseThrow(() -> new RessourceIntrouvableException(
                        "Statut introuvable : « " + DemandeStatusService.EN_ATTENTE_VALIDATION_RH + " »"));
        return demandeRepository.findByDemandeStatusIdDemandeStatusOrderByDateCreationAsc(statut.getIdDemandeStatus());
    }

    @Transactional
    public Demande deciderRh(Integer id, Utilisateur utilisateur, boolean accepter) {
        if (!estRole(utilisateur, "RH")) {
            throw new AccesRefuseException("Seule la RH peut valider une demande.");
        }
        Demande demande = demandeRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Demande introuvable : " + id));
        if (demande.getDemandeStatus() == null
                || !DemandeStatusService.EN_ATTENTE_VALIDATION_RH.equalsIgnoreCase(
                        demande.getDemandeStatus().getLibelleStatus())) {
            throw new DecisionRhImpossibleException(
                    "La demande n° " + id + " n'est pas en attente de validation RH.");
        }
        String codeStatut = accepter
                ? DemandeStatusService.EN_ATTENTE_VALIDATION_BUDGET
                : DemandeStatusService.REFUSE;
        DemandeStatus statut = demandeStatusService.trouverParLibelle(codeStatut)
                .orElseThrow(() -> new RessourceIntrouvableException("Statut introuvable : « " + codeStatut + " »"));
        demande.setDemandeStatus(statut);
        demande.setDateReponse(LocalDateTime.now());
        return demandeRepository.save(demande);
    }

    public boolean peutConsulter(Demande demande, Utilisateur utilisateur) {
        if (estAdminOuRh(utilisateur)) {
            return true;
        }
        if (estFinance(utilisateur) && demande.getDemandeStatus() != null
                && STATUT_ATTENTE_BUDGET.equalsIgnoreCase(demande.getDemandeStatus().getLibelleStatus())) {
            return true;
        }
        if (estManager(utilisateur)) {
            Integer idDepartement = idDepartementUtilisateur(utilisateur);
            return idDepartement != null
                    && demande.getDepartement() != null
                    && idDepartement.equals(demande.getDepartement().getIdDept());
        }
        return utilisateur != null
                && demande.getDemandeur() != null
                && demande.getDemandeur().getIdUser().equals(utilisateur.getIdUser());
    }

    public boolean departementAutorise(Integer idDepartement, Utilisateur utilisateur) {
        if (estAdmin(utilisateur)) {
            return true;
        }
        if (!estManager(utilisateur)) {
            return false;
        }
        Integer idDepartementManager = idDepartementUtilisateur(utilisateur);
        return idDepartementManager != null && idDepartementManager.equals(idDepartement);
    }

    public Integer idDepartementUtilisateur(Utilisateur utilisateur) {
        if (utilisateur == null || utilisateur.getEmploye() == null || utilisateur.getEmploye().getDepartement() == null) {
            return null;
        }
        return utilisateur.getEmploye().getDepartement().getIdDept();
    }

    private boolean estAdminOuRh(Utilisateur utilisateur) {
        return estAdmin(utilisateur) || estRole(utilisateur, "RH");
    }

    private boolean estAdmin(Utilisateur utilisateur) {
        return estRole(utilisateur, "Administrateur");
    }

    private boolean estManager(Utilisateur utilisateur) {
        return estRole(utilisateur, "Manager");
    }

    private boolean estRole(Utilisateur utilisateur, String nomRole) {
        return utilisateur != null
                && utilisateur.getRole() != null
                && nomRole.equalsIgnoreCase(utilisateur.getRole().getNomRole());
    }

    @Transactional
    public Demande validerFinance(Integer idDemande, Utilisateur utilisateur) {
        verifierValidationFinance(idDemande, utilisateur);
        return transitionner(idDemande, STATUT_ATTENTE_BUDGET, STATUT_ATTENTE_DIRECTION);
    }

    @Transactional
    public Demande refuserFinance(Integer idDemande, Utilisateur utilisateur) {
        verifierValidationFinance(idDemande, utilisateur);
        return transitionner(idDemande, STATUT_ATTENTE_BUDGET, STATUT_REFUSE);
    }

    @Transactional
    public Demande validerDirection(Integer idDemande, Utilisateur utilisateur) {
        verifierValidationDirection(utilisateur);
        return transitionner(idDemande, STATUT_ATTENTE_DIRECTION, STATUT_APPROUVE);
    }

    @Transactional
    public Demande refuserDirection(Integer idDemande, Utilisateur utilisateur) {
        verifierValidationDirection(utilisateur);
        return transitionner(idDemande, STATUT_ATTENTE_DIRECTION, STATUT_REFUSE);
    }

    public boolean estFinance(Utilisateur utilisateur) {
        if (utilisateur == null) {
            return false;
        }
        if (estRole(utilisateur, "Finance")) {
            return true;
        }
        return utilisateur.getEmploye() != null
                && utilisateur.getEmploye().getDepartement() != null
                && "Finance".equalsIgnoreCase(utilisateur.getEmploye().getDepartement().getNomDept());
    }

    public boolean estAdministrateur(Utilisateur utilisateur) {
        return estAdmin(utilisateur);
    }

    private void verifierValidationFinance(Integer idDemande, Utilisateur utilisateur) {
        if (utilisateur == null) {
            throw new AccesRefuseException("Un utilisateur authentifie est requis pour valider un budget.");
        }
        if (estAdmin(utilisateur)) {
            return;
        }
        if (!estFinance(utilisateur)) {
            throw new AccesRefuseException("Seuls les profils Finance peuvent valider un budget.");
        }
        Demande demande = demandeRepository.findById(idDemande)
                .orElseThrow(() -> new RessourceIntrouvableException("Demande introuvable : " + idDemande));
        Integer idDepartementDemande = demande.getDepartement() == null
                ? null
                : demande.getDepartement().getIdDept();
        Integer idDepartementUtilisateur = idDepartementUtilisateur(utilisateur);
        if (idDepartementDemande != null && idDepartementDemande.equals(idDepartementUtilisateur)) {
            throw new AccesRefuseException(
                    "Un departement ne peut pas valider le budget de sa propre demande.");
        }
    }

    private void verifierValidationDirection(Utilisateur utilisateur) {
        if (!estAdmin(utilisateur)) {
            throw new AccesRefuseException("Seule la direction peut valider une demande.");
        }
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
                        "Statut initial introuvable : « " + demandeStatusService.getLibelleStatutInitial() + " »"));
    }

    private <T> T trouverReference(Optional<T> ressource, String nom) {
        return ressource.orElseThrow(() -> new RessourceIntrouvableException("Référence introuvable : " + nom));
    }
}
