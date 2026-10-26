package com.gestion.controller;

import com.gestion.exception.RessourceIntrouvableException;
import com.gestion.model.Candidature;
import com.gestion.model.QuestionContrat;
import com.gestion.model.ReponseContrat;
import com.gestion.service.NegociationContratService;
import com.gestion.service.QuestionContratService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/candidatures/rh/{id}/propositions")
@PreAuthorize("hasAnyAuthority('ROLE_ADMINISTRATEUR', 'ROLE_RH')")
@RequiredArgsConstructor
public class PropositionContratController {

    private final NegociationContratService negociationContratService;
    private final QuestionContratService questionContratService;

    @GetMapping
    public String etudier(@PathVariable Integer id, Model model) {
        Candidature candidature = negociationContratService.chargerCandidature(id);
        List<ReponseContrat> tour = negociationContratService.dernierTour(id);
        Map<Integer, ReponseContrat> reponses = negociationContratService.reponsesParQuestion(tour);
        Integer salaireCourant = negociationContratService.salaireCourant(candidature, tour);

        model.addAttribute("pageTitle", "Etude de la proposition");
        model.addAttribute("candidature", candidature);
        model.addAttribute("questions", questionContratService.listerActives());
        model.addAttribute("reponses", reponses);
        model.addAttribute("salaireCourant", salaireCourant);
        return "candidature/proposition-contrat";
    }

    @PostMapping("/accepter")
    public String accepter(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            negociationContratService.accepterProposition(id);
            redirectAttributes.addFlashAttribute("succes",
                    "La proposition du candidat est acceptee. Le contrat peut etre emis.");
        } catch (RessourceIntrouvableException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("erreur", ex.getMessage());
        }
        return "redirect:/candidatures/rh/" + id;
    }

    @PostMapping("/contre-proposer")
    public String contreProposer(@PathVariable Integer id,
                                 @RequestParam(name = "salairePropose") Integer salairePropose,
                                 @RequestParam Map<String, String> params,
                                 RedirectAttributes redirectAttributes) {
        try {
            Map<Integer, Boolean> acceptations = new HashMap<>();
            Map<Integer, String> propositions = new HashMap<>();
            for (QuestionContrat question : questionContratService.listerActives()) {
                Integer questionId = question.getIdQuestionContrat();
                acceptations.put(questionId, params.containsKey("accepte_" + questionId));
                propositions.put(questionId, params.get("proposition_" + questionId));
            }
            negociationContratService.contreProposer(id, salairePropose, acceptations,
                    propositions, params.get("commentaire"));
            redirectAttributes.addFlashAttribute("succes",
                    "La contre-proposition a ete envoyee au candidat.");
        } catch (RessourceIntrouvableException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("erreur", ex.getMessage());
            return "redirect:/candidatures/rh/" + id + "/propositions";
        }
        return "redirect:/candidatures/rh/" + id;
    }

    @PostMapping("/refuser")
    public String refuser(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            negociationContratService.refuserProposition(id);
            redirectAttributes.addFlashAttribute("succes", "La candidature est refusee.");
        } catch (RessourceIntrouvableException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("erreur", ex.getMessage());
        }
        return "redirect:/candidatures/rh/" + id;
    }
}
