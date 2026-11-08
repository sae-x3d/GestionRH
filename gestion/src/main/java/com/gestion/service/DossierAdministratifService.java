package com.gestion.service;

import com.gestion.enums.ChampSensible;
import com.gestion.enums.StatutChangementSensible;
import com.gestion.exception.AccesRefuseException;
import com.gestion.exception.RessourceIntrouvableException;
import com.gestion.model.ChangementSensible;
import com.gestion.model.Employe;
import com.gestion.model.HistoriqueSalaire;
import com.gestion.model.Utilisateur;
import com.gestion.repository.ChangementSensibleRepository;
import com.gestion.repository.EmployeRepository;
import com.gestion.repository.HistoriqueSalaireRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class DossierAdministratifService {

    private final EmployeRepository employeRepository;
    private final HistoriqueSalaireRepository historiqueSalaireRepository;
    private final ChangementSensibleRepository changementRepository;
    private final HistoriqueCarriereService historiqueCarriereService;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public Employe trouver(Integer idEmp) {
        return employeRepository.findById(idEmp)
                .orElseThrow(() -> new RessourceIntrouvableException("Employe introuvable : " + idEmp));
    }

    @Transactional(readOnly = true)
    public List<HistoriqueSalaire> historiqueSalaire(Integer idEmp) {
        return historiqueSalaireRepository.findByEmployeIdEmpOrderByDateEffetDesc(idEmp);
    }

    @Transactional
    public HistoriqueSalaire changerSalaire(Integer idEmp, Integer montant, LocalDate dateEffet,
                                            String motif, Utilisateur auteur) {
        if (montant == null || montant <= 0) {
            throw new IllegalStateException("Le montant du salaire doit etre positif.");
        }
        Employe employe = trouver(idEmp);
        String avant = String.valueOf(employe.getSalaireEmp());
        HistoriqueSalaire historique = historiqueSalaireRepository.save(HistoriqueSalaire.builder()
                .employe(employe)
                .montant(montant)
                .dateEffet(dateEffet == null ? LocalDate.now() : dateEffet)
                .motif(texteOuNull(motif))
                .auteur(auteur)
                .dateCreation(LocalDateTime.now())
                .build());
        employe.setSalaireEmp(montant);
        employeRepository.save(employe);
        auditService.enregistrer(auteur == null ? "system" : auteur.getUsername(), "SALAIRE",
                "/employes/" + idEmp, "Employe", String.valueOf(idEmp),
                "avant={salaire:" + avant + "}; apres={salaire:" + montant + ",dateEffet:"
                        + (dateEffet == null ? LocalDate.now() : dateEffet) + "}; motif=" + texteOuNull(motif), null);
        return historique;
    }

    @Transactional
    public ChangementSensible soumettreChangement(Integer idEmp, ChampSensible champ, String nouvelleValeur,
                                                  String motif, Utilisateur demandeur) {
        Employe employe = trouver(idEmp);
        if (champ == null || nouvelleValeur == null || nouvelleValeur.isBlank()) {
            throw new IllegalStateException("Le champ et la nouvelle valeur sont obligatoires.");
        }
        String ancienne = valeurCourante(employe, champ);
        ChangementSensible changement = changementRepository.save(ChangementSensible.builder()
                .employe(employe)
                .champ(champ)
                .ancienneValeur(ancienne)
                .nouvelleValeur(nouvelleValeur.trim())
                .statut(StatutChangementSensible.SOUMIS)
                .demandeur(demandeur)
                .motif(texteOuNull(motif))
                .dateSoumission(LocalDateTime.now())
                .build());
        auditService.enregistrer(demandeur == null ? "system" : demandeur.getUsername(), "DEMANDE",
                "/employes/" + idEmp + "/changements", "ChangementSensible",
                String.valueOf(changement.getIdChangement()),
                "avant={" + champ + ":" + ancienne + "}; apres={" + champ + ":"
                        + changement.getNouvelleValeur() + ",statut:" + changement.getStatut() + "}", null);
        return changement;
    }

    @Transactional(readOnly = true)
    public List<ChangementSensible> changementsEnAttente() {
        return changementRepository.findByStatutOrderByDateSoumissionAsc(StatutChangementSensible.SOUMIS);
    }

    @Transactional(readOnly = true)
    public List<ChangementSensible> changementsEmploye(Integer idEmp) {
        return changementRepository.findByEmployeIdEmpOrderByDateSoumissionDesc(idEmp);
    }

    @Transactional
    public ChangementSensible validerChangement(Integer idChangement, boolean accepter,
                                                String commentaire, Utilisateur validateur) {
        ChangementSensible changement = changementRepository.findById(idChangement)
                .orElseThrow(() -> new RessourceIntrouvableException("Changement introuvable."));
        if (changement.getStatut() != StatutChangementSensible.SOUMIS) {
            throw new IllegalStateException("Ce changement a deja ete traite.");
        }
        if (!estRhOuAdmin(validateur)) {
            throw new AccesRefuseException("Seule la RH peut valider un changement sensible.");
        }
        String statutAvant = changement.getStatut().name();
        String valeurAvant = valeurCourante(changement.getEmploye(), changement.getChamp());
        if (accepter) {
            Employe employe = changement.getEmploye();
            appliquer(employe, changement.getChamp(), changement.getNouvelleValeur());
            employeRepository.save(employe);
            changement.setStatut(StatutChangementSensible.VALIDE);
        } else {
            changement.setStatut(StatutChangementSensible.REFUSE);
        }
        changement.setValidateur(validateur);
        changement.setDateValidation(LocalDateTime.now());
        if (commentaire != null && !commentaire.isBlank()) {
            changement.setMotif(commentaire.trim());
        }
        auditService.enregistrer(validateur == null ? "system" : validateur.getUsername(), "VALIDATION",
                "/employes/changements/" + idChangement, "ChangementSensible",
                String.valueOf(idChangement),
                "avant={statut:" + statutAvant + "," + changement.getChamp() + ":" + valeurAvant
                        + "}; apres={statut:" + changement.getStatut() + "," + changement.getChamp() + ":"
                        + valeurCourante(changement.getEmploye(), changement.getChamp()) + "}", null);
        return changementRepository.save(changement);
    }

    @Transactional
    public void enregistrerCarriereInitiale(Employe employe, Utilisateur auteur) {
        historiqueCarriereService.enregistrer(employe, employe.getPoste(), employe.getDepartement(),
                null, employe.getDateEmbauche() == null ? LocalDate.now()
                        : employe.getDateEmbauche().toLocalDate(),
                "EMBAUCHE", auteur);
    }

    @Transactional
    public void enregistrerAffectation(Employe employe, String motif, Utilisateur auteur) {
        historiqueCarriereService.enregistrer(employe, employe.getPoste(), employe.getDepartement(),
                null, LocalDate.now(), motif, auteur);
    }

    public boolean peutVoirEmploye(Employe employe, Utilisateur utilisateur) {
        if (employe == null || utilisateur == null) {
            return false;
        }
        String role = role(utilisateur);
        if (List.of("ADMINISTRATEUR", "RH", "DIRECTION", "FINANCE", "PAIE", "IT").contains(role)) {
            return true;
        }
        if ("MANAGER".equals(role)) {
            return employe.getDepartement() != null && utilisateur.getEmploye() != null
                    && employe.getDepartement().getIdDept().equals(
                    utilisateur.getEmploye().getDepartement() == null ? null
                            : utilisateur.getEmploye().getDepartement().getIdDept());
        }
        return utilisateur.getEmploye() != null
                && employe.getIdEmp().equals(utilisateur.getEmploye().getIdEmp());
    }

    public boolean peutVoirSalaire(Employe employe, Utilisateur utilisateur) {
        String role = role(utilisateur);
        if (List.of("ADMINISTRATEUR", "RH", "FINANCE", "PAIE", "DIRECTION").contains(role)) {
            return true;
        }
        return estProprietaire(employe, utilisateur);
    }

    public boolean peutVoirRib(Employe employe, Utilisateur utilisateur) {
        String role = role(utilisateur);
        if (List.of("ADMINISTRATEUR", "RH", "FINANCE", "PAIE", "DIRECTION").contains(role)) {
            return true;
        }
        return estProprietaire(employe, utilisateur);
    }

    public boolean peutModifierIdentite(Employe employe, Utilisateur utilisateur) {
        String role = role(utilisateur);
        return List.of("ADMINISTRATEUR", "RH").contains(role) || estProprietaire(employe, utilisateur);
    }

    public boolean peutGererDossier(Utilisateur utilisateur) {
        return List.of("ADMINISTRATEUR", "RH").contains(role(utilisateur));
    }

    public boolean estRhOuAdmin(Utilisateur utilisateur) {
        return peutGererDossier(utilisateur);
    }

    public boolean estProprietaire(Employe employe, Utilisateur utilisateur) {
        return employe != null && utilisateur != null && utilisateur.getEmploye() != null
                && employe.getIdEmp().equals(utilisateur.getEmploye().getIdEmp());
    }

    private void appliquer(Employe employe, ChampSensible champ, String valeur) {
        switch (champ) {
            case RIB -> employe.setRib(valeur);
            case ADRESSE -> employe.setAdresse(valeur);
            case TELEPHONE -> employe.setTelephone(valeur);
            case ETAT_CIVIL -> employe.setEtatCivil(valeur);
            case PIECE_IDENTITE -> employe.setPieceIdentite(valeur);
            case CONTACT_URGENCE -> employe.setContactUrgence(valeur);
        }
    }

    private String valeurCourante(Employe employe, ChampSensible champ) {
        return switch (champ) {
            case RIB -> employe.getRib();
            case ADRESSE -> employe.getAdresse();
            case TELEPHONE -> employe.getTelephone();
            case ETAT_CIVIL -> employe.getEtatCivil();
            case PIECE_IDENTITE -> employe.getPieceIdentite();
            case CONTACT_URGENCE -> employe.getContactUrgence();
        };
    }

    private String role(Utilisateur utilisateur) {
        if (utilisateur == null || utilisateur.getRole() == null) {
            return "";
        }
        return Normalizer.normalize(utilisateur.getRole().getNomRole(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").replace("ROLE_", "")
                .toUpperCase(Locale.ROOT).trim();
    }

    private String texteOuNull(String valeur) {
        return valeur == null || valeur.isBlank() ? null : valeur.trim();
    }
}
