package com.gestion.service;

import com.gestion.exception.RessourceIntrouvableException;
import com.gestion.model.Candidature;
import com.gestion.model.CandidatureStatus;
import com.gestion.model.OffreEmploi;
import com.gestion.model.QuestionContrat;
import com.gestion.model.ReponseContrat;
import com.gestion.model.Utilisateur;
import com.gestion.repository.CandidatureRepository;
import com.gestion.repository.ReponseContratRepository;
import com.gestion.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NegociationContratService {

    public static final String CODE_SALAIRE = "SALAIRE";
    public static final String AUTEUR_CANDIDAT = "CANDIDAT";
    public static final String AUTEUR_RH = "RH";

    private final CandidatureRepository candidatureRepository;
    private final ReponseContratRepository reponseContratRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final QuestionContratService questionContratService;
    private final CandidatureStatusService candidatureStatusService;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public Candidature chargerCandidature(Integer idCandidature) {
        return candidatureRepository.findByIdAvecDetails(idCandidature)
                .orElseThrow(() -> new RessourceIntrouvableException(
                        "Candidature introuvable : " + idCandidature));
    }

    @Transactional(readOnly = true)
    public List<ReponseContrat> historique(Integer idCandidature) {
        return reponseContratRepository
                .findByCandidatureIdCandidatureOrderByNumeroTourAscIdReponseContratAsc(idCandidature);
    }

    @Transactional(readOnly = true)
    public List<ReponseContrat> dernierTour(Integer idCandidature) {
        int dernier = reponseContratRepository.trouverDernierTour(idCandidature);
        if (dernier == 0) {
            return List.of();
        }
        return reponseContratRepository
                .findByCandidatureIdCandidatureAndNumeroTourOrderByIdReponseContratAsc(idCandidature, dernier);
    }

    public ReponseContrat reponseSalaire(List<ReponseContrat> tour) {
        return tour.stream()
                .filter(reponse -> CODE_SALAIRE.equals(reponse.getCode()))
                .findFirst()
                .orElse(null);
    }

    public Map<Integer, ReponseContrat> reponsesParQuestion(List<ReponseContrat> tour) {
        Map<Integer, ReponseContrat> resultat = new HashMap<>();
        for (ReponseContrat reponse : tour) {
            if (reponse.getQuestion() != null) {
                resultat.put(reponse.getQuestion().getIdQuestionContrat(), reponse);
            }
        }
        return resultat;
    }

    public Integer salaireCourant(Candidature candidature, List<ReponseContrat> tour) {
        ReponseContrat salaire = reponseSalaire(tour);
        if (salaire != null && salaire.getProposition() != null && !salaire.getProposition().isBlank()) {
            Integer montant = montant(salaire.getProposition());
            if (montant != null && montant > 0) {
                return montant;
            }
        }
        OffreEmploi offre = candidature.getOffre();
        return offre == null ? null : offre.getSalaire();
    }

    public boolean candidatPeutRepondre(Candidature candidature, List<ReponseContrat> tour) {
        String statut = statut(candidature);
        if ("RETENU".equals(statut)) {
            return tour.isEmpty() || AUTEUR_RH.equals(auteur(tour));
        }
        return false;
    }

    @Transactional
    public Candidature soumettreParCandidat(Integer idCandidature, Integer idUser,
                                            boolean refusGlobal,
                                            boolean salaireAccepte,
                                            Integer salairePropose,
                                            Map<Integer, Boolean> acceptations,
                                            Map<Integer, String> propositions) {
        Candidature candidature = chargerCandidature(idCandidature);
        verifierProprietaire(candidature, idUser);
        List<ReponseContrat> tour = dernierTour(idCandidature);
        if (!candidatPeutRepondre(candidature, tour)) {
            throw new IllegalStateException("Le questionnaire contrat n'est pas disponible a cette etape.");
        }
        if (refusGlobal) {
            changerStatut(candidature, "CONTRAT_REFUSE");
            notifierGestionnaires("Le candidat " + nomCandidat(candidature)
                    + " a refuse le contrat.", lienDossier(candidature));
            return candidatureRepository.save(candidature);
        }

        Integer salaireCourant = salaireCourant(candidature, tour);
        Integer salaireRetenu = salaireAccepte ? salaireCourant : salairePropose;
        if (salaireRetenu == null || salaireRetenu <= 0) {
            throw new IllegalStateException("Le salaire est obligatoire : acceptez le salaire propose "
                    + "ou indiquez un montant.");
        }

        List<QuestionContrat> questions = questionContratService.listerActives();
        boolean toutAccepte = salaireAccepte;
        int numeroTour = prochainTour(idCandidature);
        for (QuestionContrat question : questions) {
            boolean accepte = Boolean.TRUE.equals(acceptations.get(question.getIdQuestionContrat()));
            String proposition = texteOuNull(propositions.get(question.getIdQuestionContrat()));
            if (!accepte && proposition == null) {
                throw new IllegalStateException("La question \"" + question.getLibelle()
                        + "\" doit etre acceptee ou faire l'objet d'une proposition.");
            }
            toutAccepte = toutAccepte && accepte;
            enregistrer(candidature, question, null, accepte, proposition,
                    numeroTour, AUTEUR_CANDIDAT);
        }
        enregistrer(candidature, null, CODE_SALAIRE, salaireAccepte,
                salaireAccepte ? null : String.valueOf(salaireRetenu), numeroTour, AUTEUR_CANDIDAT);

        if (toutAccepte) {
            candidature.setSalaireConvenu(salaireRetenu);
            changerStatut(candidature, "ACCEPTEE");
            notifierGestionnaires("Le candidat " + nomCandidat(candidature)
                    + " a accepte les termes du contrat.", lienDossier(candidature));
            notificationService.envoyer(candidature.getUtilisateur(), "CONTRAT",
                    "Vos termes sont acceptes. Le contrat va etre prepare par les RH.",
                    "/candidatures");
        } else {
            changerStatut(candidature, "PROPOSITION");
            notifierGestionnaires("Le candidat " + nomCandidat(candidature)
                    + " a fait une proposition sur le contrat.", lienPropositions(candidature));
        }
        return candidatureRepository.save(candidature);
    }

    @Transactional
    public Candidature accepterProposition(Integer idCandidature) {
        Candidature candidature = chargerCandidature(idCandidature);
        verifierStatut(candidature, "PROPOSITION");
        List<ReponseContrat> tour = dernierTour(idCandidature);
        if (!AUTEUR_CANDIDAT.equals(auteur(tour))) {
            throw new IllegalStateException("La derniere proposition n'a pas ete faite par le candidat.");
        }
        Integer salaire = salaireCourant(candidature, tour);
        if (salaire == null || salaire <= 0) {
            throw new IllegalStateException("Le salaire propose est invalide.");
        }
        candidature.setSalaireConvenu(salaire);
        changerStatut(candidature, "ACCEPTEE");
        notificationService.envoyer(candidature.getUtilisateur(), "CONTRAT",
                "Votre proposition a ete acceptee. Le contrat va etre prepare.",
                "/candidatures");
        return candidatureRepository.save(candidature);
    }

    @Transactional
    public Candidature contreProposer(Integer idCandidature, Integer salairePropose,
                                      Map<Integer, Boolean> acceptations,
                                      Map<Integer, String> propositions,
                                      String commentaire) {
        Candidature candidature = chargerCandidature(idCandidature);
        verifierStatut(candidature, "PROPOSITION");
        if (salairePropose == null || salairePropose <= 0) {
            throw new IllegalStateException("Le salaire propose par les RH est obligatoire.");
        }
        List<QuestionContrat> questions = questionContratService.listerActives();
        int numeroTour = prochainTour(idCandidature);
        for (QuestionContrat question : questions) {
            boolean accepte = Boolean.TRUE.equals(acceptations.get(question.getIdQuestionContrat()));
            String proposition = texteOuNull(propositions.get(question.getIdQuestionContrat()));
            if (!accepte && proposition == null) {
                throw new IllegalStateException("La question \"" + question.getLibelle()
                        + "\" doit etre acceptee ou faire l'objet d'une contre-proposition.");
            }
            enregistrer(candidature, question, null, accepte, proposition, numeroTour, AUTEUR_RH);
        }
        enregistrer(candidature, null, CODE_SALAIRE, false,
                String.valueOf(salairePropose), numeroTour, AUTEUR_RH);
        changerStatut(candidature, "RETENU");

        String message = "Les RH ont fait une contre-proposition pour votre contrat.";
        if (commentaire != null && !commentaire.isBlank()) {
            message += " " + commentaire.trim();
        }
        notificationService.envoyer(candidature.getUtilisateur(), "CONTRAT", message,
                lienQuestionnaire(candidature));
        return candidatureRepository.save(candidature);
    }

    @Transactional
    public Candidature refuserProposition(Integer idCandidature) {
        Candidature candidature = chargerCandidature(idCandidature);
        verifierStatut(candidature, "PROPOSITION");
        changerStatut(candidature, "REFUSEE");
        notificationService.envoyer(candidature.getUtilisateur(), "CONTRAT",
                "Votre proposition n'a pas ete retenue. La candidature est close.",
                "/candidatures");
        return candidatureRepository.save(candidature);
    }

    @Transactional
    public void envoyerQuestionnaire(Integer idCandidature) {
        Candidature candidature = chargerCandidature(idCandidature);
        verifierStatut(candidature, "RETENU");
        notificationService.envoyer(candidature.getUtilisateur(), "CONTRAT",
                "Vous etes retenu. Merci de repondre au questionnaire du contrat.",
                lienQuestionnaire(candidature));
    }

    private void enregistrer(Candidature candidature, QuestionContrat question, String code,
                             boolean accepte, String proposition, int numeroTour, String auteur) {
        reponseContratRepository.save(ReponseContrat.builder()
                .candidature(candidature)
                .question(question)
                .code(code)
                .accepte(accepte)
                .proposition(proposition)
                .numeroTour(numeroTour)
                .auteur(auteur)
                .dateReponse(LocalDateTime.now())
                .build());
    }

    private void verifierProprietaire(Candidature candidature, Integer idUser) {
        if (candidature.getUtilisateur() == null
                || !candidature.getUtilisateur().getIdUser().equals(idUser)) {
            throw new IllegalStateException("Cette candidature ne vous appartient pas.");
        }
    }

    private void verifierStatut(Candidature candidature, String statutAttendu) {
        if (!statutAttendu.equals(statut(candidature))) {
            throw new IllegalStateException("La candidature doit etre au statut " + statutAttendu + ".");
        }
    }

    private void changerStatut(Candidature candidature, String libelle) {
        CandidatureStatus statut = candidatureStatusService.trouverParLibelle(libelle)
                .orElseThrow(() -> new RessourceIntrouvableException(
                        "Statut de candidature introuvable : \"" + libelle + "\""));
        candidature.setStatut(statut);
        if ("ACCEPTEE".equals(libelle) || "REFUSEE".equals(libelle)
                || "CONTRAT_REFUSE".equals(libelle)) {
            candidature.setDateDecision(LocalDateTime.now());
        }
    }

    private int prochainTour(Integer idCandidature) {
        return reponseContratRepository.trouverDernierTour(idCandidature) + 1;
    }

    private String statut(Candidature candidature) {
        return candidature.getStatut() == null || candidature.getStatut().getLibelle() == null
                ? ""
                : candidature.getStatut().getLibelle().toUpperCase();
    }

    private String auteur(List<ReponseContrat> tour) {
        return tour.isEmpty() ? null : tour.get(0).getAuteur();
    }

    private Integer montant(String valeur) {
        try {
            return Integer.valueOf(valeur.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String texteOuNull(String valeur) {
        return valeur == null || valeur.isBlank() ? null : valeur.trim();
    }

    private String nomCandidat(Candidature candidature) {
        if (candidature.getUtilisateur() == null) {
            return "inconnu";
        }
        if (candidature.getUtilisateur().getEmploye() != null
                && candidature.getUtilisateur().getEmploye().getNomEmp() != null) {
            return candidature.getUtilisateur().getEmploye().getNomEmp();
        }
        return candidature.getUtilisateur().getUsername();
    }

    private void notifierGestionnaires(String message, String lien) {
        for (Utilisateur gestionnaire : utilisateurRepository.trouverGestionnairesCandidatures()) {
            notificationService.envoyer(gestionnaire, "CANDIDATURE", message, lien);
        }
    }

    private String lienDossier(Candidature candidature) {
        return "/candidatures/rh/" + candidature.getIdCandidature();
    }

    private String lienPropositions(Candidature candidature) {
        return "/candidatures/rh/" + candidature.getIdCandidature() + "/propositions";
    }

    private String lienQuestionnaire(Candidature candidature) {
        return "/candidatures/" + candidature.getIdCandidature() + "/questionnaire-contrat";
    }
}
