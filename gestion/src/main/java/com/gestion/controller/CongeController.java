package com.gestion.controller;

import com.gestion.exception.AccesRefuseException;
import com.gestion.model.DemandeConge;
import com.gestion.model.TypeConge;
import com.gestion.model.Utilisateur;
import com.gestion.service.CongeReferentielService;
import com.gestion.service.CongeService;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class CongeController {

    private final CongeService congeService;
    private final CongeReferentielService referentielService;

    @GetMapping("/conges")
    public String mesConges(Model model, Authentication authentication) {
        Utilisateur utilisateur = utilisateur(authentication);
        model.addAttribute("pageTitle", "Mes conges");
        model.addAttribute("types", referentielService.typesActifs());
        model.addAttribute("demandes", utilisateur.getEmploye() == null ? java.util.List.of()
                : congeService.demandesEmploye(utilisateur.getEmploye().getIdEmp()));
        Map<Integer, BigDecimal> soldes = new HashMap<>();
        if (utilisateur.getEmploye() != null) {
            for (TypeConge type : referentielService.typesActifs()) {
                soldes.put(type.getIdTypeConge(), congeService.totalDisponible(
                        utilisateur.getEmploye().getIdEmp(), type.getIdTypeConge()));
            }
            model.addAttribute("lots", congeService.soldes(utilisateur.getEmploye().getIdEmp()));
        }
        model.addAttribute("soldes", soldes);
        model.addAttribute("estManager", congeService.estManager(utilisateur));
        model.addAttribute("estRh", congeService.estRhOuAdmin(utilisateur));
        return "conges/liste";
    }

    @PostMapping("/conges")
    public String soumettre(@RequestParam("typeConge") Integer typeConge,
                            @RequestParam("dateDebut")
                            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
                            @RequestParam("dateFin")
                            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
                            @RequestParam(name = "demiJourDebut", defaultValue = "false") boolean demiJourDebut,
                            @RequestParam(name = "demiJourFin", defaultValue = "false") boolean demiJourFin,
                            @RequestParam(name = "motif", required = false) String motif,
                            @RequestParam(name = "justificatif", required = false) MultipartFile justificatif,
                            Authentication authentication, RedirectAttributes flash) {
        Utilisateur utilisateur = utilisateur(authentication);
        try {
            congeService.soumettre(utilisateur.getEmploye(), typeConge, dateDebut, dateFin,
                    demiJourDebut, demiJourFin, motif, utilisateur, justificatif);
            flash.addFlashAttribute("succes", "Demande de conge soumise.");
        } catch (IllegalStateException ex) {
            flash.addFlashAttribute("erreur", ex.getMessage());
        }
        return "redirect:/conges";
    }

    @GetMapping("/conges/{id}/justificatif")
    public ResponseEntity<ByteArrayResource> justificatif(@PathVariable Integer id,
                                                           Authentication authentication) {
        Utilisateur user = utilisateur(authentication);
        Path path = congeService.cheminJustificatif(id, user);
        try {
            byte[] bytes = Files.readAllBytes(path);
            MediaType mediaType = MediaTypeFactory.getMediaType(path.getFileName().toString())
                    .orElse(MediaType.APPLICATION_OCTET_STREAM);
            return ResponseEntity.ok().contentType(mediaType)
                    .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                            .filename(path.getFileName().toString()).build().toString())
                    .body(new ByteArrayResource(bytes));
        } catch (IOException e) {
            throw new com.gestion.exception.RessourceIntrouvableException("Justificatif introuvable.");
        }
    }

    @PostMapping("/conges/{id}/annuler")
    public String annuler(@PathVariable Integer id, Authentication authentication, RedirectAttributes flash) {
        try {
            congeService.annuler(id, utilisateur(authentication));
            flash.addFlashAttribute("succes", "Demande annulee et solde recredite.");
        } catch (IllegalStateException | AccesRefuseException ex) {
            flash.addFlashAttribute("erreur", ex.getMessage());
        }
        return "redirect:/conges";
    }

    @GetMapping("/conges/validation/manager")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER','ROLE_ADMINISTRATEUR','ROLE_RH')")
    public String validationManager(Model model, Authentication authentication) {
        model.addAttribute("pageTitle", "Validation des conges");
        model.addAttribute("demandes", congeService.demandesEnAttenteManager(utilisateur(authentication)));
        return "conges/validation-manager";
    }

    @PostMapping("/conges/validation/manager/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER','ROLE_ADMINISTRATEUR','ROLE_RH')")
    public String traiterManager(@PathVariable Integer id,
                                 @RequestParam("accepter") boolean accepter,
                                 @RequestParam(name = "commentaire", required = false) String commentaire,
                                 Authentication authentication, RedirectAttributes flash) {
        try {
            congeService.validerManager(id, accepter, commentaire, utilisateur(authentication));
            flash.addFlashAttribute("succes", accepter ? "Demande transmise a la RH." : "Demande refusee.");
        } catch (IllegalStateException | AccesRefuseException ex) {
            flash.addFlashAttribute("erreur", ex.getMessage());
        }
        return "redirect:/conges/validation/manager";
    }

    @GetMapping("/conges/validation/rh")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMINISTRATEUR','ROLE_RH')")
    public String validationRh(Model model) {
        model.addAttribute("pageTitle", "Validation RH des conges");
        model.addAttribute("demandes", congeService.demandesEnAttenteRh());
        return "conges/validation-rh";
    }

    @GetMapping("/conges/validation/direction")
    @PreAuthorize("hasAnyAuthority('ROLE_DIRECTION','ROLE_ADMINISTRATEUR')")
    public String validationDirection(Model model) {
        model.addAttribute("pageTitle", "Validation Direction des conges exceptionnels");
        model.addAttribute("demandes", congeService.demandesEnAttenteDirection());
        return "conges/validation-direction";
    }

    @PostMapping("/conges/validation/direction/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_DIRECTION','ROLE_ADMINISTRATEUR')")
    public String traiterDirection(@PathVariable Integer id, @RequestParam boolean accepter,
                                   @RequestParam(required = false) String commentaire,
                                   Authentication authentication, RedirectAttributes flash) {
        try { congeService.validerDirection(id, accepter, commentaire, utilisateur(authentication));
            flash.addFlashAttribute("succes", "Decision Direction enregistree.");
        } catch (IllegalStateException | AccesRefuseException e) { flash.addFlashAttribute("erreur", e.getMessage()); }
        return "redirect:/conges/validation/direction";
    }

    @PostMapping("/conges/validation/rh/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMINISTRATEUR','ROLE_RH')")
    public String traiterRh(@PathVariable Integer id,
                            @RequestParam("accepter") boolean accepter,
                            @RequestParam(name = "commentaire", required = false) String commentaire,
                            Authentication authentication, RedirectAttributes flash) {
        try {
            congeService.validerRh(id, accepter, commentaire, utilisateur(authentication));
            flash.addFlashAttribute("succes", accepter ? "Demande validee." : "Demande refusee.");
        } catch (IllegalStateException | AccesRefuseException ex) {
            flash.addFlashAttribute("erreur", ex.getMessage());
        }
        return "redirect:/conges/validation/rh";
    }

    @GetMapping("/conges/calendrier")
    public String calendrier(@RequestParam(name = "mois", required = false) String mois,
                             Model model, Authentication authentication) {
        YearMonth moisCourant = parseMois(mois);
        LocalDate debut = moisCourant.atDay(1);
        LocalDate fin = moisCourant.atEndOfMonth();
        model.addAttribute("pageTitle", "Calendrier des conges");
        model.addAttribute("mois", moisCourant);
        model.addAttribute("debut", debut);
        model.addAttribute("fin", fin);
        model.addAttribute("demandes", congeService.calendrier(debut, fin, utilisateur(authentication)));
        model.addAttribute("moisPrecedent", moisCourant.minusMonths(1));
        model.addAttribute("moisSuivant", moisCourant.plusMonths(1));
        return "conges/calendrier";
    }

    @GetMapping("/conges/parametres")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMINISTRATEUR','ROLE_RH')")
    public String parametres(Model model) {
        model.addAttribute("pageTitle", "Referentiels de conges");
        model.addAttribute("types", referentielService.types());
        model.addAttribute("joursFeries", referentielService.joursFeries());
        model.addAttribute("regimes", referentielService.regimes());
        model.addAttribute("droits", referentielService.droits());
        return "conges/parametres";
    }

    @PostMapping("/conges/parametres/type")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMINISTRATEUR','ROLE_RH')")
    public String ajouterType(@RequestParam("code") String code,
                              @RequestParam("libelle") String libelle,
                              @RequestParam(name = "decompteSolde", defaultValue = "false") boolean decompteSolde,
                              @RequestParam(name = "justificatifRequis", defaultValue = "false") boolean justificatifRequis,
                              @RequestParam(name = "preavisJours", defaultValue = "0") Integer preavisJours,
                              @RequestParam(name = "dureeMaxJours", required = false) BigDecimal dureeMaxJours,
                              @RequestParam(name = "validationDirection", defaultValue = "false") Boolean validationDirection,
                              @RequestParam(name = "directionAPartirJours", required = false) BigDecimal directionAPartirJours,
                              RedirectAttributes flash) {
        try {
            referentielService.ajouterType(code, libelle, decompteSolde, justificatifRequis,
                    preavisJours, dureeMaxJours, validationDirection, directionAPartirJours);
            flash.addFlashAttribute("succes", "Type de conge ajoute.");
        } catch (IllegalStateException ex) {
            flash.addFlashAttribute("erreur", ex.getMessage());
        }
        return "redirect:/conges/parametres";
    }

    @PostMapping("/conges/parametres/jour-ferie")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMINISTRATEUR','ROLE_RH')")
    public String ajouterJourFerie(@RequestParam("date")
                                   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                   @RequestParam("libelle") String libelle,
                                   RedirectAttributes flash) {
        try {
            referentielService.ajouterJourFerie(date, libelle);
            flash.addFlashAttribute("succes", "Jour ferie ajoute.");
        } catch (IllegalStateException ex) {
            flash.addFlashAttribute("erreur", ex.getMessage());
        }
        return "redirect:/conges/parametres";
    }

    @PostMapping("/conges/parametres/regime")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMINISTRATEUR','ROLE_RH')")
    public String ajouterRegime(@RequestParam("libelle") String libelle,
                                @RequestParam(name = "heuresHebdomadaires", required = false) BigDecimal heures,
                                @RequestParam(name = "joursOuvres", required = false) String joursOuvres,
                                RedirectAttributes flash) {
        try {
            referentielService.ajouterRegime(libelle, heures, joursOuvres);
            flash.addFlashAttribute("succes", "Regime de travail ajoute.");
        } catch (IllegalStateException ex) {
            flash.addFlashAttribute("erreur", ex.getMessage());
        }
        return "redirect:/conges/parametres";
    }

    @PostMapping("/conges/parametres/droit")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMINISTRATEUR','ROLE_RH')")
    public String ajouterDroit(@RequestParam("typeConge") Integer typeConge,
                               @RequestParam(name = "ancienneteMinMois", defaultValue = "0") Integer anciennete,
                               @RequestParam("joursParAn") BigDecimal joursParAn,
                               RedirectAttributes flash) {
        try {
            referentielService.ajouterDroit(typeConge, anciennete, joursParAn);
            flash.addFlashAttribute("succes", "Droit a conge ajoute.");
        } catch (IllegalStateException ex) {
            flash.addFlashAttribute("erreur", ex.getMessage());
        }
        return "redirect:/conges/parametres";
    }

    @PostMapping("/conges/batch/credits")
    @PreAuthorize("hasAuthority('ROLE_ADMINISTRATEUR')")
    public String lancerCredits(RedirectAttributes flash) {
        int credits = congeService.crediterDroitsAnnuels();
        flash.addFlashAttribute("succes", credits + " credit(s) de conge cree(s).");
        return "redirect:/conges/parametres";
    }

    @PostMapping("/conges/batch/expiration")
    @PreAuthorize("hasAuthority('ROLE_ADMINISTRATEUR')")
    public String lancerExpiration(RedirectAttributes flash) {
        int expires = congeService.expirerSoldes();
        flash.addFlashAttribute("succes", expires + " lot(s) expire(s).");
        return "redirect:/conges/parametres";
    }

    private YearMonth parseMois(String mois) {
        if (mois == null || mois.isBlank()) {
            return YearMonth.now();
        }
        try {
            return YearMonth.parse(mois);
        } catch (RuntimeException ex) {
            return YearMonth.now();
        }
    }

    private Utilisateur utilisateur(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof UtilisateurDetails details) {
            return details.getUtilisateur();
        }
        throw new AccesRefuseException("Vous devez etre connecte.");
    }
}
