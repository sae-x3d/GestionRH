package com.gestion.service;

import com.gestion.enums.RoleValidation;
import com.gestion.enums.StatutDossierValidation;
import com.gestion.enums.TypeObjetValidation;
import com.gestion.event.DecisionValidationEvent;
import com.gestion.exception.AccesRefuseException;
import com.gestion.exception.RessourceIntrouvableException;
import com.gestion.model.Affectation;
import com.gestion.model.Contrat;
import com.gestion.model.Demande;
import com.gestion.model.Document;
import com.gestion.model.Employe;
import com.gestion.model.Onboarding;
import com.gestion.model.OnboardingTache;
import com.gestion.model.Poste;
import com.gestion.model.Utilisateur;
import com.gestion.repository.AffectationRepository;
import com.gestion.repository.ContratRepository;
import com.gestion.repository.DocumentRepository;
import com.gestion.repository.EmployeRepository;
import com.gestion.repository.OnboardingRepository;
import com.gestion.repository.OnboardingTacheRepository;
import com.gestion.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OnboardingService {

    private static final String EN_COURS = "EN_COURS";
    private static final String EN_VALIDATION_MANAGER = "EN_VALIDATION_MANAGER";
    private static final String EN_VALIDATION_RH = "EN_VALIDATION_RH";
    private static final String EN_VALIDATION_DIRECTION = "EN_VALIDATION_DIRECTION";
    private static final String A_CORRIGER = "A_CORRIGER";
    private static final String TERMINE = "TERMINE";
    private static final String TITULARISATION = "TITULARISATION";

    private final OnboardingRepository onboardingRepository;
    private final OnboardingTacheRepository tacheRepository;
    private final DocumentRepository documentRepository;
    private final AffectationRepository affectationRepository;
    private final ContratRepository contratRepository;
    private final EmployeRepository employeRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final ValidationWorkflowService workflowService;
    private final StockageDocumentService stockageDocumentService;
    private final NotificationService notificationService;
    private final ContratPdfService pdfService;

    @Transactional
    public Onboarding demarrer(Contrat contrat, Employe employe) {
        if (contrat == null || employe == null) {
            throw new IllegalStateException("Le contrat et l'employe sont requis pour demarrer l'onboarding.");
        }
        Onboarding existant = onboardingRepository.findByContratIdContrat(contrat.getIdContrat()).orElse(null);
        if (existant != null) {
            return existant;
        }
        Demande demande = demandeDe(contrat);
        Poste poste = employe.getPoste() != null ? employe.getPoste()
                : demande == null ? null : demande.getPoste();
        com.gestion.model.Departement departement = employe.getDepartement() != null ? employe.getDepartement()
                : demande == null ? null : demande.getDepartement();
        if (poste == null || departement == null) {
            throw new IllegalStateException(
                    "Le poste et le departement sont requis pour demarrer l'onboarding.");
        }
        if (employe.getPoste() == null || employe.getDepartement() == null) {
            employe.setPoste(poste);
            employe.setDepartement(departement);
            employeRepository.save(employe);
        }

        Affectation affectation = Affectation.builder()
                .employe(employe)
                .poste(poste)
                .departement(departement)
                .manager(managerDe(departement.getIdDept()))
                .dateDebut(contrat.getDateEffet())
                .statut("ACTIVE")
                .dateCreation(LocalDateTime.now())
                .build();
        affectation = affectationRepository.save(affectation);

        Onboarding onboarding = Onboarding.builder()
                .contrat(contrat)
                .employe(employe)
                .affectation(affectation)
                .statut(EN_COURS)
                .dateDebut(contrat.getDateEffet())
                .dateCreation(LocalDateTime.now())
                .build();
        ajouterTachesParDefaut(onboarding, contrat);
        Onboarding enregistre = onboardingRepository.save(onboarding);

        notificationService.envoyer(contrat.getCandidature().getUtilisateur(), "ONBOARDING",
                "Votre parcours d'integration est ouvert. Consultez les taches a realiser.",
                "/mon-onboarding");
        return enregistre;
    }

    @Transactional(readOnly = true)
    public Onboarding trouver(Integer id, Utilisateur utilisateur) {
        Onboarding onboarding = onboardingRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Onboarding introuvable."));
        if (!peutVoir(onboarding, utilisateur)) {
            throw new AccesRefuseException("Vous n'avez pas acces a cet onboarding.");
        }
        return onboarding;
    }

    @Transactional(readOnly = true)
    public List<Onboarding> mesOnboardings(Utilisateur utilisateur) {
        if (utilisateur == null || utilisateur.getEmploye() == null) {
            return List.of();
        }
        return onboardingRepository.findByEmployeIdEmpOrderByDateDebutDesc(
                utilisateur.getEmploye().getIdEmp());
    }

    @Transactional(readOnly = true)
    public List<Onboarding> lister(Utilisateur utilisateur) {
        if (workflowService.estAdministrateur(utilisateur) || workflowService.estRh(utilisateur)) {
            return onboardingRepository.findAll();
        }
        if (!workflowService.estManager(utilisateur)
                || utilisateur.getEmploye() == null
                || utilisateur.getEmploye().getDepartement() == null) {
            return mesOnboardings(utilisateur);
        }
        Integer departementId = utilisateur.getEmploye().getDepartement().getIdDept();
        return onboardingRepository.findAll().stream()
                .filter(onboarding -> departementId.equals(departement(onboarding)))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OnboardingTache> taches(Onboarding onboarding) {
        return tacheRepository.findByOnboardingIdOnboardingOrderByOrdreAsc(onboarding.getIdOnboarding());
    }

    @Transactional(readOnly = true)
    public List<Document> documents(Onboarding onboarding) {
        return documentRepository.findByOnboardingIdOnboardingOrderByDateUploadAsc(onboarding.getIdOnboarding());
    }

    @Transactional(readOnly = true)
    public Document document(Integer idDocument, Utilisateur utilisateur) {
        Document document = documentRepository.findById(idDocument)
                .orElseThrow(() -> new RessourceIntrouvableException("Document introuvable."));
        if (!peutVoir(document.getOnboarding(), utilisateur)) {
            throw new AccesRefuseException("Vous n'avez pas acces a ce document.");
        }
        return document;
    }

    public Path cheminDocument(Document document) {
        return stockageDocumentService.chemin(document.getChemin());
    }

    @Transactional
    public OnboardingTache cocherTache(Integer idTache, Utilisateur utilisateur,
                                       boolean fait, String commentaire) {
        OnboardingTache tache = tacheRepository.findById(idTache)
                .orElseThrow(() -> new RessourceIntrouvableException("Tache d'onboarding introuvable."));
        Onboarding onboarding = tache.getOnboarding();
        if (!peutVoir(onboarding, utilisateur)) {
            throw new AccesRefuseException("Vous n'avez pas acces a cette tache.");
        }
        if (!List.of(EN_COURS, A_CORRIGER).contains(onboarding.getStatut())) {
            throw new IllegalStateException("Cet onboarding n'est plus modifiable.");
        }
        tache.setFait(fait);
        tache.setDateRealisation(fait ? LocalDate.now() : null);
        tache.setCommentaire(texteOuNull(commentaire));
        return tacheRepository.save(tache);
    }

    @Transactional
    public Document ajouterDocument(Integer idOnboarding, String typeDocument,
                                    MultipartFile fichier, Utilisateur utilisateur) {
        Onboarding onboarding = trouver(idOnboarding, utilisateur);
        if (!List.of(EN_COURS, A_CORRIGER).contains(onboarding.getStatut())) {
            throw new IllegalStateException("Cet onboarding n'est plus modifiable.");
        }
        if (texteOuNull(typeDocument) == null) {
            throw new IllegalStateException("Le type de document est obligatoire.");
        }
        String nom = stockageDocumentService.enregistrer(
                fichier, "ONB", idOnboarding);
        Document document = Document.builder()
                .onboarding(onboarding)
                .typeDocument(typeDocument.trim())
                .nomOriginal(fichier.getOriginalFilename())
                .chemin(nom)
                .dateUpload(LocalDateTime.now())
                .valide(false)
                .commentaire(null)
                .build();
        return documentRepository.save(document);
    }

    @Transactional
    public Onboarding soumettreCloture(Integer idOnboarding, Utilisateur utilisateur,
                                       String decisionEssai, String commentaire) {
        Onboarding onboarding = trouver(idOnboarding, utilisateur);
        if (onboarding.getStatut() != EN_COURS && onboarding.getStatut() != A_CORRIGER) {
            throw new IllegalStateException("Cet onboarding ne peut pas etre soumis a validation.");
        }
        if (tacheRepository.countByOnboardingIdOnboardingAndObligatoireTrueAndFaitFalse(
                idOnboarding) > 0) {
            throw new IllegalStateException(
                    "Toutes les taches obligatoires doivent etre terminees avant la validation.");
        }
        if ("ESSAI".equals(onboarding.getContrat().getTypeContrat())
                && texteOuNull(decisionEssai) == null) {
            throw new IllegalStateException("La decision de fin d'essai est obligatoire.");
        }
        onboarding.setDecisionEssai(texteOuNull(decisionEssai));
        onboarding.setCommentaire(texteOuNull(commentaire));
        onboarding.setStatut(EN_VALIDATION_MANAGER);
        Onboarding enregistre = onboardingRepository.save(onboarding);

        workflowService.ouvrir(TypeObjetValidation.ONBOARDING, enregistre.getIdOnboarding(),
                departement(enregistre), utilisateur,
                "/onboarding/" + enregistre.getIdOnboarding(), "onboarding");
        return enregistre;
    }

    @Transactional
    public OnboardingTache validerTacheParRh(Integer idTache, Utilisateur utilisateur, boolean valide) {
        if (!workflowService.estRh(utilisateur) && !workflowService.estAdministrateur(utilisateur)) {
            throw new AccesRefuseException("Seul le RH peut valider les documents d'onboarding.");
        }
        OnboardingTache tache = tacheRepository.findById(idTache)
                .orElseThrow(() -> new RessourceIntrouvableException("Tache d'onboarding introuvable."));
        tache.setFait(valide);
        tache.setDateRealisation(valide ? LocalDate.now() : null);
        return tacheRepository.save(tache);
    }

    @EventListener
    @Transactional
    public void surDecision(DecisionValidationEvent evenement) {
        if (evenement.typeObjet() != TypeObjetValidation.ONBOARDING) {
            return;
        }
        Onboarding onboarding = onboardingRepository.findById(evenement.idObjet()).orElse(null);
        if (onboarding == null) {
            return;
        }
        if (evenement.statut() == StatutDossierValidation.REFUSE) {
            onboarding.setStatut(A_CORRIGER);
            onboarding.setCommentaire(commentaire(evenement.commentaire(), onboarding.getCommentaire()));
            return;
        }
        if (evenement.statut() == StatutDossierValidation.EN_COURS) {
            if (evenement.etapeValidee() == RoleValidation.MANAGER) {
                onboarding.setStatut(EN_VALIDATION_RH);
            } else if (evenement.etapeValidee() == RoleValidation.RH) {
                onboarding.setStatut(EN_VALIDATION_DIRECTION);
            }
            return;
        }
        if (evenement.statut() != StatutDossierValidation.APPROUVE) {
            return;
        }
        onboarding.setStatut(TERMINE);
        onboarding.setDateCloture(LocalDate.now());
        if (onboarding.getAffectation() != null) {
            onboarding.getAffectation().setStatut("ACTIVE");
        }
        if ("ESSAI".equals(onboarding.getContrat().getTypeContrat())
                && TITULARISATION.equalsIgnoreCase(onboarding.getDecisionEssai())) {
            convertirEssai(onboarding.getContrat());
        }
        notificationService.envoyer(onboarding.getContrat().getCandidature().getUtilisateur(),
                "ONBOARDING", "Votre parcours d'integration est termine.",
                "/mon-onboarding");
    }

    @Scheduled(cron = "0 0 6 * * *")
    @Transactional
    public void convertirEssaisEchus() {
        List<Contrat> contrats = contratRepository.findByStatutAndDateFinEssaiBefore(
                "ACCEPTE", LocalDate.now());
        for (Contrat contrat : contrats) {
            if ("ESSAI".equals(contrat.getTypeContrat())) {
                convertirEssai(contrat);
            }
        }
    }

    private void convertirEssai(Contrat ancien) {
        if (ancien == null || !"ESSAI".equals(ancien.getTypeContrat())
                || "CONVERTI".equals(ancien.getStatut())) {
            return;
        }
        LocalDate dateEffet = ancien.getDateFinEssai() == null
                ? LocalDate.now() : ancien.getDateFinEssai().plusDays(1);
        Contrat nouveau = Contrat.builder()
                .candidature(ancien.getCandidature())
                .employe(ancien.getEmploye())
                .contratPrecedent(ancien)
                .numeroContrat(numeroConversion(ancien))
                .typeContrat("CDI")
                .nomSalarie(ancien.getNomSalarie())
                .adresseSalarie(ancien.getAdresseSalarie())
                .nomEmployeur(ancien.getNomEmployeur())
                .adresseEmployeur(ancien.getAdresseEmployeur())
                .fonction(ancien.getFonction())
                .categorieProfessionnelle(ancien.getCategorieProfessionnelle())
                .indiceClassification(ancien.getIndiceClassification())
                .salaire(ancien.getSalaire())
                .dureeTravail(ancien.getDureeTravail())
                .lieuTravail(ancien.getLieuTravail())
                .dateEffet(dateEffet)
                .lieuSignature(ancien.getLieuSignature())
                .dateSignature(LocalDate.now())
                .nature(com.gestion.enums.NatureContrat.CONVERSION)
                .statut("ACCEPTE")
                .dateGeneration(LocalDateTime.now())
                .dateAcceptation(LocalDateTime.now())
                .build();
        nouveau.setPdf(pdfService.generer(nouveau));
        contratRepository.save(nouveau);

        ancien.setStatut("CONVERTI");
        contratRepository.save(ancien);
        notificationService.envoyer(ancien.getCandidature().getUtilisateur(), "CONTRAT",
                "Votre contrat d'essai est converti en CDI a compter du " + dateEffet + ".",
                "/contrats/" + nouveau.getIdContrat());
    }

    private void ajouterTachesParDefaut(Onboarding onboarding, Contrat contrat) {
        LocalDate depart = contrat.getDateEffet() == null ? LocalDate.now() : contrat.getDateEffet();
        int ordre = 1;
        ajouterTache(onboarding, "Signature du contrat de travail", ordre++, true,
                com.gestion.enums.ResponsableTache.RH, depart);
        ajouterTache(onboarding, "Remise du livret d'accueil", ordre++, true,
                com.gestion.enums.ResponsableTache.RH, depart.plusDays(1));
        ajouterTache(onboarding, "Presentation a l'equipe et au manager", ordre++, true,
                com.gestion.enums.ResponsableTache.MANAGER, depart);
        ajouterTache(onboarding, "Installation du poste de travail", ordre++, true,
                com.gestion.enums.ResponsableTache.IT, depart);
        ajouterTache(onboarding, "Formation securite et reglement interieur", ordre++, true,
                com.gestion.enums.ResponsableTache.SERVICES_GENERAUX, depart.plusDays(3));
        ajouterTache(onboarding, "Point de suivi de fin de premiere semaine", ordre++, false,
                com.gestion.enums.ResponsableTache.MANAGER, depart.plusDays(7));
        if ("ESSAI".equals(contrat.getTypeContrat())) {
            ajouterTache(onboarding, "Evaluation de fin de periode d'essai", ordre, true,
                    com.gestion.enums.ResponsableTache.MANAGER,
                    contrat.getDateFinEssai() == null ? depart.plusMonths(3) : contrat.getDateFinEssai());
        }
    }

    private void ajouterTache(Onboarding onboarding, String libelle, int ordre, boolean obligatoire,
                              com.gestion.enums.ResponsableTache responsable, LocalDate echeance) {
        onboarding.getTaches().add(OnboardingTache.builder()
                .onboarding(onboarding)
                .libelle(libelle)
                .ordre(ordre)
                .obligatoire(obligatoire)
                .responsable(responsable == null ? null : responsable.name())
                .dateEcheance(echeance)
                .fait(false)
                .dateCreation(LocalDateTime.now())
                .build());
    }

    private Demande demandeDe(Contrat contrat) {
        if (contrat == null || contrat.getCandidature() == null
                || contrat.getCandidature().getOffre() == null) {
            return null;
        }
        return contrat.getCandidature().getOffre().getDemande();
    }

    private Utilisateur managerDe(Integer departementId) {
        if (departementId == null) {
            return null;
        }
        List<Utilisateur> managers = utilisateurRepository.trouverManagersParDepartement(departementId);
        return managers.isEmpty() ? null : managers.get(0);
    }

    private Integer departement(Onboarding onboarding) {
        if (onboarding.getAffectation() != null
                && onboarding.getAffectation().getDepartement() != null) {
            return onboarding.getAffectation().getDepartement().getIdDept();
        }
        return null;
    }

    private boolean peutVoir(Onboarding onboarding, Utilisateur utilisateur) {
        if (onboarding == null || utilisateur == null) {
            return false;
        }
        if (onboarding.getEmploye() != null && utilisateur.getEmploye() != null
                && onboarding.getEmploye().getIdEmp().equals(utilisateur.getEmploye().getIdEmp())) {
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
        return utilisateur.getEmploye().getDepartement().getIdDept()
                .equals(departement(onboarding));
    }

    private String numeroConversion(Contrat ancien) {
        long tentative = contratRepository.countByCandidatureIdCandidature(
                ancien.getCandidature().getIdCandidature()) + 1;
        return "CTR-" + LocalDate.now().getYear() + "-"
                + String.format("%04d-%02d-CDI", ancien.getCandidature().getIdCandidature(), tentative);
    }

    private String commentaire(String nouveau, String ancien) {
        return nouveau == null || nouveau.isBlank() ? ancien : nouveau.trim();
    }

    private String texteOuNull(String valeur) {
        return valeur == null || valeur.isBlank() ? null : valeur.trim();
    }
}
