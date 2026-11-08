package com.gestion.service;

import com.gestion.model.JournalAudit;
import com.gestion.repository.JournalAuditRepository;
import com.gestion.model.Employe;
import com.gestion.repository.EmployeRepository;
import com.gestion.repository.ContratRepository;
import com.gestion.repository.DemandeCongeRepository;
import com.gestion.repository.AbsenceRepository;
import com.gestion.repository.HeureSupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final JournalAuditRepository journalAuditRepository;
    private final EmployeRepository employeRepository;
    private final ContratRepository contratRepository;
    private final DemandeCongeRepository demandeCongeRepository;
    private final AbsenceRepository absenceRepository;
    private final HeureSupRepository heureSupRepository;

    @Transactional(readOnly = true)
    public Map<String, String> instantaneEmploye(Integer id) {
        Employe e = employeRepository.findById(id).orElse(null);
        if (e == null) return Map.of();
        Map<String, String> valeurs = new LinkedHashMap<>();
        valeurs.put("nom", e.getNomEmp()); valeurs.put("email", e.getEmail());
        valeurs.put("statut", e.getStatut()); valeurs.put("adresse", e.getAdresse());
        valeurs.put("telephone", e.getTelephone()); valeurs.put("contactUrgence", e.getContactUrgence());
        valeurs.put("dateNaissance", String.valueOf(e.getDateNaissance()));
        valeurs.put("pieceIdentite", e.getPieceIdentite()); valeurs.put("rib", e.getRib());
        valeurs.put("salaire", String.valueOf(e.getSalaireEmp()));
        valeurs.put("departement", e.getDepartement() == null ? null : e.getDepartement().getNomDept());
        valeurs.put("poste", e.getPoste() == null ? null : e.getPoste().getNomPoste());
        valeurs.put("regime", e.getRegimeTravail() == null ? null : e.getRegimeTravail().getLibelle());
        return valeurs;
    }

    @Transactional(readOnly = true)
    public Map<String, String> instantanePourChemin(String chemin) {
        String[] segments = chemin.split("/");
        if (segments.length >= 5 && "candidatures".equals(segments[1])
                && "contrat".equals(segments[segments.length - 1]) && segments[3].matches("\\d+")) {
            return contratRepository.findFirstByCandidatureIdCandidatureOrderByDateGenerationDesc(
                    Integer.valueOf(segments[3])).map(this::instantaneContrat)
                    .orElse(Map.of("etat", "absent"));
        }
        for (int i = 1; i < segments.length; i++) {
            if (!segments[i].matches("\\d+")) continue;
            Integer id = Integer.valueOf(segments[i]);
            if (segments.length > 2 && "employes".equals(segments[1]) && segments[2].matches("\\d+"))
                return instantaneEmploye(Integer.valueOf(segments[2]));
            if (segments.length > 1 && "contrats".equals(segments[1])) {
                return contratRepository.findById(id).map(this::instantaneContrat).orElse(Map.of());
            }
            if (segments.length > 1 && "conges".equals(segments[1])) {
                return demandeCongeRepository.findById(id).map(d -> Map.ofEntries(
                        Map.entry("employe", String.valueOf(d.getEmploye().getIdEmp())),
                        Map.entry("type", String.valueOf(d.getTypeConge().getCode())),
                        Map.entry("dateDebut", String.valueOf(d.getDateDebut())),
                        Map.entry("dateFin", String.valueOf(d.getDateFin())),
                        Map.entry("nombreJours", String.valueOf(d.getNombreJours())),
                        Map.entry("statut", String.valueOf(d.getStatut())),
                        Map.entry("commentaireManager", String.valueOf(d.getCommentaireManager())),
                        Map.entry("commentaireRh", String.valueOf(d.getCommentaireRh())),
                        Map.entry("commentaireDirection", String.valueOf(d.getCommentaireDirection())))).orElse(Map.of());
            }
            if (segments.length > 1 && "absences".equals(segments[1])) {
                return absenceRepository.findById(id).map(a -> Map.ofEntries(
                        Map.entry("employe", String.valueOf(a.getEmploye().getIdEmp())),
                        Map.entry("type", String.valueOf(a.getTypeAbsence())),
                        Map.entry("dateDebut", String.valueOf(a.getDateDebut())),
                        Map.entry("dateFin", String.valueOf(a.getDateFin())),
                        Map.entry("statut", String.valueOf(a.getStatut())),
                        Map.entry("validePar", String.valueOf(a.getValidePar() == null ? null : a.getValidePar().getUsername())),
                        Map.entry("dateValidation", String.valueOf(a.getDateValidation())))).orElse(Map.of());
            }
            if (segments.length > 1 && "heures-sup".equals(segments[1])) {
                return heureSupRepository.findById(id).map(h -> Map.ofEntries(
                        Map.entry("employe", String.valueOf(h.getEmploye().getIdEmp())),
                        Map.entry("date", String.valueOf(h.getDateHeureSup())),
                        Map.entry("minutes", String.valueOf(h.getMinutes())),
                        Map.entry("majoration", String.valueOf(h.getTauxMajoration())),
                        Map.entry("statut", String.valueOf(h.getStatut())),
                        Map.entry("motif", String.valueOf(h.getMotif())),
                        Map.entry("validation", String.valueOf(h.getDateValidation())))).orElse(Map.of());
            }
        }
        return Map.of();
    }

    private Map<String, String> instantaneContrat(com.gestion.model.Contrat contrat) {
        return Map.ofEntries(Map.entry("numero", String.valueOf(contrat.getNumeroContrat())),
                Map.entry("type", String.valueOf(contrat.getTypeContrat())),
                Map.entry("fonction", String.valueOf(contrat.getFonction())),
                Map.entry("salaire", String.valueOf(contrat.getSalaire())),
                Map.entry("dateEffet", String.valueOf(contrat.getDateEffet())),
                Map.entry("dateFin", String.valueOf(contrat.getDateFin())),
                Map.entry("statut", String.valueOf(contrat.getStatut())),
                Map.entry("commentaire", String.valueOf(contrat.getCommentaire())));
    }

    @Transactional
    public void enregistrer(String username, String methode, String chemin,
                            String entite, String idEntite, String details, String adresseIp) {
        JournalAudit audit = JournalAudit.builder()
                .username(username)
                .methode(methode)
                .chemin(chemin)
                .entite(entite)
                .idEntite(idEntite)
                .details(details)
                .adresseIp(adresseIp)
                .dateAction(LocalDateTime.now())
                .build();
        journalAuditRepository.save(audit);
    }

    @Transactional(readOnly = true)
    public List<JournalAudit> lister() {
        return journalAuditRepository.findAllByOrderByDateActionDesc();
    }

    @Transactional(readOnly = true)
    public List<JournalAudit> listerParUtilisateur(String username) {
        return journalAuditRepository.findByUsernameOrderByDateActionDesc(username);
    }
}
