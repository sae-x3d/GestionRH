package com.gestion.controller;

import com.gestion.enums.ChampSensible;
import com.gestion.exception.AccesRefuseException;
import com.gestion.model.Employe;
import com.gestion.model.Utilisateur;
import com.gestion.repository.ContratRepository;
import com.gestion.repository.RegimeTravailRepository;
import com.gestion.service.AuditService;
import com.gestion.service.DocumentEmployeService;
import com.gestion.service.DossierAdministratifService;
import com.gestion.service.EmployeService;
import com.gestion.service.HistoriqueCarriereService;
import com.gestion.service.UtilisateurDetails;
import jakarta.servlet.http.HttpServletRequest;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequiredArgsConstructor
public class EmployeController {

    private final EmployeService employeService;
    private final DossierAdministratifService dossierService;
    private final HistoriqueCarriereService historiqueCarriereService;
    private final DocumentEmployeService documentEmployeService;
    private final ContratRepository contratRepository;
    private final AuditService auditService;
    private final RegimeTravailRepository regimeTravailRepository;

    @GetMapping("/employes")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMINISTRATEUR','ROLE_RH','ROLE_MANAGER','ROLE_DIRECTION')")
    public String liste(Model model) {
        model.addAttribute("pageTitle", "Salaries");
        model.addAttribute("employes", employeService.listerActifs());
        return "employe/liste";
    }

    @GetMapping("/employes/{id}")
    public String detail(@PathVariable Integer id, Model model, Authentication authentication,
                         HttpServletRequest request, RedirectAttributes flash) {
        Employe employe = dossierService.trouver(id);
        Utilisateur utilisateur = utilisateur(authentication);
        if (!dossierService.peutVoirEmploye(employe, utilisateur)) {
            throw new AccesRefuseException("Vous ne pouvez pas consulter ce dossier.");
        }
        boolean voitSalaire = dossierService.peutVoirSalaire(employe, utilisateur);
        boolean voitRib = dossierService.peutVoirRib(employe, utilisateur);
        if (voitSalaire || voitRib) {
            auditService.enregistrer(utilisateur.getUsername(), "CONSULTATION",
                    "/employes/" + id, "Employe", String.valueOf(id),
                    "Consultation dossier" + (voitSalaire ? " (salaire)" : "")
                            + (voitRib ? " (RIB)" : ""), adresseIp(request));
        }
        model.addAttribute("pageTitle", "Dossier de " + employe.getNomEmp());
        model.addAttribute("employe", employe);
        model.addAttribute("regimes", regimeTravailRepository.findByActifTrueOrderByLibelleAsc());
        model.addAttribute("contrats", contratRepository.findByEmployeIdEmpOrderByDateGenerationDesc(id));
        model.addAttribute("documents", documentEmployeService.listerParEmploye(id));
        model.addAttribute("salaires", dossierService.historiqueSalaire(id));
        model.addAttribute("carrieres", historiqueCarriereService.lister(id));
        model.addAttribute("changements", dossierService.changementsEmploye(id));
        model.addAttribute("voitSalaire", voitSalaire);
        model.addAttribute("voitRib", voitRib);
        model.addAttribute("peutModifier", dossierService.peutGererDossier(utilisateur)
                || dossierService.estProprietaire(employe, utilisateur));
        model.addAttribute("peutGerer", dossierService.peutGererDossier(utilisateur));
        model.addAttribute("champs", ChampSensible.values());
        model.addAttribute("demandeur", utilisateur);
        return "employe/detail";
    }

    @PostMapping("/employes/{id}/regime")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMINISTRATEUR','ROLE_RH')")
    public String regime(@PathVariable Integer id, @RequestParam(required = false) Integer idRegime,
                         RedirectAttributes flash) {
        Employe employe = dossierService.trouver(id);
        employe.setRegimeTravail(idRegime == null ? null : regimeTravailRepository.findById(idRegime)
                .orElseThrow(() -> new com.gestion.exception.RessourceIntrouvableException("Regime introuvable.")));
        employeService.sauvegarder(employe);
        flash.addFlashAttribute("succes", "Regime de travail applique.");
        return "redirect:/employes/" + id;
    }

