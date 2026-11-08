package com.gestion.controller;

import com.gestion.enums.TypeSortie;
import com.gestion.exception.AccesRefuseException;
import com.gestion.model.Utilisateur;
import com.gestion.service.EmployeService;
import com.gestion.service.SortieService;
import com.gestion.service.UtilisateurDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@PreAuthorize("hasAnyAuthority('ROLE_ADMINISTRATEUR','ROLE_RH')")
@RequiredArgsConstructor
public class SortieController {

    private final SortieService sortieService;
    private final EmployeService employeService;

    @GetMapping("/sorties")
    public String liste(Model model, Authentication authentication) {
        model.addAttribute("pageTitle", "Gestion des sorties");
        model.addAttribute("sorties", sortieService.lister());
        model.addAttribute("employes", employeService.listerActifs());
        model.addAttribute("typesSortie", TypeSortie.values());
        model.addAttribute("demandeur", utilisateur(authentication));
        return "sortie/liste";
    }

    @PostMapping("/sorties")
    public String creer(@RequestParam("employeId") Integer employeId,
                        @RequestParam("typeSortie") TypeSortie typeSortie,
                        @RequestParam("dateSortie")
                        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateSortie,
                        @RequestParam(name = "motif", required = false) String motif,
                        @RequestParam(name = "preavisJours", required = false) Integer preavisJours,
                        @RequestParam(name = "commentaire", required = false) String commentaire,
                        Authentication authentication, RedirectAttributes flash) {
        try {
            sortieService.creer(employeId, typeSortie, dateSortie, motif, preavisJours,
                    commentaire, utilisateur(authentication));
            flash.addFlashAttribute("succes", "Sortie enregistree.");
        } catch (IllegalStateException ex) {
            flash.addFlashAttribute("erreur", ex.getMessage());
        }
        return "redirect:/sorties";
    }

    @GetMapping("/sorties/{id}")
    public String detail(@PathVariable Integer id, Model model) {
        model.addAttribute("pageTitle", "Dossier de sortie");
        model.addAttribute("sortie", sortieService.trouver(id));
        model.addAttribute("checklist", sortieService.checklist(id));
        return "sortie/detail";
    }

    @PostMapping("/sorties/checklist/{id}")
    public String traiterChecklist(@PathVariable Integer id,
                                   @RequestParam(name = "fait", defaultValue = "false") boolean fait,
                                   @RequestParam(name = "commentaire", required = false) String commentaire,
                                   @RequestParam("sortieId") Integer sortieId,
                                   RedirectAttributes flash) {
        try {
            sortieService.marquerChecklist(id, fait, commentaire);
            flash.addFlashAttribute("succes", fait ? "Tache terminee." : "Tache rouverte.");
        } catch (IllegalStateException ex) {
            flash.addFlashAttribute("erreur", ex.getMessage());
        }
        return "redirect:/sorties/" + sortieId;
    }

    @PostMapping("/sorties/{id}/valider")
    public String valider(@PathVariable Integer id,
                          @RequestParam(name = "soldeToutCompte", required = false) Integer soldeToutCompte,
                          Authentication authentication, RedirectAttributes flash) {
        try {
            sortieService.valider(id, soldeToutCompte, utilisateur(authentication));
            flash.addFlashAttribute("succes", "Sortie validee : le salarie est marque sortant.");
        } catch (IllegalStateException ex) {
            flash.addFlashAttribute("erreur", ex.getMessage());
        }
        return "redirect:/sorties";
    }

    @PostMapping("/sorties/{id}/annuler")
    public String annuler(@PathVariable Integer id, RedirectAttributes flash) {
        try {
            sortieService.annuler(id);
            flash.addFlashAttribute("succes", "Sortie annulee.");
        } catch (IllegalStateException ex) {
            flash.addFlashAttribute("erreur", ex.getMessage());
        }
        return "redirect:/sorties";
    }

    private Utilisateur utilisateur(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof UtilisateurDetails details) {
            return details.getUtilisateur();
        }
        throw new AccesRefuseException("Vous devez etre connecte.");
    }
}
