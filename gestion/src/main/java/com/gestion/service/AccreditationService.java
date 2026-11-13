package com.gestion.service;

import com.gestion.dto.AccreditationForm;
import com.gestion.enums.RoleValidation;
import com.gestion.enums.StatutDossierValidation;
import com.gestion.enums.TypeObjetValidation;
import com.gestion.event.DecisionValidationEvent;
import com.gestion.exception.AccesRefuseException;
import com.gestion.exception.RessourceIntrouvableException;
import com.gestion.model.Accreditation;
import com.gestion.model.Candidature;
import com.gestion.model.Utilisateur;
import com.gestion.repository.AccreditationRepository;
import com.gestion.repository.CandidatureRepository;
import com.gestion.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccreditationService {

    private static final String EN_VALIDATION_MANAGER = "EN_VALIDATION_MANAGER";
    private static final String EN_VALIDATION_RH = "EN_VALIDATION_RH";
    private static final String EN_VALIDATION_DIRECTION = "EN_VALIDATION_DIRECTION";
    private static final String VALIDEE = "VALIDEE";
    private static final String A_CORRIGER = "A_CORRIGER";

    private final AccreditationRepository accreditationRepository;
    private final CandidatureRepository candidatureRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final ValidationWorkflowService workflowService;
    private final StockageDocumentService stockageDocumentService;

    @Transactional
    public Accreditation soumettre(AccreditationForm form, MultipartFile document, Utilisateur createur) {
        if (createur == null) {
            throw new AccesRefuseException("Vous devez etre connecte.");
        }
        Utilisateur titulaire = resoudreTitulaire(form.getIdUtilisateur(), createur);
        Candidature candidature = resoudreCandidature(form.getIdCandidature(), titulaire);

        if (texteOuNull(form.getTypeAccreditation()) == null
                || texteOuNull(form.getOrganisme()) == null
                || form.getDateObtention() == null) {
            throw new IllegalStateException(
                    "Le type, l'organisme et la date d'obtention de l'accreditation sont obligatoires.");
        }
        if (form.getDateExpiration() != null && form.getDateExpiration().isBefore(form.getDateObtention())) {
            throw new IllegalStateException(
                    "La date d'expiration doit etre posterieure ou egale a la date d'obtention.");
        }

        String nomDocument = stockageDocumentService.enregistrer(
                document, "ACC", titulaire.getIdUser());
        Accreditation accreditation = Accreditation.builder()
                .utilisateur(titulaire)
                .candidature(candidature)
                .typeAccreditation(form.getTypeAccreditation().trim())
                .organisme(form.getOrganisme().trim())
                .numero(texteOuNull(form.getNumero()))
                .dateObtention(form.getDateObtention())
                .dateExpiration(form.getDateExpiration())
                .statut(EN_VALIDATION_MANAGER)
                .documentNomOriginal(document.getOriginalFilename())
                .documentChemin(nomDocument)
                .commentaire(texteOuNull(form.getCommentaire()))
                .dateCreation(LocalDateTime.now())
                .build();
        Accreditation enregistree = accreditationRepository.save(accreditation);

        Integer departementId = departementDe(candidature, titulaire);
        workflowService.ouvrir(TypeObjetValidation.ACCREDITATION, enregistree.getIdAccreditation(),
                departementId, createur, "/accreditations/" + enregistree.getIdAccreditation(),
                "accreditation");
        return enregistree;
    }

    @Transactional(readOnly = true)
    public List<Accreditation> mesAccreditations(Integer idUser) {
        return accreditationRepository.findByUtilisateurIdUserOrderByDateObtentionDesc(idUser);
    }

    @Transactional(readOnly = true)
    public List<Accreditation> listerPourGestionnaire(Utilisateur utilisateur) {
        if (workflowService.estAdministrateur(utilisateur) || workflowService.estRh(utilisateur)) {
            return accreditationRepository.findAllByOrderByDateCreationDesc();
        }
        if (!workflowService.estManager(utilisateur)
                || utilisateur.getEmploye() == null
                || utilisateur.getEmploye().getDepartement() == null) {
            throw new AccesRefuseException("Vous n'avez pas acces aux accreditations.");
        }
        Integer departementId = utilisateur.getEmploye().getDepartement().getIdDept();
        return accreditationRepository.findAllByOrderByDateCreationDesc().stream()
                .filter(accreditation -> departementId.equals(departementDe(
                        accreditation.getCandidature(), accreditation.getUtilisateur())))
                .toList();
    }

    @Transactional(readOnly = true)
    public Accreditation trouver(Integer id, Utilisateur utilisateur) {
        Accreditation accreditation = accreditationRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Accreditation introuvable."));
        if (!peutVoir(accreditation, utilisateur)) {
            throw new AccesRefuseException("Vous n'avez pas acces a cette accreditation.");
        }
        return accreditation;
    }

    @Transactional(readOnly = true)
    public boolean peutVoir(Accreditation accreditation, Utilisateur utilisateur) {
        if (accreditation == null || utilisateur == null) {
            return false;
        }
        if (accreditation.getUtilisateur() != null
                && accreditation.getUtilisateur().getIdUser().equals(utilisateur.getIdUser())) {
            return true;
        }
        if (workflowService.estAdministrateur(utilisateur) || workflowService.estRh(utilisateur)) {
            return true;
        }
        if (!workflowService.estManager(utilisateur)
                || utilisateur.getEmploye() == null
                || utilisateur.getEmploye().getDepartement() == null) {
            return false;
        }
        Integer departementId = utilisateur.getEmploye().getDepartement().getIdDept();
        return departementId.equals(departementDe(
                accreditation.getCandidature(), accreditation.getUtilisateur()));
    }

    @Transactional(readOnly = true)
    public boolean aAccreditationValide(Integer idUser, LocalDate date) {
        return !accreditationRepository.trouverValides(idUser, date).isEmpty();
    }

    @Transactional(readOnly = true)
    public boolean aAccreditationValidePourCandidature(Integer idCandidature, LocalDate date) {
        return !accreditationRepository
                .trouverValidesParCandidature(idCandidature, date).isEmpty();
    }

    @Transactional(readOnly = true)
    public List<Accreditation> validesPourCandidature(Integer idCandidature, LocalDate date) {
        return accreditationRepository.trouverValidesParCandidature(idCandidature, date);
    }

    @Transactional(readOnly = true)
    public String nomDocument(Accreditation accreditation) {
        return accreditation.getDocumentNomOriginal() == null
                ? accreditation.getDocumentChemin() : accreditation.getDocumentNomOriginal();
    }

    public Path cheminDocument(Accreditation accreditation) {
        return stockageDocumentService.chemin(accreditation.getDocumentChemin());
    }

    @EventListener
    @Transactional
    public void surDecision(DecisionValidationEvent evenement) {
        if (evenement.typeObjet() != TypeObjetValidation.ACCREDITATION) {
            return;
        }
        Accreditation accreditation = accreditationRepository.findById(evenement.idObjet()).orElse(null);
        if (accreditation == null) {
            return;
        }
        if (evenement.statut() == StatutDossierValidation.REFUSE) {
            accreditation.setStatut(A_CORRIGER);
            accreditation.setCommentaire(commentaire(evenement.commentaire(), accreditation.getCommentaire()));
            return;
        }
        if (evenement.etapeValidee() == RoleValidation.MANAGER) {
            accreditation.setStatut(EN_VALIDATION_RH);
            return;
        }
        if (evenement.etapeValidee() == RoleValidation.RH) {
            accreditation.setStatut(EN_VALIDATION_DIRECTION);
            accreditation.setVerificateur(evenement.decideur());
            accreditation.setDateVerification(LocalDateTime.now());
            return;
        }
        if (evenement.statut() == StatutDossierValidation.APPROUVE) {
            accreditation.setStatut(VALIDEE);
            accreditation.setVerificateur(evenement.decideur());
            accreditation.setDateVerification(LocalDateTime.now());
        }
    }

    private Utilisateur resoudreTitulaire(Integer idUtilisateur, Utilisateur createur) {
        if (idUtilisateur == null || idUtilisateur.equals(createur.getIdUser())) {
            return createur;
        }
        if (!workflowService.estAdministrateur(createur) && !workflowService.estRh(createur)) {
            throw new AccesRefuseException(
                    "Vous ne pouvez pas deposer une accreditation pour un autre utilisateur.");
        }
        return utilisateurRepository.findById(idUtilisateur)
                .orElseThrow(() -> new RessourceIntrouvableException("Utilisateur introuvable."));
    }

    private Candidature resoudreCandidature(Integer idCandidature, Utilisateur titulaire) {
        if (idCandidature == null) {
            return null;
        }
        Candidature candidature = candidatureRepository.findByIdAvecDetails(idCandidature)
                .orElseThrow(() -> new RessourceIntrouvableException("Candidature introuvable."));
        if (candidature.getUtilisateur() == null
                || !candidature.getUtilisateur().getIdUser().equals(titulaire.getIdUser())) {
            throw new AccesRefuseException("Cette candidature n'appartient pas au titulaire.");
        }
        return candidature;
    }

    private Integer departementDe(Candidature candidature, Utilisateur titulaire) {
        if (candidature != null && candidature.getOffre() != null
                && candidature.getOffre().getDemande() != null
                && candidature.getOffre().getDemande().getDepartement() != null) {
            return candidature.getOffre().getDemande().getDepartement().getIdDept();
        }
        if (titulaire != null && titulaire.getEmploye() != null
                && titulaire.getEmploye().getDepartement() != null) {
            return titulaire.getEmploye().getDepartement().getIdDept();
        }
        return null;
    }

    private String commentaire(String nouveau, String ancien) {
        return nouveau == null || nouveau.isBlank() ? ancien : nouveau.trim();
    }

    private String texteOuNull(String valeur) {
        return valeur == null || valeur.isBlank() ? null : valeur.trim();
    }
}