    @GetMapping("/mon-dossier")
    public String monDossier(Model model, Authentication authentication) {
        Utilisateur utilisateur = utilisateur(authentication);
        if (utilisateur.getEmploye() == null) {
            model.addAttribute("pageTitle", "Mon dossier");
            model.addAttribute("erreur", "Aucun dossier salarie n'est rattache a votre compte.");
            return "employe/mon-dossier";
        }
        Employe employe = dossierService.trouver(utilisateur.getEmploye().getIdEmp());
        model.addAttribute("pageTitle", "Mon dossier");
        model.addAttribute("employe", employe);
        model.addAttribute("documents", documentEmployeService.listerParEmploye(employe.getIdEmp()));
        model.addAttribute("changements", dossierService.changementsEmploye(employe.getIdEmp()));
        model.addAttribute("salaires", dossierService.historiqueSalaire(employe.getIdEmp()));
        model.addAttribute("carrieres", historiqueCarriereService.lister(employe.getIdEmp()));
        model.addAttribute("champs", ChampSensible.values());
        return "employe/mon-dossier";
    }

    @PostMapping("/mon-dossier/identite")
    public String modifierMonIdentite(@RequestParam(name = "adresse", required = false) String adresse,
                                      @RequestParam(name = "telephone", required = false) String telephone,
                                      @RequestParam(name = "contactUrgence", required = false) String contactUrgence,
                                      Authentication authentication, RedirectAttributes flash) {
        Utilisateur utilisateur = utilisateur(authentication);
        if (utilisateur.getEmploye() == null) {
            flash.addFlashAttribute("erreur", "Aucun dossier salarie rattache.");
            return "redirect:/mon-dossier";
        }
        Employe employe = dossierService.trouver(utilisateur.getEmploye().getIdEmp());
        enrichir(employe, adresse, telephone, contactUrgence);
        employeService.sauvegarder(employe);
        flash.addFlashAttribute("succes", "Coordonnees mises a jour.");
        return "redirect:/mon-dossier";
    }

    @PostMapping("/mon-dossier/changements")
    public String soumettreChangement(@RequestParam("champ") ChampSensible champ,
                                      @RequestParam("nouvelleValeur") String nouvelleValeur,
                                      @RequestParam(name = "motif", required = false) String motif,
                                      Authentication authentication, RedirectAttributes flash) {
        Utilisateur utilisateur = utilisateur(authentication);
        if (utilisateur.getEmploye() == null) {
            flash.addFlashAttribute("erreur", "Aucun dossier salarie rattache.");
            return "redirect:/mon-dossier";
        }
        try {
            dossierService.soumettreChangement(utilisateur.getEmploye().getIdEmp(), champ,
                    nouvelleValeur, motif, utilisateur);
            flash.addFlashAttribute("succes", "Demande de changement transmise a la RH.");
        } catch (IllegalStateException ex) {
            flash.addFlashAttribute("erreur", ex.getMessage());
        }
        return "redirect:/mon-dossier";
    }

    @PostMapping("/mon-dossier/documents")
    public String deposerDocument(@RequestParam("typeDocument") String typeDocument,
                                  @RequestParam(name = "dateEmission", required = false)
                                  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateEmission,
                                  @RequestParam(name = "dateValidite", required = false)
                                  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateValidite,
                                  @RequestParam(name = "commentaire", required = false) String commentaire,
                                  @RequestParam("fichier") MultipartFile fichier,
                                  Authentication authentication, RedirectAttributes flash) {
        Utilisateur utilisateur = utilisateur(authentication);
        if (utilisateur.getEmploye() == null) {
            flash.addFlashAttribute("erreur", "Aucun dossier salarie rattache.");
            return "redirect:/mon-dossier";
        }
        try {
            documentEmployeService.ajouter(utilisateur.getEmploye().getIdEmp(), typeDocument,
                    dateEmission, dateValidite, commentaire, fichier);
            flash.addFlashAttribute("succes", "Document depose.");
        } catch (IllegalStateException ex) {
            flash.addFlashAttribute("erreur", ex.getMessage());
        }
        return "redirect:/mon-dossier";
    }

