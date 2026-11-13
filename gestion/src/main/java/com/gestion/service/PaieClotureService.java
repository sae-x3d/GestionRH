package com.gestion.service;

import com.gestion.enums.StatutDemandeConge;
import com.gestion.enums.StatutHeureSup;
import com.gestion.enums.StatutPeriodePaie;
import com.gestion.enums.TypeVariablePaie;
import com.gestion.exception.RessourceIntrouvableException;
import com.gestion.model.DemandeConge;
import com.gestion.model.HeureSup;
import com.gestion.model.PeriodePaie;
import com.gestion.model.Utilisateur;
import com.gestion.model.VariablePaie;
import com.gestion.repository.DemandeCongeRepository;
import com.gestion.repository.HeureSupRepository;
import com.gestion.repository.PeriodePaieRepository;
import com.gestion.repository.VariablePaieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaieClotureService {

    private final PeriodePaieRepository periodeRepository;
    private final VariablePaieRepository variableRepository;
    private final HeureSupRepository heureSupRepository;
    private final DemandeCongeRepository demandeCongeRepository;
    private final AuditService auditService;

    @Transactional
    public PeriodePaie obtenirOuCreer(Integer annee, Integer mois) {
        if (annee == null || mois == null || mois < 1 || mois > 12) {
            throw new IllegalStateException("La periode de paie est invalide.");
        }
        return periodeRepository.findByAnneeAndMois(annee, mois)
                .orElseGet(() -> {
                    LocalDate debut = LocalDate.of(annee, mois, 1);
                    return periodeRepository.save(PeriodePaie.builder()
                            .annee(annee)
                            .mois(mois)
                            .dateDebut(debut)
                            .dateFin(debut.withDayOfMonth(debut.lengthOfMonth()))
                            .statut(StatutPeriodePaie.OUVERTE)
                            .dateOuverture(LocalDateTime.now())
                            .build());
                });
    }

    @Transactional(readOnly = true)
    public List<PeriodePaie> periodes() {
        return periodeRepository.findAllByOrderByAnneeDescMoisDesc();
    }

    @Transactional(readOnly = true)
    public PeriodePaie periode(Integer idPeriode) {
        return periodeRepository.findById(idPeriode)
                .orElseThrow(() -> new RessourceIntrouvableException(
                        "Periode de paie introuvable : " + idPeriode));
    }

    @Transactional(readOnly = true)
    public List<VariablePaie> variables(Integer idPeriode) {
        return variableRepository
                .findByPeriodePaieIdPeriodePaieOrderByEmployeNomEmpAscTypeVariableAsc(idPeriode);
    }

    @Transactional
    public int genererVariables(Integer annee, Integer mois) {
        PeriodePaie periode = obtenirOuCreer(annee, mois);
        if (periode.getStatut() == StatutPeriodePaie.CLOTUREE) {
            throw new IllegalStateException("Cette periode est cloturee : les variables ne peuvent plus etre regenerees.");
        }
        variableRepository.deleteByPeriodePaieIdPeriodePaie(periode.getIdPeriodePaie());
        int total = 0;
        for (HeureSup heureSup : heureSupRepository
                .findByStatutAndPeriodePaieIsNullOrderByDateHeureSupAsc(StatutHeureSup.VALIDEE)) {
            if (heureSup.getDateHeureSup().isBefore(periode.getDateDebut())
                    || heureSup.getDateHeureSup().isAfter(periode.getDateFin())) {
                continue;
            }
            heureSup.setPeriodePaie(periode);
            heureSupRepository.save(heureSup);
            BigDecimal heures = BigDecimal.valueOf(heureSup.getMinutes())
                    .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
            variableRepository.save(VariablePaie.builder()
                    .periodePaie(periode)
                    .employe(heureSup.getEmploye())
                    .typeVariable(TypeVariablePaie.HEURE_SUP)
                    .libelle("Heures supplementaires " + heureSup.getTauxMajoration() + "%")
                    .quantite(heures)
                    .reference("HS-" + heureSup.getIdHeureSup())
                    .dateCreation(LocalDateTime.now())
                    .build());
            total++;
        }
        for (DemandeConge demande : demandeCongeRepository
                .findByStatutAndDateDebutLessThanEqualAndDateFinGreaterThanEqualOrderByDateDebutAsc(
                        StatutDemandeConge.VALIDEE_RH, periode.getDateFin(), periode.getDateDebut())) {
            if (demande.getTypeConge() == null
                    || !"SANS_SOLDE".equalsIgnoreCase(demande.getTypeConge().getCode())) {
                continue;
            }
            variableRepository.save(VariablePaie.builder()
                    .periodePaie(periode)
                    .employe(demande.getEmploye())
                    .typeVariable(TypeVariablePaie.ABSENCE)
                    .libelle("Absence sans solde")
                    .quantite(demande.getNombreJours())
                    .reference("ABS-" + demande.getIdDemandeConge())
                    .dateCreation(LocalDateTime.now())
                    .build());
            total++;
        }
        return total;
    }

    @Transactional
    public PeriodePaie cloturer(Integer annee, Integer mois, Utilisateur utilisateur) {
        PeriodePaie periode = obtenirOuCreer(annee, mois);
        if (periode.getStatut() == StatutPeriodePaie.CLOTUREE) {
            throw new IllegalStateException("Cette periode est deja cloturee.");
        }
        String avant = "{statut:" + periode.getStatut() + ",dateCloture:" + periode.getDateCloture() + "}";
        genererVariables(annee, mois);
        for (HeureSup heureSup : heureSupRepository
                .findByPeriodePaieIdPeriodePaieOrderByEmployeNomEmpAsc(periode.getIdPeriodePaie())) {
            if (heureSup.getStatut() == StatutHeureSup.VALIDEE) {
                heureSup.setStatut(StatutHeureSup.PAYEE);
                heureSupRepository.save(heureSup);
            }
        }
        periode.setStatut(StatutPeriodePaie.CLOTUREE);
        periode.setDateCloture(LocalDateTime.now());
        periode.setCloturePar(utilisateur);
        PeriodePaie cloturee = periodeRepository.save(periode);
        auditService.enregistrer(utilisateur == null ? "system" : utilisateur.getUsername(), "CLOTURE",
                "/paie/cloture", "PeriodePaie", annee + "-" + mois,
                "avant=" + avant + "; apres={statut:" + cloturee.getStatut()
                        + ",dateCloture:" + cloturee.getDateCloture() + "}", null);
        return cloturee;
    }

    @Transactional(readOnly = true)
    public String exporterCsv(Integer idPeriode) {
        PeriodePaie periode = periode(idPeriode);
        StringBuilder csv = new StringBuilder("salarie;type;libelle;quantite;montant;reference\n");
        for (VariablePaie variable : variables(idPeriode)) {
            csv.append(valeur(variable.getEmploye().getNomEmp())).append(';')
                    .append(variable.getTypeVariable().name()).append(';')
                    .append(valeur(variable.getLibelle())).append(';')
                    .append(variable.getQuantite() == null ? "" : variable.getQuantite()).append(';')
                    .append(variable.getMontant() == null ? "" : variable.getMontant()).append(';')
                    .append(variable.getReference() == null ? "" : variable.getReference())
                    .append('\n');
        }
        return csv.toString();
    }

    private String valeur(String valeur) {
        return valeur == null ? "" : valeur.replace(';', ',').replace('\n', ' ');
    }
}
