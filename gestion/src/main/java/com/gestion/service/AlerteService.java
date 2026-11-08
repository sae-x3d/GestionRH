package com.gestion.service;

import com.gestion.model.Contrat;
import com.gestion.model.DocumentEmploye;
import com.gestion.model.Employe;
import com.gestion.model.Utilisateur;
import com.gestion.repository.ContratRepository;
import com.gestion.repository.DocumentEmployeRepository;
import com.gestion.repository.DocumentRequisRepository;
import com.gestion.repository.EmployeRepository;
import com.gestion.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.gestion.model.DocumentRequis;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AlerteService {

    private static final Logger log = LoggerFactory.getLogger(AlerteService.class);

    private final ContratRepository contratRepository;
    private final DocumentEmployeRepository documentEmployeRepository;
    private final DocumentRequisRepository documentRequisRepository;
    private final EmployeRepository employeRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final NotificationService notificationService;

    @Value("${app.alerte.contrat-jours:30}")
    private int joursContrat;

    @Value("${app.alerte.essai-jours:15}")
    private int joursEssai;

    @Value("${app.alerte.document-jours:30}")
    private int joursDocument;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initialiserDocumentsRequis() {
        if (documentRequisRepository.count() == 0) {
            documentRequisRepository.save(DocumentRequis.builder().typeDocument("PIECE_IDENTITE").actif(true).build());
            documentRequisRepository.save(DocumentRequis.builder().typeDocument("RIB").actif(true).build());
            documentRequisRepository.save(DocumentRequis.builder().typeDocument("CONTRAT_SIGNE").actif(true).build());
        }
    }

    @Scheduled(cron = "0 0 7 * * *")
    @Transactional
    public void envoyerAlertes() {
        alerterFinsDeContrat();
        alerterFinsDePeriodeEssai();
        alerterDocumentsExpirants();
        alerterDossiersIncomplets();
    }

    @Transactional
    public int alerterDossiersIncomplets() {
        List<Utilisateur> rh = utilisateurRepository.findAll().stream()
                .filter(u -> u.getRole() != null && u.getRole().getNomRole() != null
                        && "RH".equalsIgnoreCase(u.getRole().getNomRole().replace("ROLE_", "")))
                .toList();
        if (rh.isEmpty()) {
            return 0;
        }
        int envoyees = 0;
        List<String> documentsRequis = documentRequisRepository.findByActifTrueOrderByTypeDocumentAsc()
                .stream().map(r -> r.getTypeDocument()).toList();
        for (Employe employe : employeRepository.findByDateDepartIsNullOrderByNomEmpAsc()) {
            StringBuilder manquants = new StringBuilder();
            for (String typeDocument : documentsRequis) {
                if (!documentEmployeRepository.existsByEmployeIdEmpAndTypeDocumentIgnoreCase(
                        employe.getIdEmp(), typeDocument)) manquants.append(typeDocument).append(", ");
            }
            if (manquants.isEmpty()) {
                continue;
            }
            String liste = manquants.substring(0, manquants.length() - 2);
            for (Utilisateur destinataire : rh) {
                notificationService.envoyer(destinataire, "DOSSIER",
                        "Dossier incomplet pour " + employe.getNomEmp() + " : " + liste + ".",
                        "/employes/" + employe.getIdEmp());
                envoyees++;
            }
        }
        log.info("Alertes dossier incomplet envoyees : {}", envoyees);
        return envoyees;
    }

    @Transactional
    public int alerterFinsDeContrat() {
        LocalDate aujourdHui = LocalDate.now();
        List<Contrat> contrats = contratRepository.findByStatutInAndDateFinBetweenAndAlerteFinEnvoyeeFalse(
                List.of("ACCEPTE", "EN_COURS"), aujourdHui, aujourdHui.plusDays(joursContrat));
        int envoyees = 0;
        for (Contrat contrat : contrats) {
            Utilisateur destinataire = destinataire(contrat);
            if (destinataire != null) {
                notificationService.envoyer(destinataire, "CONTRAT",
                        "Votre contrat se termine le " + contrat.getDateFin()
                                + ". Contactez le service RH pour son renouvellement.",
                        "/contrats/" + contrat.getIdContrat());
                envoyees++;
            }
            contrat.setAlerteFinEnvoyee(true);
            contratRepository.save(contrat);
        }
        log.info("Alertes fin de contrat envoyees : {}", envoyees);
        return envoyees;
    }

    @Transactional
    public int alerterFinsDePeriodeEssai() {
        LocalDate aujourdHui = LocalDate.now();
        List<Contrat> contrats = contratRepository.findByStatutInAndDateFinEssaiBetweenAndAlerteEssaiEnvoyeeFalse(
                List.of("ACCEPTE", "EN_COURS"), aujourdHui, aujourdHui.plusDays(joursEssai));
        int envoyees = 0;
        for (Contrat contrat : contrats) {
            Utilisateur destinataire = destinataire(contrat);
            if (destinataire != null) {
                notificationService.envoyer(destinataire, "CONTRAT",
                        "Votre periode d'essai se termine le " + contrat.getDateFinEssai()
                                + ". Un point de suivi sera organise.",
                        "/contrats/" + contrat.getIdContrat());
                envoyees++;
            }
            contrat.setAlerteEssaiEnvoyee(true);
            contratRepository.save(contrat);
        }
        log.info("Alertes fin de periode d'essai envoyees : {}", envoyees);
        return envoyees;
    }

    @Transactional
    public int alerterDocumentsExpirants() {
        LocalDate aujourdHui = LocalDate.now();
        List<DocumentEmploye> documents =
                documentEmployeRepository.findByDateValiditeBetweenAndAlerteExpirationEnvoyeeFalse(
                        aujourdHui, aujourdHui.plusDays(joursDocument));
        int envoyees = 0;
        for (DocumentEmploye document : documents) {
            Utilisateur destinataire = document.getEmploye() == null ? null
                    : utilisateurRepository.findByEmployeIdEmp(document.getEmploye().getIdEmp())
                    .orElse(null);
            if (destinataire != null) {
                notificationService.envoyer(destinataire, "DOCUMENT",
                        "Le document \"" + document.getTypeDocument() + "\" expire le "
                                + document.getDateValidite() + ". Merci de fournir un document a jour.",
                        "/employes/" + document.getEmploye().getIdEmp() + "/documents");
                envoyees++;
            }
            document.setAlerteExpirationEnvoyee(true);
            documentEmployeRepository.save(document);
        }
        log.info("Alertes d'expiration de documents envoyees : {}", envoyees);
        return envoyees;
    }

    private Utilisateur destinataire(Contrat contrat) {
        if (contrat == null) {
            return null;
        }
        if (contrat.getCandidature() != null && contrat.getCandidature().getUtilisateur() != null) {
            return contrat.getCandidature().getUtilisateur();
        }
        if (contrat.getEmploye() != null) {
            return utilisateurRepository.findByEmployeIdEmp(contrat.getEmploye().getIdEmp()).orElse(null);
        }
        return null;
    }
}
