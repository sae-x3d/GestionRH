package com.gestion.controller;

import com.gestion.exception.RessourceIntrouvableException;
import com.gestion.model.Candidature;
import com.gestion.model.QuestionContrat;
import com.gestion.model.ReponseContrat;
import com.gestion.model.Utilisateur;
import com.gestion.service.NegociationContratService;
import com.gestion.service.QuestionContratService;
import com.gestion.service.UtilisateurDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class QuestionnaireContratController {

    private final NegociationContratService negociationContratService;
    private final QuestionContratService questionContratService;

    @GetMapping("/candidatures/{id}/questionnaire-contrat")
    public String questionnaire(@PathVariable Integer id, Model model, Authentication authentication) {
        Utilisateur utilisateur = utilisateurCourant(authentication);
        Candidature candidature = negociationContratService.chargerCandidature(id);
        verifierProprietaire(candidature, utilisateur);

        List<ReponseContrat> dernierTour = negociationContratService.dernierTour(id);
        Map<Integer, ReponseContrat> reponses = negociationContratService.reponsesParQuestion(dernierTour);
        Integer salaireCourant = negociationContratService.salaireCourant(candidature, dernierTour);
        boolean peutRepondre = negociationContratService.candidatPeutRepondre(candidature, dernierTour);

        model.addAttribute("pageTitle", "Questionnaire du contrat");
        model.addAttribute("candidature", candidature);
        model.addAttribute("questions", questionContratService.listerActives());
        model.addAttribute("reponses", reponses);
        model.addAttribute("salaireCourant", salaireCourant);
        model.addAttribute("salaireAccepte", reponseSalaireAcceptee(dernierTour));
        model.addAttribute("peutRepondre", peutRepondre);
        model.addAttribute("auteurDernierTour", auteur(dernierTour));
        return "candidature/questionnaire-contrat";
    }

    @PostMapping("/candidatures/{id}/questionnaire-contrat")
    public String soumettre(@PathVariable Integer id,
                            @RequestParam Map<String, String> params,
                            Authentication authentication,
                            RedirectAttributes redirectAttributes) {
        Utilisateur utilisateur = utilisateurCourant(authentication);
        try {
            boolean refusGlobal = "refuser".equalsIgnoreCase(params.get("action"));
            boolean salaireAccepte = params.containsKey("salaireAccepte");

            Map<Integer, Boolean> acceptations = new HashMap<>();
            Map<Integer, String> propositions = new HashMap<>();
            for (QuestionContrat question : questionContratService.listerActives()) {
                Integer questionId = question.getIdQuestionContrat();
                acceptations.put(questionId, params.containsKey("accepte_" + questionId));
                propositions.put(questionId, params.get("proposition_" + questionId));
            }

            Candidature candidature = negociationContratService.soumettreParCandidat(
                    id, utilisateur.getIdUser(), refusGlobal, salaireAccepte,
                    entier(params.get("salairePropose")), acceptations, propositions);
            redirectAttributes.addFlashAttribute("succes",
                    "ACCEPTEE".equalsIgnoreCase(candidature.getStatut().getLibelle())
                            ? "Vos termes sont acceptes. Les RH vont preparer le contrat."
                            : "Votre reponse a ete envoyee aux RH.");
            return "redirect:/candidatures";
        } catch (RessourceIntrouvableException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("erreur", ex.getMessage());
            return "redirect:/candidatures/" + id + "/questionnaire-contrat";
        }
    }

    private boolean reponseSalaireAcceptee(List<ReponseContrat> tour) {
        ReponseContrat salaire = negociationContratService.reponseSalaire(tour);
        return salaire == null || salaire.isAccepte();
    }

    private String auteur(List<ReponseContrat> tour) {
        return tour.isEmpty() ? null : tour.get(0).getAuteur();
    }

    private Integer entier(String valeur) {
        try {
            return valeur == null || valeur.isBlank() ? null : Integer.valueOf(valeur.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private void verifierProprietaire(Candidature candidature, Utilisateur utilisateur) {
        if (candidature.getUtilisateur() == null
                || !candidature.getUtilisateur().getIdUser().equals(utilisateur.getIdUser())) {
            throw new AccessDeniedException("Cette candidature ne vous appartient pas.");
        }
    }

    private Utilisateur utilisateurCourant(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof UtilisateurDetails details) {
            return details.getUtilisateur();
        }
        throw new AccessDeniedException("Connexion requise");
    }
}
