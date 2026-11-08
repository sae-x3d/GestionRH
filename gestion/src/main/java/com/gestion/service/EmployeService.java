package com.gestion.service;

import com.gestion.enums.StatutPoste;
import com.gestion.model.Candidature;
import com.gestion.model.Demande;
import com.gestion.model.Departement;
import com.gestion.model.Employe;
import com.gestion.model.Poste;
import com.gestion.model.Utilisateur;
import com.gestion.repository.DemandeRepository;
import com.gestion.repository.DepartementRepository;
import com.gestion.repository.EmployeRepository;
import com.gestion.repository.PosteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeService {

    public static final String STATUT_DEMANDE_POURVUE = "POURVUE";

    private final EmployeRepository employeRepository;
    private final PosteRepository posteRepository;
    private final DepartementRepository departementRepository;
    private final DemandeRepository demandeRepository;
    private final DemandeStatusService demandeStatusService;
    private final DossierAdministratifService dossierAdministratifService;

    @Transactional(readOnly = true)
    public List<Employe> listerActifs() {
        return employeRepository.findByDateDepartIsNullOrderByNomEmpAsc();
    }

    @Transactional(readOnly = true)
    public List<Employe> listerTous() {
        return employeRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Employe trouver(Integer idEmp) {
        return employeRepository.findById(idEmp)
                .orElseThrow(() -> new com.gestion.exception.RessourceIntrouvableException(
                        "Employe introuvable : " + idEmp));
    }

    @Transactional
    public Employe sauvegarder(Employe employe) {
        return employeRepository.save(employe);
    }

    @Transactional
    public String genererMatricule() {
        String prefixe = "EMP-" + LocalDate.now().getYear() + "-";
        long sequence = employeRepository.count() + 1;
        String candidat = prefixe + String.format("%04d", sequence);
        while (employeRepository.existsByMatricule(candidat)) {
            sequence++;
            candidat = prefixe + String.format("%04d", sequence);
        }
        return candidat;
    }

    @Transactional
    public Employe preparerNouvelEmploye(Employe employe, Candidature candidature, Utilisateur utilisateur) {
        if (employe == null) {
            throw new IllegalStateException("La fiche employe est requise.");
        }
        if (employe.getMatricule() == null || employe.getMatricule().isBlank()) {
            employe.setMatricule(genererMatricule());
        }
        if (employe.getCandidature() == null && candidature != null) {
            employe.setCandidature(candidature);
        }
        if ((employe.getEmail() == null || employe.getEmail().isBlank()) && utilisateur != null) {
            if (utilisateur.getEmail() != null && !utilisateur.getEmail().isBlank()) {
                employe.setEmail(utilisateur.getEmail().trim());
            }
        }
        if (employe.getStatut() == null || employe.getStatut().isBlank()) {
            employe.setStatut("ACTIF");
        }
        return employeRepository.save(employe);
    }

    @Transactional
    public void finaliserEmbauche(Employe employe, Candidature candidature) {
        finaliserEmbauche(employe, candidature, null);
    }

    @Transactional
    public void finaliserEmbauche(Employe employe, Candidature candidature, Utilisateur auteur) {
        if (employe == null) {
            return;
        }
        Poste poste = employe.getPoste();
        Departement departement = employe.getDepartement();
        Demande demande = candidature != null && candidature.getOffre() != null
                ? candidature.getOffre().getDemande() : null;

        if (departement != null) {
            long actifs = employeRepository.countByDepartementIdDeptAndDateDepartIsNull(
                    departement.getIdDept());
            departement.setEffectif((int) actifs);
            if (employe.getSalaireEmp() != null) {
                int cout = departement.getBudgetConsomme() == null ? 0 : departement.getBudgetConsomme();
                departement.setBudgetConsomme(cout + employe.getSalaireEmp() * 12);
            }
            departementRepository.save(departement);
        }

        if (poste != null) {
            rafraichirStatutPoste(poste);
        }

        if (demande != null) {
            demandeStatusService.trouverParLibelle(STATUT_DEMANDE_POURVUE).ifPresent(statut -> {
                demande.setDemandeStatus(statut);
                demandeRepository.save(demande);
            });
        }

        if (employe.getSalaireEmp() != null && employe.getSalaireEmp() > 0) {
            dossierAdministratifService.changerSalaire(employe.getIdEmp(), employe.getSalaireEmp(),
                    employe.getDateEmbauche() == null ? LocalDate.now()
                            : employe.getDateEmbauche().toLocalDate(),
                    "Salaire d'embauche", auteur);
        }
        dossierAdministratifService.enregistrerCarriereInitiale(employe, auteur);
    }

    @Transactional
    public void rafraichirStatutPoste(Poste poste) {
        if (poste == null || poste.getStatut() == StatutPoste.GELE) {
            return;
        }
        long actifs = employeRepository.countByPosteIdPosteAndDateDepartIsNull(poste.getIdPoste());
        int capacite = poste.getEffectif() == null || poste.getEffectif() <= 0 ? 1 : poste.getEffectif();
        poste.setStatut(actifs >= capacite ? StatutPoste.OCCUPE : StatutPoste.VACANT);
        posteRepository.save(poste);
    }

    @Transactional
    public void changerStatutPoste(Integer idPoste, StatutPoste statut) {
        Poste poste = posteRepository.findById(idPoste)
                .orElseThrow(() -> new com.gestion.exception.RessourceIntrouvableException(
                        "Poste introuvable : " + idPoste));
        poste.setStatut(statut);
        posteRepository.save(poste);
    }

    @Transactional
    public void definirEnveloppeBudgetaire(Integer idDepartement, Integer enveloppe, Integer effectifCible) {
        Departement departement = departementRepository.findById(idDepartement)
                .orElseThrow(() -> new com.gestion.exception.RessourceIntrouvableException(
                        "Departement introuvable : " + idDepartement));
        departement.setEnveloppeBudgetaire(enveloppe);
        departement.setEffectif(effectifCible != null ? effectifCible
                : (int) employeRepository.countByDepartementIdDeptAndDateDepartIsNull(idDepartement));
        departementRepository.save(departement);
    }
}
