package com.gestion.service;

import com.gestion.dto.CreateCandidatureDTO;
import com.gestion.dto.LigneParcoursDTO;
import com.gestion.enums.CategorieNiveau;
import com.gestion.enums.StatutOffre;
import com.gestion.enums.TypeEntretien;
import com.gestion.exception.RessourceIntrouvableException;
import com.gestion.model.Candidature;
import com.gestion.model.CandidatureStatus;
import com.gestion.model.Entretien;
import com.gestion.model.Info;
import com.gestion.model.Niveau;
import com.gestion.model.OffreEmploi;
import com.gestion.model.Parcours;
import com.gestion.model.Question;
import com.gestion.model.Reponse;
import com.gestion.model.Utilisateur;
import com.gestion.repository.CandidatureRepository;
import com.gestion.repository.EntretienRepository;
import com.gestion.repository.NiveauRepository;
import com.gestion.repository.OffreEmploiRepository;
import com.gestion.repository.ParcoursRepository;
import com.gestion.repository.QuestionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class CandidatureService {

    private final CandidatureRepository candidatureRepository;
    private final OffreEmploiRepository offreEmploiRepository;
    private final ParcoursRepository parcoursRepository;
    private final NiveauRepository niveauRepository;
    private final CandidatureStatusService candidatureStatusService;
    private final StockageCvService stockageCvService;
    private final QuestionRepository questionRepository;
    private final EntretienRepository entretienRepository;

    public CandidatureService(CandidatureRepository candidatureRepository,
                              OffreEmploiRepository offreEmploiRepository,
                              ParcoursRepository parcoursRepository,
                              NiveauRepository niveauRepository,
                              CandidatureStatusService candidatureStatusService,
                              StockageCvService stockageCvService,
                              QuestionRepository questionRepository,
                              EntretienRepository entretienRepository) {
        this.candidatureRepository = candidatureRepository;
        this.offreEmploiRepository = offreEmploiRepository;
        this.parcoursRepository = parcoursRepository;
        this.niveauRepository = niveauRepository;
        this.candidatureStatusService = candidatureStatusService;
        this.stockageCvService = stockageCvService;
        this.questionRepository = questionRepository;
        this.entretienRepository = entretienRepository;
    }

    @Transactional(readOnly = true)
    public List<OffreEmploi> listerOffresOuvertes() {
        return offreEmploiRepository.findByStatutOrderByDatePublicationDesc(StatutOffre.PUBLIEE);
    }

    @Transactional(readOnly = true)
    public List<Candidature> listerParUtilisateur(Integer idUser) {
        return candidatureRepository.findByUtilisateurIdUserOrderByDateCandidatureDesc(idUser);
    }

    @Transactional(readOnly = true)
    public List<Candidature> listerParOffre(Integer idOffre) {
        return candidatureRepository.findByOffreIdOffreOrderByDateCandidatureDesc(idOffre);
    }

    @Transactional(readOnly = true)
    public List<Candidature> rechercherPourRh(Integer idOffre, String statut) {
        String statutFiltre = (statut == null || statut.isBlank()) ? null : statut.trim();
        return candidatureRepository.rechercherAvecDetails(idOffre, statutFiltre);
    }

    @Transactional(readOnly = true)
    public Candidature trouverParIdAvecDetails(Integer idCandidature) {
        return candidatureRepository.findByIdAvecDetails(idCandidature)
                .orElseThrow(() -> new RessourceIntrouvableException(
                        "Candidature introuvable : " + idCandidature));
    }

    @Transactional
    public Candidature marquerEnExamen(Integer idCandidature) {
        return transitionner(idCandidature, Set.of("RECUE"), "EN_EXAMEN", false);
    }

    @Transactional
    public Candidature retenir(Integer idCandidature) {
        return transitionner(idCandidature, Set.of("RECUE", "EN_EXAMEN"), "RETENUE", false);
    }

    @Transactional
    public Candidature refuser(Integer idCandidature) {
        return transitionner(idCandidature, Set.of("RECUE", "EN_EXAMEN", "RETENUE"), "REFUSEE", true);
    }

    @Transactional
    public Candidature accepter(Integer idCandidature) {
        return transitionner(idCandidature, Set.of("RETENUE"), "ACCEPTEE", true);
    }

    @Transactional
    public Entretien planifierEntretien(Integer idCandidature, TypeEntretien type,
                                        LocalDateTime dateHeure, String lieu, String participants) {
        Candidature candidature = trouverParIdAvecDetails(idCandidature);
        String statut = candidature.getStatut().getLibelle();
        if (!"EN_EXAMEN".equalsIgnoreCase(statut) && !"RETENUE".equalsIgnoreCase(statut)) {
            throw new IllegalStateException(
                    "Un entretien ou un test ne peut etre planifie que pour une candidature en examen ou retenue.");
        }
        if (type == null || dateHeure == null) {
            throw new IllegalStateException("Le type et la date de l'entretien/test sont obligatoires.");
        }

        Entretien entretien = Entretien.builder()
                .candidature(candidature)
                .type(type)
                .dateHeure(dateHeure)
                .lieu(texteOuNull(lieu))
                .participants(texteOuNull(participants))
                .build();
        return entretienRepository.save(entretien);
    }

    @Transactional
    public Entretien enregistrerResultat(Integer idEntretien, String resultat, String evaluation) {
        Entretien entretien = entretienRepository.findById(idEntretien)
                .orElseThrow(() -> new RessourceIntrouvableException(
                        "Entretien/test introuvable : " + idEntretien));
        if (resultat == null || resultat.isBlank()) {
            throw new IllegalStateException("Le resultat de l'entretien/test est obligatoire.");
        }
        entretien.setResultat(resultat.trim());
        entretien.setEvaluation(texteOuNull(evaluation));
        return entretienRepository.save(entretien);
    }

    @Transactional(readOnly = true)
    public List<Parcours> listerParcours() {
        return parcoursRepository.findAllByOrderByTypeAsc();
    }

    @Transactional(readOnly = true)
    public List<Niveau> listerNiveaux() {
        return niveauRepository.findAllByOrderByIdNiveauAsc();
    }

    @Transactional(readOnly = true)
    public boolean aDejaPostule(Integer idUser, Integer idOffre) {
        return candidatureRepository.existsByUtilisateurIdUserAndOffreIdOffre(idUser, idOffre);
    }

    @Transactional
    public Candidature postuler(CreateCandidatureDTO dto, MultipartFile cv, Utilisateur utilisateur) {
        OffreEmploi offre = offreEmploiRepository.findById(dto.getOffreId())
                .orElseThrow(() -> new RessourceIntrouvableException("Offre introuvable : " + dto.getOffreId()));

        verifierOffreOuverte(offre);

        if (candidatureRepository.existsByUtilisateurIdUserAndOffreIdOffre(
                utilisateur.getIdUser(), offre.getIdOffre())) {
            throw new IllegalStateException("Vous avez deja postule a cette offre.");
        }

        CandidatureStatus statutInitial = candidatureStatusService.trouverStatutInitial()
                .orElseThrow(() -> new RessourceIntrouvableException(
                        "Statut de candidature initial introuvable : \""
                                + candidatureStatusService.getLibelleStatutInitial() + "\""));

        Candidature candidature = Candidature.builder()
                .utilisateur(utilisateur)
                .offre(offre)
                .dateCandidature(LocalDateTime.now())
                .statut(statutInitial)
                .build();

        for (LigneParcoursDTO ligne : dto.getParcours()) {
            Parcours parcours = parcoursRepository.findById(ligne.getParcoursId())
                    .orElseThrow(() -> new RessourceIntrouvableException(
                            "Parcours introuvable : " + ligne.getParcoursId()));
            Niveau niveau = niveauRepository.findById(ligne.getNiveauId())
                    .orElseThrow(() -> new RessourceIntrouvableException(
                            "Niveau introuvable : " + ligne.getNiveauId()));

            verifierCoherenceParcoursNiveau(parcours, niveau);

            candidature.getParcours().add(Info.builder()
                    .candidature(candidature)
                    .parcours(parcours)
                    .niveau(niveau)
                    .domaine(texteOuNull(ligne.getDomaine()))
                    .commentaire(texteOuNull(ligne.getCommentaire()))
                    .build());
        }

        ajouterReponsesQuestionnaire(candidature, dto, offre);

        Candidature enregistree = candidatureRepository.save(candidature);

        String nomCv = stockageCvService.enregistrer(cv, utilisateur.getUsername(), enregistree.getIdCandidature());
        enregistree.setCvNum(nomCv);
        return candidatureRepository.save(enregistree);
    }

    private void ajouterReponsesQuestionnaire(Candidature candidature, CreateCandidatureDTO dto, OffreEmploi offre) {
        List<Question> questions = questionRepository
                .findByOffreIdOffreOrderByOrdreAscIdQuestionAsc(offre.getIdOffre());

        for (Question question : questions) {
            String valeur = dto.getReponses().get(question.getIdQuestion());
            if (valeur == null || valeur.isBlank()) {
                throw new IllegalStateException(
                        "La question \"" + question.getLibelle() + "\" est obligatoire.");
            }
            candidature.getReponses().add(Reponse.builder()
                    .candidature(candidature)
                    .question(question)
                    .valeur(valeur.trim())
                    .build());
        }
    }

    private Candidature transitionner(Integer idCandidature, Set<String> statutsAutorises,
                                     String statutCible, boolean decisionFinale) {
        Candidature candidature = trouverParIdAvecDetails(idCandidature);
        String statutActuel = candidature.getStatut().getLibelle() == null
                ? ""
                : candidature.getStatut().getLibelle().toUpperCase();

        if (!statutsAutorises.contains(statutActuel)) {
            throw new IllegalStateException("La candidature est au statut \"" + statutActuel
                    + "\" et ne peut pas passer au statut \"" + statutCible + "\".");
        }

        CandidatureStatus nouveauStatut = candidatureStatusService.trouverParLibelle(statutCible)
                .orElseThrow(() -> new RessourceIntrouvableException(
                        "Statut de candidature introuvable : \"" + statutCible + "\""));

        candidature.setStatut(nouveauStatut);
        if (decisionFinale) {
            candidature.setDateDecision(LocalDateTime.now());
        }
        return candidatureRepository.save(candidature);
    }

    private void verifierCoherenceParcoursNiveau(Parcours parcours, Niveau niveau) {
        CategorieNiveau categorieAttendue = "Etude".equalsIgnoreCase(parcours.getType())
                ? CategorieNiveau.DIPLOME
                : CategorieNiveau.DUREE;
        if (niveau.getCategorie() != categorieAttendue) {
            throw new IllegalStateException("Le niveau \"" + niveau.getLibelle()
                    + "\" n'est pas compatible avec le parcours \"" + parcours.getType() + "\".");
        }
    }

    private void verifierOffreOuverte(OffreEmploi offre) {
        if (offre.getStatut() != StatutOffre.PUBLIEE) {
            throw new IllegalStateException("Cette offre n'est pas ouverte aux candidatures.");
        }
        if (offre.getDateCloture() != null && offre.getDateCloture().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Cette offre est fermee : la date de cloture est depassee.");
        }
    }

    private String texteOuNull(String valeur) {
        if (valeur == null || valeur.isBlank()) {
            return null;
        }
        return valeur.trim();
    }
}