    @PostMapping("/employes/{id}/identite")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMINISTRATEUR','ROLE_RH')")
    public String modifierIdentite(@PathVariable Integer id,
                                   @RequestParam(name = "dateNaissance", required = false)
                                   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateNaissance,
                                   @RequestParam(name = "sexe", required = false) String sexe,
                                   @RequestParam(name = "nationalite", required = false) String nationalite,
                                   @RequestParam(name = "etatCivil", required = false) String etatCivil,
                                   @RequestParam(name = "pieceIdentite", required = false) String pieceIdentite,
                                   @RequestParam(name = "adresse", required = false) String adresse,
                                   @RequestParam(name = "telephone", required = false) String telephone,
                                   @RequestParam(name = "contactUrgence", required = false) String contactUrgence,
                                   RedirectAttributes flash) {
        Employe employe = dossierService.trouver(id);
        employe.setDateNaissance(dateNaissance);
        employe.setSexe(texteOuNull(sexe));
        employe.setNationalite(texteOuNull(nationalite));
        employe.setEtatCivil(texteOuNull(etatCivil));
        employe.setPieceIdentite(texteOuNull(pieceIdentite));
        enrichir(employe, adresse, telephone, contactUrgence);
        employeService.sauvegarder(employe);
        flash.addFlashAttribute("succes", "Identite mise a jour.");
        return "redirect:/employes/" + id;
    }

    @PostMapping("/employes/{id}/salaire")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMINISTRATEUR','ROLE_RH')")
    public String changerSalaire(@PathVariable Integer id,
                                 @RequestParam("montant") Integer montant,
                                 @RequestParam(name = "dateEffet", required = false)
                                 @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateEffet,
                                 @RequestParam(name = "motif", required = false) String motif,
                                 Authentication authentication, RedirectAttributes flash) {
        try {
            dossierService.changerSalaire(id, montant, dateEffet, motif, utilisateur(authentication));
            flash.addFlashAttribute("succes", "Salaire mis a jour et historise.");
        } catch (IllegalStateException ex) {
            flash.addFlashAttribute("erreur", ex.getMessage());
        }
        return "redirect:/employes/" + id;
    }

    @GetMapping("/employes/changements")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMINISTRATEUR','ROLE_RH')")
    public String changements(Model model) {
        model.addAttribute("pageTitle", "Changements sensibles");
        model.addAttribute("changements", dossierService.changementsEnAttente());
        return "employe/changements";
    }

    @PostMapping("/employes/changements/{id}/traiter")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMINISTRATEUR','ROLE_RH')")
    public String traiterChangement(@PathVariable Integer id,
                                    @RequestParam("accepter") boolean accepter,
                                    @RequestParam(name = "commentaire", required = false) String commentaire,
                                    Authentication authentication, RedirectAttributes flash) {
        try {
            dossierService.validerChangement(id, accepter, commentaire, utilisateur(authentication));
            flash.addFlashAttribute("succes", accepter ? "Changement valide." : "Changement refuse.");
        } catch (IllegalStateException ex) {
            flash.addFlashAttribute("erreur", ex.getMessage());
        }
        return "redirect:/employes/changements";
    }

    private void enrichir(Employe employe, String adresse, String telephone, String contactUrgence) {
        if (adresse != null) {
            employe.setAdresse(texteOuNull(adresse));
        }
        if (telephone != null) {
            employe.setTelephone(texteOuNull(telephone));
        }
        if (contactUrgence != null) {
            employe.setContactUrgence(texteOuNull(contactUrgence));
        }
    }

    private String adresseIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String texteOuNull(String valeur) {
        return valeur == null || valeur.isBlank() ? null : valeur.trim();
    }

    private Utilisateur utilisateur(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof UtilisateurDetails details) {
            return details.getUtilisateur();
        }
        throw new AccesRefuseException("Vous devez etre connecte.");
    }
}
