package com.gestion.controller;

import com.gestion.exception.AccesRefuseException;
import com.gestion.model.DocumentEmploye;
import com.gestion.model.Employe;
import com.gestion.model.Utilisateur;
import com.gestion.service.DocumentEmployeService;
import com.gestion.service.EmployeService;
import com.gestion.service.UtilisateurDetails;
import com.gestion.service.ValidationWorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

@Controller
@RequestMapping("/employes/{idEmp}/documents")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class DocumentEmployeController {

    private final DocumentEmployeService documentService;
    private final EmployeService employeService;
    private final ValidationWorkflowService workflowService;

    @GetMapping
    public String liste(@PathVariable Integer idEmp, Model model, Authentication authentication) {
        Employe employe = employeService.trouver(idEmp);
        verifierAcces(employe, authentication);
        model.addAttribute("pageTitle", "Documents du salarie");
        model.addAttribute("employe", employe);
        model.addAttribute("documents", documentService.listerParEmploye(idEmp));
        model.addAttribute("demandeur", utilisateur(authentication));
        return "employe/documents";
    }

    @PostMapping
    public String ajouter(@PathVariable Integer idEmp,
                          @RequestParam("typeDocument") String typeDocument,
                          @RequestParam(name = "dateEmission", required = false)
                          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateEmission,
                          @RequestParam(name = "dateValidite", required = false)
                          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateValidite,
                          @RequestParam(name = "commentaire", required = false) String commentaire,
                          @RequestParam("fichier") MultipartFile fichier,
                          Authentication authentication, RedirectAttributes flash) {
        try {
            Employe employe = employeService.trouver(idEmp);
            verifierAcces(employe, authentication);
            documentService.ajouter(idEmp, typeDocument, dateEmission, dateValidite, commentaire, fichier);
            flash.addFlashAttribute("succes", "Document ajoute a la GED du salarie.");
        } catch (AccesRefuseException | IllegalStateException ex) {
            flash.addFlashAttribute("erreur", ex.getMessage());
        }
        return "redirect:/employes/" + idEmp + "/documents";
    }

    @GetMapping("/{idDocument}")
    public ResponseEntity<ByteArrayResource> telecharger(@PathVariable Integer idEmp,
                                                         @PathVariable Integer idDocument,
                                                         Authentication authentication) {
        Employe employe = employeService.trouver(idEmp);
        verifierAcces(employe, authentication);
        DocumentEmploye document = documentService.trouver(idDocument);
        Path chemin = documentService.chemin(document);
        try {
            byte[] contenu = Files.readAllBytes(chemin);
            MediaType type = MediaTypeFactory.getMediaType(document.getNomOriginal())
                    .orElse(MediaType.APPLICATION_OCTET_STREAM);
            return ResponseEntity.ok()
                    .contentType(type)
                    .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                            .filename(document.getNomOriginal()).build().toString())
                    .body(new ByteArrayResource(contenu));
        } catch (IOException ex) {
            throw new com.gestion.exception.RessourceIntrouvableException("Document introuvable.");
        }
    }

    private void verifierAcces(Employe employe, Authentication authentication) {
        Utilisateur utilisateur = utilisateur(authentication);
        if (workflowService.estAdministrateur(utilisateur) || workflowService.estRh(utilisateur)) {
            return;
        }
        if (utilisateur.getEmploye() != null
                && employe.getIdEmp().equals(utilisateur.getEmploye().getIdEmp())) {
            return;
        }
        throw new AccesRefuseException("Vous n'avez pas acces a ces documents.");
    }

    private Utilisateur utilisateur(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof UtilisateurDetails details) {
            return details.getUtilisateur();
        }
        throw new AccesRefuseException("Vous devez etre connecte.");
    }
}
