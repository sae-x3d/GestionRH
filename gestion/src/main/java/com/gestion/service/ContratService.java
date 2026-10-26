package com.gestion.service;

import com.gestion.config.ParametresContrat;
import com.gestion.dto.CreerContratDTO;
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
        return contratRepository.findByCandidatureIdCandidature(id).orElse(null);
    }

    @Transactional(readOnly = true)
    public CreerContratDTO preparerFormulaire(Integer candidatureId) {
        return preparerFormulaire(chargerCandidature(candidatureId));
    }

    @Transactional
    public Contrat generer(Integer candidatureId, CreerContratDTO dto) {
        Candidature candidature = chargerCandidature(candidatureId);
        if (contratRepository.findByCandidatureIdCandidature(candidatureId).isPresent()) {
            throw new IllegalStateException("Un contrat existe deja pour cette candidature.");
        }
        return creer(candidature, dto);
    }

    @Transactional
    public Contrat genererAutomatiquement(Integer candidatureId, String nomSalarie,
                                          String adresseSalarie, LocalDate dateFin, String motifCdd) {
        Candidature candidature = chargerCandidature(candidatureId);
        Contrat existant = contratRepository.findByCandidatureIdCandidature(candidatureId).orElse(null);
        if (existant != null) {
            return existant;
        }

        CreerContratDTO dto = preparerFormulaire(candidature);
        dto.setNomSalarie(premierNonVide(nomSalarie, dto.getNomSalarie()));
        dto.setAdresseSalarie(premierNonVide(adresseSalarie, dto.getAdresseSalarie()));
        dto.setDateFin(dateFin);
        dto.setMotifCdd(texteOuNull(motifCdd));
        return creer(candidature, dto);
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
            emp = employeRepository.save(emp);
        }
        if (user.getEmploye() == null) {
            user.setEmploye(emp);
            utilisateurRepository.save(user);
        }
        c.setEmploye(emp);
        c.setDateAcceptation(LocalDateTime.now());
        c.setStatut("ACCEPTE");
        c.setPdf(pdfService.generer(c));
        Contrat accepteContrat = contratRepository.save(c);
        for (Utilisateur gestionnaire : utilisateurRepository.trouverGestionnairesCandidatures()) {
            notificationService.envoyer(gestionnaire, "CONTRAT",
                    "Le candidat a accepte le contrat " + c.getNumeroContrat() + ".",
                    "/contrats/" + c.getIdContrat());
        }
        return accepteContrat;
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

    private Contrat creer(Candidature candidature, CreerContratDTO dto) {
        if (!"ACCEPTEE".equalsIgnoreCase(candidature.getStatut().getLibelle())) {
            throw new IllegalStateException(
                    "La candidature doit etre acceptee par RH avant l'emission du contrat.");
        }

        String type = normaliserType(dto.getTypeContrat());
        LocalDate aujourdHui = LocalDate.now();
        LocalDate dateEffet = dto.getDateEffet();
        if (dateEffet == null) {
            throw new IllegalStateException("La date d'effet du contrat est obligatoire.");
        }
        if (dateEffet.isBefore(aujourdHui)) {
            throw new IllegalStateException("La date d'effet ne peut pas etre dans le passe.");
        }

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
                .motifCdd("CDD".equals(type) ? texteOuNull(dto.getMotifCdd()) : null)
                .lieuSignature(parametres.getLieuSignature())
                .dateSignature(null)
                .statut(STATUT_EN_ATTENTE)
                .dateGeneration(LocalDateTime.now())
                .build();
        contrat.setPdf(pdfService.generer(contrat));
        Contrat enregistre = contratRepository.save(contrat);
        notificationService.envoyer(candidature.getUtilisateur(), "CONTRAT",
                "Votre contrat " + enregistre.getNumeroContrat()
                        + " est disponible. Consultez-le et indiquez votre decision.",
                "/contrats/" + enregistre.getIdContrat());
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
        dto.setTypeContrat(normaliserType(offre == null ? null : offre.getTypeContrat()));
        dto.setNomSalarie(nomParDefaut(candidature));
        dto.setAdresseSalarie("");
        dto.setNomEmployeur(parametres.getEmployeurNom());
        dto.setAdresseEmployeur(parametres.getEmployeurAdresse());
        dto.setCategorieProfessionnelle(parametres.getCategorieProfessionnelle());
        dto.setIndiceClassification(parametres.getIndiceClassification());
        dto.setDureeTravail(parametres.getDureeTravail());
        dto.setLieuTravail(lieuTravail(offre, demande));
        dto.setDateEffet(dateEffetParDefaut(demande));
        return dto;
    }

    private Candidature chargerCandidature(Integer id) {
        return candidatureRepository.findByIdAvecDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("Candidature introuvable."));
    }

    private String numeroContrat(Candidature candidature) {
        return "CTR-" + LocalDate.now().getYear() + "-"
                + String.format("%04d", candidature.getIdCandidature());
    }

    private String normaliserType(String type) {
        if (type == null || type.isBlank()) {
            return "CDI";
        }
        String valeur = type.trim().toUpperCase();
        if (!List.of("CDI", "CDD").contains(valeur)) {
            throw new IllegalStateException("Type de contrat invalide : " + type);
        }
        return valeur;
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
