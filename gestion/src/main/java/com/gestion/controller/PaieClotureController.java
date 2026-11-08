package com.gestion.controller;

import com.gestion.exception.AccesRefuseException;
import com.gestion.model.PeriodePaie;
import com.gestion.model.Utilisateur;
import com.gestion.service.PaieClotureService;
import com.gestion.service.UtilisateurDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@Controller
@PreAuthorize("hasAnyAuthority('ROLE_ADMINISTRATEUR','ROLE_RH','ROLE_FINANCE','ROLE_PAIE')")
@RequiredArgsConstructor
public class PaieClotureController {

    private final PaieClotureService paieClotureService;

    @GetMapping("/paie/cloture")
    public String index(@RequestParam(name = "annee", required = false) Integer annee,
                        @RequestParam(name = "mois", required = false) Integer mois,
                        Model model) {
        LocalDate aujourdHui = LocalDate.now();
        int anneeCourante = annee == null ? aujourdHui.getYear() : annee;
        int moisCourant = mois == null ? aujourdHui.getMonthValue() : mois;
        PeriodePaie periode = paieClotureService.obtenirOuCreer(anneeCourante, moisCourant);
        model.addAttribute("pageTitle", "Cloture de paie");
        model.addAttribute("annee", anneeCourante);
        model.addAttribute("mois", moisCourant);
        model.addAttribute("periode", periode);
        model.addAttribute("periodes", paieClotureService.periodes());
        model.addAttribute("variables", paieClotureService.variables(periode.getIdPeriodePaie()));
        return "paie/cloture";
    }

    @PostMapping("/paie/cloture/generer")
    public String generer(@RequestParam("annee") Integer annee,
                          @RequestParam("mois") Integer mois,
                          RedirectAttributes flash) {
        try {
            int total = paieClotureService.genererVariables(annee, mois);
            flash.addFlashAttribute("succes", total + " variable(s) de paie generee(s).");
        } catch (IllegalStateException ex) {
            flash.addFlashAttribute("erreur", ex.getMessage());
        }
        return "redirect:/paie/cloture?annee=" + annee + "&mois=" + mois;
    }

    @PostMapping("/paie/cloture/cloturer")
    public String cloturer(@RequestParam("annee") Integer annee,
                           @RequestParam("mois") Integer mois,
                           Authentication authentication, RedirectAttributes flash) {
        try {
            paieClotureService.cloturer(annee, mois, utilisateur(authentication));
            flash.addFlashAttribute("succes", "Periode cloturee : variables figees.");
        } catch (IllegalStateException ex) {
            flash.addFlashAttribute("erreur", ex.getMessage());
        }
        return "redirect:/paie/cloture?annee=" + annee + "&mois=" + mois;
    }

    @GetMapping("/paie/variables/{id}/export")
    public ResponseEntity<byte[]> exporter(@PathVariable Integer id) {
        String csv = paieClotureService.exporterCsv(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("variables-paie-" + id + ".csv", StandardCharsets.UTF_8).toString())
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv.getBytes(StandardCharsets.UTF_8));
    }

    private Utilisateur utilisateur(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof UtilisateurDetails details) {
            return details.getUtilisateur();
        }
        throw new AccesRefuseException("Vous devez etre connecte.");
    }
}
