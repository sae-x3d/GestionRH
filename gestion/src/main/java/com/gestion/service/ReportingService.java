package com.gestion.service;

import com.gestion.enums.StatutDemandeConge;
import com.gestion.enums.StatutHeureSup;
import com.gestion.model.Candidature;
import com.gestion.model.Contrat;
import com.gestion.model.DemandeConge;
import com.gestion.model.Employe;
import com.gestion.model.HeureSup;
import com.gestion.repository.CandidatureRepository;
import com.gestion.repository.ContratRepository;
import com.gestion.repository.DemandeCongeRepository;
import com.gestion.repository.EmployeRepository;
import com.gestion.repository.HeureSupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
public class ReportingService {

    private final EmployeRepository employeRepository;
    private final ContratRepository contratRepository;
    private final CandidatureRepository candidatureRepository;
    private final DemandeCongeRepository demandeCongeRepository;
    private final HeureSupRepository heureSupRepository;

    public record LigneEffectif(String libelle, long effectif) {
    }

    @Transactional(readOnly = true)
    public List<Employe> effectifsActifs() {
        return employeRepository.findByDateDepartIsNullOrderByNomEmpAsc();
    }

    @Transactional(readOnly = true)
    public Map<String, Long> effectifsParDepartement() {
        Map<String, Long> resultat = new TreeMap<>();
        for (Employe employe : effectifsActifs()) {
            String cle = employe.getDepartement() == null ? "Non affecte"
                    : employe.getDepartement().getNomDept();
            resultat.merge(cle, 1L, Long::sum);
        }
        return resultat;
    }

    @Transactional(readOnly = true)
    public Map<String, Long> effectifsParPoste() {
        Map<String, Long> resultat = new TreeMap<>();
        for (Employe employe : effectifsActifs()) {
            String cle = employe.getPoste() == null ? "Non affecte" : employe.getPoste().getNomPoste();
            resultat.merge(cle, 1L, Long::sum);
        }
        return resultat;
    }

    @Transactional(readOnly = true)
    public Map<String, Long> coutsAnnuelsParDepartement() {
        Map<String, Long> resultat = new TreeMap<>();
        for (Employe employe : effectifsActifs()) {
            if (employe.getSalaireEmp() == null) {
                continue;
            }
            String cle = employe.getDepartement() == null ? "Non affecte"
                    : employe.getDepartement().getNomDept();
            resultat.merge(cle, employe.getSalaireEmp() * 12L, Long::sum);
        }
        return resultat;
    }

    @Transactional(readOnly = true)
    public Map<String, Long> candidaturesParStatut() {
        Map<String, Long> resultat = new LinkedHashMap<>();
        for (Candidature candidature : candidatureRepository.findAll()) {
            String cle = candidature.getStatut() == null ? "INCONNU" : candidature.getStatut().getLibelle();
            resultat.merge(cle, 1L, Long::sum);
        }
        return resultat;
    }

    @Transactional(readOnly = true)
    public double delaiMoyenRecrutementJours() {
        long total = 0;
        long nombre = 0;
        for (Contrat contrat : contratRepository.findAll()) {
            if (contrat.getCandidature() == null || contrat.getCandidature().getDateCandidature() == null
                    || contrat.getDateGeneration() == null) {
                continue;
            }
            total += ChronoUnit.DAYS.between(
                    contrat.getCandidature().getDateCandidature(), contrat.getDateGeneration());
            nombre++;
        }
        return nombre == 0 ? 0 : (double) total / nombre;
    }

    @Transactional(readOnly = true)
    public long totalEmployesActifs() {
        return employeRepository.countByDateDepartIsNull();
    }

    @Transactional(readOnly = true)
    public Map<String, Double> absenteismeParDepartement() {
        LocalDate debut = LocalDate.now().withDayOfMonth(1);
        LocalDate fin = debut.withDayOfMonth(debut.lengthOfMonth());
        Map<String, Double> joursAbsence = new TreeMap<>();
        for (DemandeConge demande : demandeCongeRepository.findAll()) {
            if (demande.getStatut() != StatutDemandeConge.VALIDEE_MANAGER
                    && demande.getStatut() != StatutDemandeConge.VALIDEE_RH) {
                continue;
            }
            if (demande.getDateDebut() == null || demande.getDateFin() == null
                    || demande.getDateDebut().isAfter(fin) || demande.getDateFin().isBefore(debut)) {
                continue;
            }
            LocalDate borneDebut = demande.getDateDebut().isBefore(debut)
                    ? debut : demande.getDateDebut();
            LocalDate borneFin = demande.getDateFin().isAfter(fin) ? fin : demande.getDateFin();
            long jours = joursOuvres(borneDebut, borneFin);
            if (jours == 0) {
                continue;
            }
            String cle = libelleDepartement(demande.getEmploye());
            joursAbsence.merge(cle, (double) jours, Double::sum);
        }
        long joursOuvresMois = joursOuvres(debut, fin);
        Map<String, Long> effectifs = effectifsParDepartement();
        Map<String, Double> resultat = new TreeMap<>();
        for (Map.Entry<String, Long> entree : effectifs.entrySet()) {
            double base = entree.getValue() * joursOuvresMois;
            double absences = joursAbsence.getOrDefault(entree.getKey(), 0.0);
            double taux = base == 0 ? 0
                    : BigDecimal.valueOf(absences * 100 / base)
                    .setScale(1, RoundingMode.HALF_UP).doubleValue();
            resultat.put(entree.getKey(), taux);
        }
        return resultat;
    }

    @Transactional(readOnly = true)
    public Map<String, BigDecimal> heuresSupParDepartement() {
        LocalDate debut = LocalDate.now().withDayOfMonth(1);
        LocalDate fin = debut.withDayOfMonth(debut.lengthOfMonth());
        Map<String, Long> minutes = new TreeMap<>();
        for (HeureSup heureSup : heureSupRepository.findAll()) {
            if (heureSup.getStatut() == StatutHeureSup.REFUSEE
                    || heureSup.getDateHeureSup() == null
                    || heureSup.getDateHeureSup().isBefore(debut)
                    || heureSup.getDateHeureSup().isAfter(fin)) {
                continue;
            }
            String cle = libelleDepartement(heureSup.getEmploye());
            minutes.merge(cle, (long) heureSup.getMinutes(), Long::sum);
        }
        Map<String, BigDecimal> resultat = new TreeMap<>();
        minutes.forEach((cle, valeur) -> resultat.put(cle,
                BigDecimal.valueOf(valeur).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP)));
        return resultat;
    }

    private long joursOuvres(LocalDate debut, LocalDate fin) {
        long jours = 0;
        LocalDate courant = debut;
        while (!courant.isAfter(fin)) {
            if (courant.getDayOfWeek() != DayOfWeek.SATURDAY
                    && courant.getDayOfWeek() != DayOfWeek.SUNDAY) {
                jours++;
            }
            courant = courant.plusDays(1);
        }
        return jours;
    }

    private String libelleDepartement(Employe employe) {
        return employe == null || employe.getDepartement() == null
                ? "Non affecte" : employe.getDepartement().getNomDept();
    }
}
