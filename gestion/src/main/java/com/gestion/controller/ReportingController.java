package com.gestion.controller;

import com.gestion.service.ReportingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Controller
@PreAuthorize("hasAnyAuthority('ROLE_ADMINISTRATEUR','ROLE_RH','ROLE_FINANCE')")
@RequiredArgsConstructor
public class ReportingController {

    private final ReportingService reportingService;

    @GetMapping("/reporting")
    public String tableau(Model model) {
        model.addAttribute("pageTitle", "Reporting RH");
        model.addAttribute("totalActifs", reportingService.totalEmployesActifs());
        model.addAttribute("delaiMoyen", reportingService.delaiMoyenRecrutementJours());
        model.addAttribute("effectifsDepartement", reportingService.effectifsParDepartement());
        model.addAttribute("effectifsPoste", reportingService.effectifsParPoste());
        model.addAttribute("coutsDepartement", reportingService.coutsAnnuelsParDepartement());
        model.addAttribute("candidaturesStatut", reportingService.candidaturesParStatut());
        model.addAttribute("absenteismeDepartement", reportingService.absenteismeParDepartement());
        model.addAttribute("heuresSupDepartement", reportingService.heuresSupParDepartement());
        return "reporting/tableau";
    }

    @GetMapping("/reporting/export.csv")
    public ResponseEntity<byte[]> exporter() {
        StringBuilder csv = new StringBuilder("Indicateur;Valeur\n");
        csv.append("Effectif actif;").append(reportingService.totalEmployesActifs()).append('\n');
        csv.append("Delai moyen de recrutement (jours);")
                .append(String.format("%.1f", reportingService.delaiMoyenRecrutementJours())).append('\n');
        ajouter(csv, "Effectif par departement", reportingService.effectifsParDepartement());
        ajouter(csv, "Effectif par poste", reportingService.effectifsParPoste());
        ajouter(csv, "Cout annuel par departement", reportingService.coutsAnnuelsParDepartement());
        ajouter(csv, "Candidatures par statut", reportingService.candidaturesParStatut());
        csv.append('\n').append("Absenteisme par departement (%);\n");
        reportingService.absenteismeParDepartement().forEach((cle, valeur) ->
                csv.append(cle).append(';').append(String.format("%.1f", valeur)).append('\n'));
        csv.append('\n').append("Heures supplementaires par departement (h);\n");
        reportingService.heuresSupParDepartement().forEach((cle, valeur) ->
                csv.append(cle).append(';').append(valeur).append('\n'));
        byte[] contenu = csv.toString().getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("reporting-rh.csv").build().toString())
                .body(contenu);
    }

    private void ajouter(StringBuilder csv, String titre, Map<String, Long> donnees) {
        csv.append('\n').append(titre).append(";\n");
        donnees.forEach((cle, valeur) -> csv.append(cle).append(';').append(valeur).append('\n'));
    }
}
