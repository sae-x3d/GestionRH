package com.gestion.service;

import com.gestion.enums.RoleValidation;
import com.gestion.enums.StatutDossierValidation;
import com.gestion.enums.StatutValidation;
import com.gestion.enums.TypeObjetValidation;
import com.gestion.event.DecisionValidationEvent;
import com.gestion.exception.AccesRefuseException;
import com.gestion.exception.RessourceIntrouvableException;
import com.gestion.model.Utilisateur;
import com.gestion.model.ValidationDossier;
import com.gestion.model.ValidationEtape;
import com.gestion.repository.UtilisateurRepository;
import com.gestion.repository.ValidationDossierRepository;
import com.gestion.repository.ValidationEtapeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ValidationWorkflowService {

    private static final String LIEN_DEFAUT = "/validations/bloc4";
    private static final List<String> ROLES_MANAGER = List.of("MANAGER", "ROLE_MANAGER");
    private static final List<String> ROLES_RH = List.of("RH", "ROLE_RH");
    private static final List<String> ROLES_ADMIN = List.of("ADMINISTRATEUR", "ROLE_ADMINISTRATEUR");

    private final ValidationDossierRepository dossierRepository;
    private final ValidationEtapeRepository etapeRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final NotificationService notificationService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public ValidationDossier ouvrir(TypeObjetValidation typeObjet, Integer idObjet,
                                    Integer departementId, Utilisateur createur, String lien,
                                    String libelle) {
        if (typeObjet == null || idObjet == null) {
            throw new IllegalStateException("Le type et l'identifiant du dossier sont obligatoires.");
        }
        ValidationDossier dernier = dossierRepository
                .findFirstByTypeObjetAndIdObjetOrderByNumeroTentativeDesc(typeObjet, idObjet)
                .orElse(null);
        if (dernier != null && dernier.getStatut() == StatutDossierValidation.EN_COURS) {
            return dernier;
        }

        int tentative = dernier == null || dernier.getNumeroTentative() == null
                ? 1 : dernier.getNumeroTentative() + 1;
        ValidationDossier dossier = ValidationDossier.builder()
                .typeObjet(typeObjet)
                .idObjet(idObjet)
                .numeroTentative(tentative)
                .departementId(departementId)
                .statut(StatutDossierValidation.EN_COURS)
                .etapeCourante(1)
                .createur(createur)
                .lien(lien == null || lien.isBlank() ? LIEN_DEFAUT : lien.trim())
                .dateCreation(LocalDateTime.now())
                .build();

        dossier.getEtapes().add(etape(dossier, 1, RoleValidation.MANAGER));
        dossier.getEtapes().add(etape(dossier, 2, RoleValidation.RH));
        dossier.getEtapes().add(etape(dossier, 3, RoleValidation.DIRECTION));

        ValidationDossier enregistre = dossierRepository.save(dossier);
        notifierEtape(enregistre, libelle);
        return enregistre;
    }

    @Transactional
    public ValidationDossier approuver(Integer idDossier, Utilisateur decideur, String commentaire) {
        ValidationDossier dossier = charger(idDossier);
        verifierDossierEnCours(dossier);
        ValidationEtape etape = etapeCourante(dossier);
        verifierDecision(dossier, etape, decideur);

        etape.setStatut(StatutValidation.APPROUVE);
        etape.setDecideur(decideur);
        etape.setDateDecision(LocalDateTime.now());
        etape.setCommentaire(texteOuNull(commentaire));

        if (dossier.getEtapeCourante() < 3) {
            RoleValidation etapeValidee = etape.getRoleRequis();
            dossier.setEtapeCourante(dossier.getEtapeCourante() + 1);
            ValidationDossier enregistre = dossierRepository.save(dossier);
            notifierEtape(enregistre, libelle(enregistre.getTypeObjet()));
            eventPublisher.publishEvent(new DecisionValidationEvent(
                    enregistre.getTypeObjet(), enregistre.getIdObjet(),
                    StatutDossierValidation.EN_COURS, etapeValidee, decideur,
                    texteOuNull(commentaire), enregistre.getLien()));
            return enregistre;
        }

        dossier.setStatut(StatutDossierValidation.APPROUVE);
        dossier.setDateCloture(LocalDateTime.now());
        ValidationDossier enregistre = dossierRepository.save(dossier);
        notifierCreateur(enregistre, true, commentaire);
        eventPublisher.publishEvent(new DecisionValidationEvent(
                enregistre.getTypeObjet(), enregistre.getIdObjet(),
                StatutDossierValidation.APPROUVE, etape.getRoleRequis(), decideur,
                texteOuNull(commentaire), enregistre.getLien()));
        return enregistre;
    }

    @Transactional
    public ValidationDossier refuser(Integer idDossier, Utilisateur decideur, String commentaire) {
        ValidationDossier dossier = charger(idDossier);
        verifierDossierEnCours(dossier);
        ValidationEtape etape = etapeCourante(dossier);
        verifierDecision(dossier, etape, decideur);

        etape.setStatut(StatutValidation.REFUSE);
        etape.setDecideur(decideur);
        etape.setDateDecision(LocalDateTime.now());
        etape.setCommentaire(texteOuNull(commentaire));

        dossier.setStatut(StatutDossierValidation.REFUSE);
        dossier.setDateCloture(LocalDateTime.now());
        ValidationDossier enregistre = dossierRepository.save(dossier);
        notifierCreateur(enregistre, false, commentaire);
        eventPublisher.publishEvent(new DecisionValidationEvent(
                enregistre.getTypeObjet(), enregistre.getIdObjet(),
                StatutDossierValidation.REFUSE, etape.getRoleRequis(), decideur,
                texteOuNull(commentaire), enregistre.getLien()));
        return enregistre;
    }

    @Transactional(readOnly = true)
    public List<ValidationDossier> dossiersPour(Utilisateur utilisateur) {
        return dossierRepository.findByStatutOrderByDateCreationDesc(StatutDossierValidation.EN_COURS)
                .stream()
                .filter(dossier -> peutDecider(dossier, utilisateur))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ValidationDossier> dossiersParType(TypeObjetValidation typeObjet) {
        return dossierRepository.findByTypeObjetOrderByDateCreationDesc(typeObjet);
    }

    @Transactional(readOnly = true)
    public List<ValidationDossier> historique(TypeObjetValidation typeObjet, Integer idObjet) {
        return dossierRepository.findByTypeObjetAndIdObjetOrderByNumeroTentativeDesc(typeObjet, idObjet);
    }

    @Transactional(readOnly = true)
    public ValidationDossier dernierDossier(TypeObjetValidation typeObjet, Integer idObjet) {
        return dossierRepository.findFirstByTypeObjetAndIdObjetOrderByNumeroTentativeDesc(typeObjet, idObjet)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public boolean estEnAttente(TypeObjetValidation typeObjet, Integer idObjet) {
        ValidationDossier dossier = dernierDossier(typeObjet, idObjet);
        return dossier != null && dossier.getStatut() == StatutDossierValidation.EN_COURS;
    }

    @Transactional(readOnly = true)
    public ValidationDossier charger(Integer idDossier) {
        return dossierRepository.findByIdDossier(idDossier)
                .orElseThrow(() -> new RessourceIntrouvableException("Dossier de validation introuvable."));
    }

    @Transactional(readOnly = true)
    public RoleValidation roleAttendu(ValidationDossier dossier) {
        if (dossier == null || dossier.getStatut() != StatutDossierValidation.EN_COURS) {
            return null;
        }
        return etapeCourante(dossier).getRoleRequis();
    }

    public boolean peutDecider(ValidationDossier dossier, Utilisateur utilisateur) {
        if (dossier == null || utilisateur == null
                || dossier.getStatut() != StatutDossierValidation.EN_COURS) {
            return false;
        }
        ValidationEtape etape = etapeCourante(dossier);
        if (etape == null || etape.getStatut() != StatutValidation.EN_ATTENTE) {
            return false;
        }
        return switch (etape.getRoleRequis()) {
            case MANAGER -> estManagerDuDepartement(utilisateur, dossier.getDepartementId())
                    || estAdministrateur(utilisateur) && !aUnManager(dossier.getDepartementId());
            case RH -> aRole(utilisateur, ROLES_RH);
            case DIRECTION -> estAdministrateur(utilisateur);
        };
    }

    public String libelle(TypeObjetValidation typeObjet) {
        return typeObjet == null ? "dossier" : typeObjet.getLibelle().toLowerCase(Locale.ROOT);
    }

    public boolean estAdministrateur(Utilisateur utilisateur) {
        return aRole(utilisateur, ROLES_ADMIN);
    }

    public boolean estRh(Utilisateur utilisateur) {
        return aRole(utilisateur, ROLES_RH);
    }

    public boolean estManager(Utilisateur utilisateur) {
        return aRole(utilisateur, ROLES_MANAGER);
    }

    private ValidationEtape etape(ValidationDossier dossier, int ordre, RoleValidation role) {
        return ValidationEtape.builder()
                .dossier(dossier)
                .ordre(ordre)
                .roleRequis(role)
                .statut(StatutValidation.EN_ATTENTE)
                .build();
    }

    private ValidationEtape etapeCourante(ValidationDossier dossier) {
        if (dossier.getEtapes() == null || dossier.getEtapes().isEmpty()) {
            List<ValidationEtape> etapes = etapeRepository
                    .findByDossierIdDossierOrderByOrdreAsc(dossier.getIdDossier());
            dossier.setEtapes(etapes);
        }
        return dossier.getEtapes().stream()
                .filter(etape -> etape.getOrdre().equals(dossier.getEtapeCourante()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Etape de validation introuvable."));
    }

    private void verifierDossierEnCours(ValidationDossier dossier) {
        if (dossier.getStatut() != StatutDossierValidation.EN_COURS) {
            throw new IllegalStateException("Ce dossier de validation n'est plus en cours.");
        }
    }

    private void verifierDecision(ValidationDossier dossier, ValidationEtape etape, Utilisateur decideur) {
        if (decideur == null) {
            throw new AccesRefuseException("Vous devez etre connecte pour valider ce dossier.");
        }
        if (etape.getStatut() != StatutValidation.EN_ATTENTE) {
            throw new IllegalStateException("Cette etape de validation a deja ete traitee.");
        }
        if (!peutDecider(dossier, decideur)) {
            throw new AccesRefuseException("Vous n'avez pas le role requis pour cette etape de validation.");
        }
    }

    private void notifierEtape(ValidationDossier dossier, String libelle) {
        if (dossier == null || dossier.getStatut() != StatutDossierValidation.EN_COURS) {
            return;
        }
        RoleValidation role = roleAttendu(dossier);
        List<Utilisateur> destinataires = switch (role) {
            case MANAGER -> {
                List<Utilisateur> managers = dossier.getDepartementId() == null
                        ? List.of()
                        : utilisateurRepository.trouverManagersParDepartement(dossier.getDepartementId());
                yield managers.isEmpty() ? utilisateurRepository.trouverParRoles(ROLES_ADMIN) : managers;
            }
            case RH -> utilisateurRepository.trouverParRoles(ROLES_RH);
            case DIRECTION -> utilisateurRepository.trouverParRoles(ROLES_ADMIN);
        };
        String message = "Le dossier " + libelle + " n°" + dossier.getIdObjet()
                + " attend la validation " + role.getLibelle() + ".";
        for (Utilisateur destinataire : destinataires) {
            notificationService.envoyer(destinataire, "VALIDATION",
                    message, dossier.getLien());
        }
    }

    private void notifierCreateur(ValidationDossier dossier, boolean approuve, String commentaire) {
        if (dossier.getCreateur() == null) {
            return;
        }
        String message = "Votre dossier " + libelle(dossier.getTypeObjet()) + " n°"
                + dossier.getIdObjet() + " a ete " + (approuve ? "approuve" : "refuse") + "."
                + (commentaire == null || commentaire.isBlank() ? "" : " Motif : " + commentaire.trim());
        notificationService.envoyer(dossier.getCreateur(), "VALIDATION", message, dossier.getLien());
    }

    private boolean estManagerDuDepartement(Utilisateur utilisateur, Integer departementId) {
        if (departementId == null || utilisateur.getEmploye() == null
                || utilisateur.getEmploye().getDepartement() == null
                || !departementId.equals(utilisateur.getEmploye().getDepartement().getIdDept())) {
            return false;
        }
        return aRole(utilisateur, ROLES_MANAGER);
    }

    private boolean aUnManager(Integer departementId) {
        return departementId != null && utilisateurRepository.existsManagerPourDepartement(departementId);
    }

    private boolean aRole(Utilisateur utilisateur, List<String> roles) {
        if (utilisateur == null || utilisateur.getRole() == null
                || utilisateur.getRole().getNomRole() == null) {
            return false;
        }
        String role = utilisateur.getRole().getNomRole().trim().toUpperCase(Locale.ROOT);
        return roles.contains(role);
    }

    private String texteOuNull(String valeur) {
        return valeur == null || valeur.isBlank() ? null : valeur.trim();
    }
}
