package com.gestion.service;

import com.gestion.config.ParametresContrat;
import com.gestion.dto.CreerContratDTO;
import com.gestion.enums.RoleValidation;
import com.gestion.enums.StatutDossierValidation;
import com.gestion.enums.TypeObjetValidation;
import com.gestion.event.DecisionValidationEvent;
import com.gestion.model.Candidature;
import com.gestion.model.Contrat;
import com.gestion.model.Demande;
import com.gestion.model.Employe;
import com.gestion.model.OffreEmploi;
import com.gestion.model.Provenance;
import com.gestion.model.Utilisateur;
import com.gestion.repository.CandidatureRepository;
import com.gestion.repository.ContratRepository;
import com.gestion.repository.EmployeRepository;
import com.gestion.repository.ProvenanceRepository;
import com.gestion.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ContratService {

    private static final String STATUT_EN_ATTENTE = "EN_ATTENTE_ACCEPTATION";

    private final ContratRepository contratRepository;
    private final CandidatureRepository candidatureRepository;
    private final EmployeRepository employeRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final ProvenanceRepository provenanceRepository;
    private final ContratPdfService pdfService;
    private final ParametresContrat parametres;
    private final CandidatureStatusService candidatureStatusService;
    private final NotificationService notificationService;
    private final AccreditationService accreditationService;
    private final ValidationWorkflowService workflowService;
    private final OnboardingService onboardingService;
    private final EmployeService employeService;

    @Transactional(readOnly = true)
    public boolean peutVoir(Contrat contrat, Utilisateur utilisateur) {
        if (contrat == null || utilisateur == null) {
            return false;
        }
        if (contrat.getCandidature().getUtilisateur().getIdUser().equals(utilisateur.getIdUser())) {
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
                .equals(departementId(contrat.getCandidature()));
    }

    @Transactional(readOnly = true)
    public List<Contrat> contratsCandidat(Integer userId) {
        return contratRepository.findByCandidatureUtilisateurIdUserOrderByDateGenerationDesc(userId);
    }

    @Transactional(readOnly = true)
    public List<Contrat> contratsRh() {
        return contratRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Contrat trouver(Integer id) {
        return contratRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Contrat introuvable."));
    }

    @Transactional(readOnly = true)
    public Contrat contratCandidature(Integer id) {
        return contratRepository.findFirstByCandidatureIdCandidatureOrderByDateGenerationDesc(id)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public Contrat contratActifCandidature(Integer id) {
        Contrat contrat = contratCandidature(id);
        return contrat != null && estActif(contrat.getStatut()) ? contrat : null;
    }

    @Transactional(readOnly = true)
    public List<Contrat> historiqueCandidature(Integer id) {
        return contratRepository.findByCandidatureIdCandidatureOrderByDateGenerationDesc(id);
    }

    @Transactional(readOnly = true)
    public CreerContratDTO preparerFormulaire(Integer candidatureId) {
        return preparerFormulaire(chargerCandidature(candidatureId));
    }

    @Transactional
    public Contrat generer(Integer candidatureId, CreerContratDTO dto) {
        return generer(candidatureId, dto, null);
    }

    @Transactional
    public Contrat generer(Integer candidatureId, CreerContratDTO dto, Utilisateur createur) {
        Candidature candidature = chargerCandidature(candidatureId);
        if (contratActifCandidature(candidatureId) != null) {
            throw new IllegalStateException(
                    "Un contrat est deja en cours pour cette candidature. Consultez son historique.");
        }
        return creer(candidature, dto, createur);
    }

    @Transactional
    public Contrat genererAutomatiquement(Integer candidatureId, String nomSalarie,
                                          String adresseSalarie, LocalDate dateFin, String motifCdd) {
        Candidature candidature = chargerCandidature(candidatureId);
        Contrat existant = contratActifCandidature(candidatureId);
        if (existant != null) {
            return existant;
        }

        CreerContratDTO dto = preparerFormulaire(candidature);
        dto.setNomSalarie(premierNonVide(nomSalarie, dto.getNomSalarie()));
        dto.setAdresseSalarie(premierNonVide(adresseSalarie, dto.getAdresseSalarie()));
        dto.setDateFin(dateFin);
        dto.setMotifCdd(texteOuNull(motifCdd));
        return creer(candidature, dto, null);
    }

    @Transactional
    public Contrat renouveler(Integer contratId, String typeCible, LocalDate nouvelleFin,
                              Integer nouveauSalaire, String motif, Utilisateur createur) {
        Contrat ancien = trouver(contratId);
        if (!List.of("CDD", "ESSAI").contains(ancien.getTypeContrat())) {
            throw new IllegalStateException("Seul un CDD ou un contrat d'essai peut etre renouvele.");
        }
        LocalDate dateEffet = ancien.getDateFin() != null
                ? ancien.getDateFin().plusDays(1) : LocalDate.now();
        Integer salaire = nouveauSalaire != null && nouveauSalaire > 0
                ? nouveauSalaire : ancien.getSalaire();
        return creerSuite(ancien, typeCible, dateEffet, nouvelleFin, salaire, motif,
                com.gestion.enums.NatureContrat.RENOUVELLEMENT, createur);
    }

    @Transactional
    public Contrat convertirEnCdi(Integer contratId, Integer nouveauSalaire, Utilisateur createur) {
        Contrat ancien = trouver(contratId);
        LocalDate dateEffet = ancien.getDateFin() != null
                ? ancien.getDateFin().plusDays(1) : LocalDate.now();
        Integer salaire = nouveauSalaire != null && nouveauSalaire > 0
                ? nouveauSalaire : ancien.getSalaire();
        return creerSuite(ancien, "CDI", dateEffet, null, salaire, null,
                com.gestion.enums.NatureContrat.CONVERSION, createur);
    }

    @Transactional
    public Contrat avenant(Integer contratId, Integer nouveauSalaire, String nouveauLieu,
                           String commentaire, Utilisateur createur) {
        Contrat ancien = trouver(contratId);
        Integer salaire = nouveauSalaire != null && nouveauSalaire > 0
                ? nouveauSalaire : ancien.getSalaire();
        String lieu = nouveauLieu == null || nouveauLieu.isBlank()
                ? ancien.getLieuTravail() : nouveauLieu.trim();
        if (salaire.equals(ancien.getSalaire()) && lieu.equals(ancien.getLieuTravail())) {
            throw new IllegalStateException("Aucune modification a enregistrer dans l'avenant.");
        }
        Contrat nouveau = Contrat.builder()
                .candidature(ancien.getCandidature())
                .employe(ancien.getEmploye())
                .contratPrecedent(ancien)
                .numeroContrat(ancien.getNumeroContrat() + "-AV" + ((ancien.getNumeroAvenant() == null
                        ? 0 : ancien.getNumeroAvenant()) + 1))
                .nature(com.gestion.enums.NatureContrat.AVENANT)
                .numeroAvenant((ancien.getNumeroAvenant() == null ? 0 : ancien.getNumeroAvenant()) + 1)
                .typeContrat(ancien.getTypeContrat())
                .nomSalarie(ancien.getNomSalarie())
                .adresseSalarie(ancien.getAdresseSalarie())
                .nomEmployeur(ancien.getNomEmployeur())
                .adresseEmployeur(ancien.getAdresseEmployeur())
                .fonction(ancien.getFonction())
                .categorieProfessionnelle(ancien.getCategorieProfessionnelle())
                .indiceClassification(ancien.getIndiceClassification())
                .salaire(salaire)
                .dureeTravail(ancien.getDureeTravail())
                .lieuTravail(lieu)
                .dateEffet(LocalDate.now())
                .dateFin(ancien.getDateFin())
                .dateFinEssai(ancien.getDateFinEssai())
                .dureeEssaiMois(ancien.getDureeEssaiMois())
                .motifCdd(ancien.getMotifCdd())
                .lieuSignature(ancien.getLieuSignature())
                .dateSignature(LocalDate.now())
                .statut("ACCEPTE")
                .dateGeneration(LocalDateTime.now())
                .dateAcceptation(LocalDateTime.now())
                .commentaire(commentaire)
                .build();
        nouveau.setPdf(pdfService.generer(nouveau));
        Contrat enregistre = contratRepository.save(nouveau);
        ancien.setStatut("AVENANT");
        contratRepository.save(ancien);
        if (ancien.getCandidature() != null) {
            notificationService.envoyer(ancien.getCandidature().getUtilisateur(), "CONTRAT",
                    "Un avenant a votre contrat est disponible.", "/contrats/" + enregistre.getIdContrat());
        }
        return enregistre;
    }

    @Transactional(readOnly = true)
    public List<Contrat> contratsAEcheance(int jours) {
        LocalDate aujourdHui = LocalDate.now();
        return contratRepository.findByStatutInAndDateFinBetweenAndAlerteFinEnvoyeeFalse(
                List.of("ACCEPTE", "EN_COURS"), aujourdHui, aujourdHui.plusDays(jours));
    }

    private Contrat creerSuite(Contrat ancien, String typeCible, LocalDate dateEffet,
                               LocalDate dateFin, Integer salaire, String motif,
                               com.gestion.enums.NatureContrat nature, Utilisateur createur) {
        String type = typeCible == null || typeCible.isBlank()
                ? ancien.getTypeContrat() : typeCible.trim().toUpperCase();
        if (!List.of("CDI", "CDD", "ESSAI").contains(type)) {
            throw new IllegalStateException("Type de contrat cible invalide : " + typeCible);
        }
        if ("CDD".equals(type)) {
            if (dateFin == null) {
                throw new IllegalStateException("La date de fin du nouveau CDD est obligatoire.");
            }
            if (!dateFin.isAfter(dateEffet)) {
                throw new IllegalStateException("La date de fin doit etre apres la date d'effet.");
            }
        } else if (dateFin != null) {
            throw new IllegalStateException("Un " + type + " ne comporte pas de date de fin.");
        }
        Contrat nouveau = Contrat.builder()
                .candidature(ancien.getCandidature())
                .employe(ancien.getEmploye())
                .contratPrecedent(ancien)
                .numeroContrat(numeroContrat(ancien.getCandidature()))
                .nature(nature)
                .typeContrat(type)
                .nomSalarie(ancien.getNomSalarie())
                .adresseSalarie(ancien.getAdresseSalarie())
                .nomEmployeur(ancien.getNomEmployeur())
                .adresseEmployeur(ancien.getAdresseEmployeur())
                .fonction(ancien.getFonction())
                .categorieProfessionnelle(ancien.getCategorieProfessionnelle())
                .indiceClassification(ancien.getIndiceClassification())
                .salaire(salaire)
                .dureeTravail(ancien.getDureeTravail())
                .lieuTravail(ancien.getLieuTravail())
                .dateEffet(dateEffet)
                .dateFin("CDD".equals(type) ? dateFin : null)
                .dureeEssaiMois(null)
                .dateFinEssai(null)
                .motifCdd("CDD".equals(type) ? (motif == null ? ancien.getMotifCdd() : motif.trim()) : null)
                .lieuSignature(ancien.getLieuSignature())
                .dateSignature(LocalDate.now())
                .statut("ACCEPTE")
                .dateGeneration(LocalDateTime.now())
                .dateAcceptation(LocalDateTime.now())
                .build();
        nouveau.setPdf(pdfService.generer(nouveau));
        Contrat enregistre = contratRepository.save(nouveau);
        ancien.setStatut("RENOUVELE");
        contratRepository.save(ancien);
        if (ancien.getCandidature() != null) {
            notificationService.envoyer(ancien.getCandidature().getUtilisateur(), "CONTRAT",
                    "Votre contrat est renouvele a compter du " + dateEffet + ".",
                    "/contrats/" + enregistre.getIdContrat());
        }
        return enregistre;
    }

    @Transactional
    public Contrat repondre(Integer contratId, Integer userId, boolean accepte) {
        Contrat c = contratRepository.findById(contratId)
                .orElseThrow(() -> new IllegalArgumentException("Contrat introuvable."));
        if (!c.getCandidature().getUtilisateur().getIdUser().equals(userId)) {
            throw new IllegalStateException("Ce contrat ne vous appartient pas.");
        }
        if (!STATUT_EN_ATTENTE.equals(c.getStatut())) {
            throw new IllegalStateException("Ce contrat a deja ete traite.");
        }
        if (!accepte) {
            c.setStatut("REFUSE");
            Contrat refuse = contratRepository.save(c);
            Candidature candidature = c.getCandidature();
            candidatureStatusService.trouverParLibelle("CONTRAT_REFUSE").ifPresent(statut -> {
                candidature.setStatut(statut);
                candidature.setDateDecision(LocalDateTime.now());
                candidatureRepository.save(candidature);
            });
            for (Utilisateur gestionnaire : utilisateurRepository.trouverGestionnairesCandidatures()) {
                notificationService.envoyer(gestionnaire, "CONTRAT",
                        "Le candidat a refuse le contrat " + c.getNumeroContrat() + ".",
                        "/candidatures/rh/" + candidature.getIdCandidature());
            }
            return refuse;
        }
        Utilisateur user = utilisateurRepository.verrouillerParId(userId)
                .orElseThrow(() -> new IllegalStateException("Candidat introuvable."));
        Employe emp = user.getEmploye() != null ? user.getEmploye() : c.getEmploye();
        if (emp == null) {
            emp = creerEmployeDepuisContrat(c);
        }
        emp = employeService.preparerNouvelEmploye(emp, c.getCandidature(), user);
        if (user.getEmploye() == null) {
            user.setEmploye(emp);
            utilisateurRepository.save(user);
        }
        c.setEmploye(emp);
        c.setDateAcceptation(LocalDateTime.now());
        c.setStatut("ACCEPTE");
        if (c.getPdf() == null) {
            c.setPdf(pdfService.generer(c));
        }
        Contrat accepteContrat = contratRepository.save(c);
        employeService.finaliserEmbauche(emp, c.getCandidature(), user);
        onboardingService.demarrer(accepteContrat, emp);
        for (Utilisateur gestionnaire : utilisateurRepository.trouverGestionnairesCandidatures()) {
            notificationService.envoyer(gestionnaire, "CONTRAT",
                    "Le candidat a accepte le contrat " + c.getNumeroContrat() + ".",
                    "/contrats/" + c.getIdContrat());
        }
        return accepteContrat;
    }

    @EventListener
    @Transactional
    public void surDecision(DecisionValidationEvent evenement) {
        if (evenement.typeObjet() != TypeObjetValidation.CONTRAT) {
            return;
        }
        Contrat contrat = contratRepository.findById(evenement.idObjet()).orElse(null);
        if (contrat == null) {
            return;
        }
        if (evenement.statut() == StatutDossierValidation.REFUSE) {
            contrat.setStatut("A_CORRIGER");
            return;
        }
        if (evenement.statut() == StatutDossierValidation.EN_COURS) {
            if (evenement.etapeValidee() == RoleValidation.MANAGER) {
                contrat.setStatut("EN_VALIDATION_RH");
            } else if (evenement.etapeValidee() == RoleValidation.RH) {
                contrat.setStatut("EN_VALIDATION_DIRECTION");
            }
            return;
        }
        if (evenement.statut() != StatutDossierValidation.APPROUVE) {
            return;
        }
        contrat.setStatut(STATUT_EN_ATTENTE);
        contrat.setDateSignature(LocalDate.now());
        contrat.setPdf(pdfService.generer(contrat));
        notificationService.envoyer(contrat.getCandidature().getUtilisateur(), "CONTRAT",
                "Votre contrat " + contrat.getNumeroContrat()
                        + " est valide. Consultez-le et indiquez votre decision.",
                "/contrats/" + contrat.getIdContrat());
    }

    private Employe creerEmployeDepuisContrat(Contrat contrat) {
        OffreEmploi offre = contrat.getCandidature().getOffre();
        Demande demande = offre == null ? null : offre.getDemande();
        if (demande == null || demande.getPoste() == null || demande.getDepartement() == null) {
            throw new IllegalStateException(
                    "Le poste et le departement valides de la demande sont requis pour creer la fiche employe.");
        }
        if (contrat.getNomSalarie() == null || contrat.getNomSalarie().isBlank()
                || contrat.getDateEffet() == null || contrat.getSalaire() == null || contrat.getSalaire() <= 0) {
            throw new IllegalStateException(
                    "Le nom, la date d'embauche et le salaire du contrat doivent etre valides.");
        }
        Provenance provenance = provenanceRepository.findByLibelleIgnoreCase("Recrutement externe")
                .orElseThrow(() -> new IllegalStateException(
                        "La provenance 'Recrutement externe' manque dans la base."));

        return Employe.builder()
                .nomEmp(contrat.getNomSalarie().trim())
                .dateEmbauche(contrat.getDateEffet().atStartOfDay())
                .poste(demande.getPoste())
                .departement(demande.getDepartement())
                .salaireEmp(contrat.getSalaire())
                .provenance(provenance)
                .build();
    }

    private Contrat creer(Candidature candidature, CreerContratDTO dto, Utilisateur createur) {
        if (!"ACCEPTEE".equalsIgnoreCase(candidature.getStatut().getLibelle())) {
            throw new IllegalStateException(
                    "La candidature doit etre acceptee par RH avant l'emission du contrat.");
        }

        String typeDemande = normaliserType(dto.getTypeContrat());
        LocalDate aujourdHui = LocalDate.now();
        LocalDate dateEffet = dto.getDateEffet();
        if (dateEffet == null) {
            throw new IllegalStateException("La date d'effet du contrat est obligatoire.");
        }
        if (dateEffet.isBefore(aujourdHui)) {
            throw new IllegalStateException("La date d'effet ne peut pas etre dans le passe.");
        }

        String type = determinerType(candidature, typeDemande, dto, dateEffet);
        LocalDate dateFin = dto.getDateFin();
        if (dateFin != null && !dateFin.isAfter(dateEffet)) {
            throw new IllegalStateException("La date de fin doit etre apres la date d'effet.");
        }
        if ("CDD".equals(type)) {
            if (dateFin == null || texteOuNull(dto.getMotifCdd()) == null) {
                throw new IllegalStateException("Pour un CDD, le terme et son motif doivent etre precises.");
            }
            if (dateFin.isBefore(dateEffet.plusMonths(1)) || dateFin.isAfter(dateEffet.plusYears(2))) {
                throw new IllegalStateException("La duree d'un CDD doit etre comprise entre un mois et deux ans.");
            }
        } else if ("ESSAI".equals(type)) {
            if (dateFin != null) {
                throw new IllegalStateException("Un contrat d'essai ne comporte pas de date de fin de CDD.");
            }
        } else if (dateFin != null) {
            throw new IllegalStateException("Un CDI ne comporte pas de date de fin.");
        }

        OffreEmploi offre = candidature.getOffre();
        if (offre == null || texteOuNull(offre.getTitre()) == null) {
            throw new IllegalStateException("Le titre du poste de l'offre est manquant.");
        }
        Integer salaire = candidature.getSalaireConvenu() != null && candidature.getSalaireConvenu() > 0
                ? candidature.getSalaireConvenu()
                : offre.getSalaire();
        if (salaire == null || salaire <= 0) {
            throw new IllegalStateException("Le salaire convenu du candidat est manquant.");
        }

        String nomSalarie = obligatoire(dto.getNomSalarie(), "Le nom du salarie est obligatoire.");
        String adresseSalarie = obligatoire(dto.getAdresseSalarie(), "L'adresse du salarie est obligatoire.");
        String nomEmployeur = obligatoire(dto.getNomEmployeur(), "Le nom de l'employeur est obligatoire.");
        String adresseEmployeur = obligatoire(dto.getAdresseEmployeur(), "L'adresse de l'employeur est obligatoire.");
        String categorie = obligatoire(dto.getCategorieProfessionnelle(),
                "La categorie professionnelle est obligatoire.");
        String indice = obligatoire(dto.getIndiceClassification(),
                "L'indice de classification est obligatoire.");
        String duree = obligatoire(dto.getDureeTravail(), "La duree du travail est obligatoire.");
        String lieu = obligatoire(dto.getLieuTravail(), "Le lieu de travail est obligatoire.");

        Contrat contrat = Contrat.builder()
                .candidature(candidature)
                .numeroContrat(numeroContrat(candidature))
                .typeContrat(type)
                .nomSalarie(nomSalarie)
                .adresseSalarie(adresseSalarie)
                .nomEmployeur(nomEmployeur)
                .adresseEmployeur(adresseEmployeur)
                .fonction(offre.getTitre().trim())
                .categorieProfessionnelle(categorie)
                .indiceClassification(indice)
                .salaire(salaire)
                .dureeTravail(duree)
                .lieuTravail(lieu)
                .dateEffet(dateEffet)
                .dateFin("CDD".equals(type) ? dateFin : null)
                .dureeEssaiMois("ESSAI".equals(type) ? 3 : null)
                .dateFinEssai("ESSAI".equals(type) ? dateEffet.plusMonths(3) : null)
                .motifCdd("CDD".equals(type) ? texteOuNull(dto.getMotifCdd()) : null)
                .lieuSignature(parametres.getLieuSignature())
                .dateSignature(null)
                .nature(com.gestion.enums.NatureContrat.INITIAL)
                .statut("EN_VALIDATION_MANAGER")
                .dateGeneration(LocalDateTime.now())
                .build();
        Contrat enregistre = contratRepository.save(contrat);
        workflowService.ouvrir(TypeObjetValidation.CONTRAT, enregistre.getIdContrat(),
                departementId(candidature), createur,
                "/contrats/" + enregistre.getIdContrat(), "contrat");
        return enregistre;
    }

    private CreerContratDTO preparerFormulaire(Candidature candidature) {
        if (!"ACCEPTEE".equalsIgnoreCase(candidature.getStatut().getLibelle())) {
            throw new IllegalStateException(
                    "La candidature doit etre acceptee par RH avant l'emission du contrat.");
        }
        OffreEmploi offre = candidature.getOffre();
        Demande demande = offre == null ? null : offre.getDemande();

        CreerContratDTO dto = new CreerContratDTO();
        LocalDate dateEffet = dateEffetParDefaut(demande);
        dto.setDateEffet(dateEffet);
        dto.setTypeContrat(accreditationService.aAccreditationValidePourCandidature(
                candidature.getIdCandidature(), dateEffet) ? "CDI" : "ESSAI");
        dto.setNomSalarie(nomParDefaut(candidature));
        dto.setAdresseSalarie("");
        dto.setNomEmployeur(parametres.getEmployeurNom());
        dto.setAdresseEmployeur(parametres.getEmployeurAdresse());
        dto.setCategorieProfessionnelle(parametres.getCategorieProfessionnelle());
        dto.setIndiceClassification(parametres.getIndiceClassification());
        dto.setDureeTravail(parametres.getDureeTravail());
        dto.setLieuTravail(lieuTravail(offre, demande));
        return dto;
    }

    private Candidature chargerCandidature(Integer id) {
        return candidatureRepository.findByIdAvecDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("Candidature introuvable."));
    }

    private String numeroContrat(Candidature candidature) {
        long tentative = contratRepository.countByCandidatureIdCandidature(
                candidature.getIdCandidature()) + 1;
        return "CTR-" + LocalDate.now().getYear() + "-"
                + String.format("%04d-%02d", candidature.getIdCandidature(), tentative);
    }

    private String normaliserType(String type) {
        if (type == null || type.isBlank()) {
            return "AUTO";
        }
        String valeur = type.trim().toUpperCase();
        if (!List.of("CDI", "CDD", "ESSAI", "AUTO").contains(valeur)) {
            throw new IllegalStateException("Type de contrat invalide : " + type);
        }
        return valeur;
    }

    private String determinerType(Candidature candidature, String typeDemande,
                                  CreerContratDTO dto, LocalDate dateEffet) {
        boolean accredite = accreditationService.aAccreditationValidePourCandidature(
                candidature.getIdCandidature(), dateEffet)
                || (candidature.getUtilisateur() != null
                && accreditationService.aAccreditationValide(
                        candidature.getUtilisateur().getIdUser(), dateEffet));
        if (accredite) {
            return "CDI";
        }
        if ("CDD".equals(typeDemande)) {
            if (texteOuNull(dto.getMotifCdd()) == null) {
                throw new IllegalStateException("Le motif du CDD est obligatoire.");
            }
            return "CDD";
        }
        return "ESSAI";
    }

    private Integer departementId(Candidature candidature) {
        if (candidature == null || candidature.getOffre() == null
                || candidature.getOffre().getDemande() == null
                || candidature.getOffre().getDemande().getDepartement() == null) {
            return null;
        }
        return candidature.getOffre().getDemande().getDepartement().getIdDept();
    }

    private boolean estActif(String statut) {
        return statut != null && List.of(
                "BROUILLON", "EN_VALIDATION_MANAGER", "EN_VALIDATION_RH",
                "EN_VALIDATION_DIRECTION", "EN_ATTENTE_ACCEPTATION", "ACCEPTE",
                "EN_COURS").contains(statut);
    }

    private LocalDate dateEffetParDefaut(Demande demande) {
        LocalDate aujourdHui = LocalDate.now();
        if (demande != null && demande.getDateSouhaite() != null) {
            LocalDate souhaitee = demande.getDateSouhaite().toLocalDate();
            if (!souhaitee.isBefore(aujourdHui)) {
                return souhaitee;
            }
        }
        return aujourdHui;
    }

    private String lieuTravail(OffreEmploi offre, Demande demande) {
        if (offre != null && texteOuNull(offre.getLieu()) != null) {
            return offre.getLieu().trim();
        }
        if (demande != null && demande.getDepartement() != null
                && texteOuNull(demande.getDepartement().getNomDept()) != null) {
            return demande.getDepartement().getNomDept().trim();
        }
        return parametres.getLieuSignature();
    }

    private String nomParDefaut(Candidature candidature) {
        if (candidature.getUtilisateur() != null) {
            Employe employe = candidature.getUtilisateur().getEmploye();
            if (employe != null && texteOuNull(employe.getNomEmp()) != null) {
                return employe.getNomEmp().trim();
            }
            if (texteOuNull(candidature.getUtilisateur().getUsername()) != null) {
                return candidature.getUtilisateur().getUsername().trim();
            }
        }
        return "";
    }

    private String obligatoire(String valeur, String message) {
        String resultat = texteOuNull(valeur);
        if (resultat == null) {
            throw new IllegalStateException(message);
        }
        return resultat;
    }

    private String premierNonVide(String... valeurs) {
        for (String valeur : valeurs) {
            String resultat = texteOuNull(valeur);
            if (resultat != null) {
                return resultat;
            }
        }
        return "";
    }

    private String texteOuNull(String valeur) {
        if (valeur == null || valeur.isBlank()) {
            return null;
        }
        return valeur.trim();
    }
}
