package com.gestion.service;

import com.gestion.enums.StatutAnomalie;
import com.gestion.enums.StatutDemandeConge;
import com.gestion.enums.StatutHeureSup;
import com.gestion.enums.StatutPlanning;
import com.gestion.enums.TypeAnomaliePointage;
import com.gestion.enums.TypeJourPlanning;
import com.gestion.exception.AccesRefuseException;
import com.gestion.exception.RessourceIntrouvableException;
import com.gestion.model.AnomaliePointage;
import com.gestion.model.Employe;
import com.gestion.model.HeureSup;
import com.gestion.model.ParametreTempsTravail;
import com.gestion.model.Planning;
import com.gestion.model.PlanningJour;
import com.gestion.model.Pointage;
import com.gestion.model.JourTravaille;
import com.gestion.model.PeriodePaie;
import com.gestion.enums.StatutPeriodePaie;
import com.gestion.model.Utilisateur;
import com.gestion.repository.AnomaliePointageRepository;
import com.gestion.repository.EmployeRepository;
import com.gestion.repository.DemandeCongeRepository;
import com.gestion.repository.AbsenceRepository;
import com.gestion.enums.StatutAbsence;
import com.gestion.repository.HeureSupRepository;
import com.gestion.repository.JourFerieRepository;
import com.gestion.repository.ParametreTempsTravailRepository;
import com.gestion.repository.PlanningJourRepository;
import com.gestion.repository.PlanningRepository;
import com.gestion.repository.PointageRepository;
import com.gestion.repository.JourTravailleRepository;
import com.gestion.repository.PeriodePaieRepository;
import com.gestion.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class TempsTravailService {

    private static final int TOLERANCE_HEURES_MANQUANTES = 15;

    private final ParametreTempsTravailRepository parametreRepository;
    private final PlanningRepository planningRepository;
    private final PlanningJourRepository planningJourRepository;
    private final PointageRepository pointageRepository;
    private final AnomaliePointageRepository anomalieRepository;
    private final HeureSupRepository heureSupRepository;
    private final EmployeRepository employeRepository;
    private final JourFerieRepository jourFerieRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final NotificationService notificationService;
    private final DemandeCongeRepository demandeCongeRepository;
    private final AbsenceRepository absenceRepository;
    private final JourTravailleRepository jourTravailleRepository;
    private final PeriodePaieRepository periodePaieRepository;
    private final AuditService auditService;
    @Value("${app.planning.couverture-minimale:0.5}")
    private double couvertureMinimale;

    @Transactional
    public ParametreTempsTravail parametre() {
        return parametreRepository.findFirstByActifTrueOrderByIdParametreAsc()
                .orElseGet(() -> parametreRepository.save(ParametreTempsTravail.builder().build()));
    }

    @Transactional
    public ParametreTempsTravail enregistrerParametre(BigDecimal heuresHebdomadaires,
                                                      LocalTime heureDebut, LocalTime heureFin,
                                                      Integer pauseMinutes, Integer toleranceRetardMinutes) {
        return enregistrerParametre(heuresHebdomadaires, heureDebut, heureFin, pauseMinutes,
                toleranceRetardMinutes, parametre().getSeuilTranche25Minutes(),
                parametre().getMajorationNuit(), parametre().getMajorationDimanche(), parametre().getMajorationFerie());
    }

    @Transactional
    public ParametreTempsTravail enregistrerParametre(BigDecimal heuresHebdomadaires,
                                                      LocalTime heureDebut, LocalTime heureFin,
                                                      Integer pauseMinutes, Integer toleranceRetardMinutes,
                                                      Integer tranche25, Integer nuit, Integer dimanche, Integer ferie) {
        if (heuresHebdomadaires == null || heuresHebdomadaires.signum() <= 0) {
            throw new IllegalStateException("Le volume hebdomadaire doit etre positif.");
        }
        if (heureDebut == null || heureFin == null || !heureDebut.isBefore(heureFin)) {
            throw new IllegalStateException("Les horaires de debut et de fin sont invalides.");
        }
        if (pauseMinutes == null || pauseMinutes < 0 || pauseMinutes > 240) {
            throw new IllegalStateException("La pause doit etre comprise entre 0 et 240 minutes.");
        }
        if (toleranceRetardMinutes == null || toleranceRetardMinutes < 0 || toleranceRetardMinutes > 120) {
            throw new IllegalStateException("La tolerance de retard doit etre comprise entre 0 et 120 minutes.");
        }
        if (tranche25 == null || tranche25 < 0 || tranche25 > 2400
                || !tauxValide(nuit) || !tauxValide(dimanche) || !tauxValide(ferie))
            throw new IllegalStateException("Seuil ou taux de majoration invalide.");
        ParametreTempsTravail parametre = parametre();
        parametre.setHeuresHebdomadaires(heuresHebdomadaires);
        parametre.setSeuilHebdoHeuresSup(heuresHebdomadaires);
        parametre.setHeureDebut(heureDebut);
        parametre.setHeureFin(heureFin);
        parametre.setPauseMinutes(pauseMinutes);
        parametre.setToleranceRetardMinutes(toleranceRetardMinutes);
        parametre.setSeuilTranche25Minutes(tranche25);
        parametre.setMajorationNuit(nuit); parametre.setMajorationDimanche(dimanche);
        parametre.setMajorationFerie(ferie);
        parametre.setActif(true);
        return parametreRepository.save(parametre);
    }

    @Transactional(readOnly = true)
    public List<Planning> planningsEquipe(Utilisateur utilisateur, LocalDate semaine) {
        LocalDate lundi = lundi(semaine);
        if (estRhOuAdmin(utilisateur)) {
            return planningRepository.findByDateDebutBetweenOrderByDateDebutAscEmployeNomEmpAsc(lundi, lundi);
        }
        if (utilisateur.getEmploye() == null || utilisateur.getEmploye().getDepartement() == null) {
            return List.of();
        }
        return planningRepository.findByEmployeDepartementIdDeptAndDateDebutOrderByEmployeNomEmpAsc(
                utilisateur.getEmploye().getDepartement().getIdDept(), lundi);
    }

    @Transactional(readOnly = true)
    public Planning planning(Integer idPlanning) {
        return planningRepository.findById(idPlanning)
                .orElseThrow(() -> new RessourceIntrouvableException("Planning introuvable : " + idPlanning));
    }

    @Transactional(readOnly = true)
    public Planning planningEmploye(Integer idEmp, LocalDate semaine) {
        return planningRepository.findByEmployeIdEmpAndDateDebut(idEmp, lundi(semaine)).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<PlanningJour> jours(Integer idPlanning) {
        return planningJourRepository.findByPlanningIdPlanningOrderByDateJourAsc(idPlanning);
    }

    @Transactional(readOnly = true)
    public boolean sousEffectif(Integer idPlanning, LocalDate date) {
        Planning p = planning(idPlanning);
        if (p.getEmploye() == null || p.getEmploye().getDepartement() == null) return false;
        Integer dept = p.getEmploye().getDepartement().getIdDept();
        long effectif = employeRepository.countByDepartementIdDeptAndDateDepartIsNull(dept);
        if (effectif <= 1) return false;
        long planifies = planningJourRepository.countByPlanningEmployeDepartementIdDeptAndDateJourAndTypeJour(
                dept, date, TypeJourPlanning.TRAVAIL);
        return planifies < Math.ceil(effectif * Math.max(0, Math.min(1, couvertureMinimale)));
    }

    @Transactional
    public Planning creerPlanning(Employe employe, LocalDate semaine, Utilisateur createur) {
        if (employe == null) {
            throw new IllegalStateException("Salarie obligatoire.");
        }
        if (createur != null && !peutGerer(createur, employe)) {
            throw new AccesRefuseException("Vous ne pouvez pas planifier ce salarie.");
        }
        LocalDate lundi = lundi(semaine);
        Planning planning = planningRepository.findByEmployeIdEmpAndDateDebut(employe.getIdEmp(), lundi)
                .orElse(null);
        if (planning != null) {
            return planning;
        }
        ParametreTempsTravail parametre = parametre();
        List<LocalDate> feries = jourFerieRepository
                .findByDateJourFerieBetween(lundi, lundi.plusDays(6))
                .stream().map(j -> j.getDateJourFerie()).toList();
        Planning nouveau = Planning.builder()
                .employe(employe)
                .dateDebut(lundi)
                .dateFin(lundi.plusDays(6))
                .statut(StatutPlanning.BROUILLON)
                .creePar(createur)
                .dateCreation(LocalDateTime.now())
                .build();
        List<PlanningJour> jours = new ArrayList<>();
        BigDecimal hebdo = employe.getRegimeTravail() != null
                && employe.getRegimeTravail().getHeuresHebdomadaires() != null
                ? employe.getRegimeTravail().getHeuresHebdomadaires() : parametre.getHeuresHebdomadaires();
        int nbJoursOuvres = Math.max(1, joursOuvresSemaine(employe));
        int dureeNetteJour = hebdo.multiply(BigDecimal.valueOf(60)).intValue() / nbJoursOuvres;
        for (int i = 0; i < 7; i++) {
            LocalDate date = lundi.plusDays(i);
            PlanningJour.PlanningJourBuilder jour = PlanningJour.builder()
                    .planning(nouveau)
                    .dateJour(date);
            if (feries.contains(date)) {
                jours.add(jour.typeJour(TypeJourPlanning.FERIE).build());
            } else if (!estJourOuvre(employe, date)) {
                jours.add(jour.typeJour(TypeJourPlanning.REPOS).build());
            } else {
                LocalTime fin = parametre.getHeureDebut().plusMinutes(dureeNetteJour + parametre.getPauseMinutes());
                jours.add(jour.typeJour(TypeJourPlanning.TRAVAIL)
                        .heureDebut(parametre.getHeureDebut())
                        .heureFin(fin)
                        .pauseMinutes(parametre.getPauseMinutes())
                        .build());
            }
        }
        nouveau.setJours(jours);
        return planningRepository.save(nouveau);
    }

    @Transactional
    public PlanningJour modifierJour(Integer idPlanning, LocalDate date, TypeJourPlanning type,
                                     LocalTime heureDebut, LocalTime heureFin, Integer pauseMinutes,
                                     Utilisateur utilisateur) {
        Planning planning = planning(idPlanning);
        if (!peutGerer(utilisateur, planning.getEmploye())) {
            throw new AccesRefuseException("Vous ne pouvez pas modifier ce planning.");
        }
        if (planning.getStatut() == StatutPlanning.PUBLIE && !estRhOuAdmin(utilisateur)) {
            throw new IllegalStateException("Un planning publie ne peut etre modifie que par la RH.");
        }
        if (date.isBefore(planning.getDateDebut()) || date.isAfter(planning.getDateFin())) {
            throw new IllegalStateException("La date sort de la semaine planifiee.");
        }
        if (type == TypeJourPlanning.TRAVAIL && demandeCongeRepository
                .existsByEmployeIdEmpAndStatutAndDateDebutLessThanEqualAndDateFinGreaterThanEqual(
                        planning.getEmploye().getIdEmp(), StatutDemandeConge.VALIDEE_RH, date, date)) {
            throw new IllegalStateException("Un conge valide bloque cette journee de planning.");
        }
        if (type == TypeJourPlanning.TRAVAIL && absenceRepository
                .existsByEmployeIdEmpAndStatutAndDateDebutLessThanEqualAndDateFinGreaterThanEqual(
                        planning.getEmploye().getIdEmp(), StatutAbsence.VALIDEE_RH, date, date)) {
            throw new IllegalStateException("Une absence valide bloque cette journee de planning.");
        }
        if (type == TypeJourPlanning.TRAVAIL
                && (heureDebut == null || heureFin == null || !heureDebut.isBefore(heureFin))) {
            throw new IllegalStateException("Les horaires de travail sont obligatoires et incoherents.");
        }
        if (type == TypeJourPlanning.TRAVAIL) {
            int pause = pauseMinutes == null ? 0 : pauseMinutes;
            long jourMinutes = Duration.between(heureDebut, heureFin).toMinutes() - pause;
            if (jourMinutes < 0 || jourMinutes > 12 * 60) {
                throw new IllegalStateException("La journee doit contenir au plus 12 heures de travail.");
            }
            PlanningJour precedent = planningJourRepository
                    .findByPlanningEmployeIdEmpAndDateJour(planning.getEmploye().getIdEmp(), date.minusDays(1))
                    .orElse(null);
            if (precedent != null && precedent.getHeureFin() != null
                    && Duration.between(precedent.getHeureFin(), heureDebut).toMinutes() < 11 * 60) {
                throw new IllegalStateException("Un repos quotidien minimum de 11 heures est requis.");
            }
            long semaineMinutes = planningJourRepository.findByPlanningIdPlanningOrderByDateJourAsc(idPlanning)
                    .stream().filter(j -> j.getTypeJour() == TypeJourPlanning.TRAVAIL && !j.getDateJour().equals(date))
                    .mapToLong(j -> j.getHeureDebut() == null || j.getHeureFin() == null ? 0
                            : Math.max(0, Duration.between(j.getHeureDebut(), j.getHeureFin()).toMinutes()
                                    - (j.getPauseMinutes() == null ? 0 : j.getPauseMinutes())))
                    .sum() + jourMinutes;
            BigDecimal heuresMax = planning.getEmploye().getRegimeTravail() != null
                    && planning.getEmploye().getRegimeTravail().getHeuresHebdomadaires() != null
                    ? planning.getEmploye().getRegimeTravail().getHeuresHebdomadaires()
                    : parametre().getHeuresHebdomadaires();
            BigDecimal maximum = heuresMax.multiply(BigDecimal.valueOf(60));
            if (BigDecimal.valueOf(semaineMinutes).compareTo(maximum) > 0) {
                throw new IllegalStateException("Le planning depasse la duree hebdomadaire autorisee.");
            }
        }
        PlanningJour jour = planningJourRepository
                .findByPlanningEmployeIdEmpAndDateJour(planning.getEmploye().getIdEmp(), date)
                .orElseGet(() -> PlanningJour.builder().planning(planning).dateJour(date).build());
        if (jour.getPlanning() != null && !jour.getPlanning().getIdPlanning().equals(idPlanning)) {
            throw new IllegalStateException("Ce jour appartient a un autre planning.");
        }
        jour.setTypeJour(type);
        jour.setHeureDebut(type == TypeJourPlanning.TRAVAIL ? heureDebut : null);
        jour.setHeureFin(type == TypeJourPlanning.TRAVAIL ? heureFin : null);
        jour.setPauseMinutes(type == TypeJourPlanning.TRAVAIL ? pauseMinutes : null);
        PlanningJour enregistre = planningJourRepository.save(jour);
        planning.setStatut(StatutPlanning.BROUILLON);
        planningRepository.save(planning);
        return enregistre;
    }

    @Transactional
    public Planning publier(Integer idPlanning, Utilisateur utilisateur) {
        Planning planning = planning(idPlanning);
        if (!peutGerer(utilisateur, planning.getEmploye())) {
            throw new AccesRefuseException("Vous ne pouvez pas publier ce planning.");
        }
        planning.setStatut(StatutPlanning.PUBLIE);
        planning.setDatePublication(LocalDateTime.now());
        utilisateurRepository.findByEmployeIdEmp(planning.getEmploye().getIdEmp())
                .ifPresent(u -> notificationService.envoyer(u, "PLANNING",
                        "Votre planning de la semaine du "
                                + planning.getDateDebut() + " est disponible.",
                        "/pointage"));
        return planningRepository.save(planning);
    }

    @Transactional
    public Pointage pointerArrivee(Employe employe, LocalDateTime moment) {
        if (employe == null) {
            throw new IllegalStateException("Aucun dossier salarie n'est rattache a votre compte.");
        }
        LocalDate date = moment.toLocalDate();
        if (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            throw new IllegalStateException("Le pointage n'est pas ouvert le week-end.");
        }
        Pointage pointage = pointageRepository.findByEmployeIdEmpAndDatePointage(employe.getIdEmp(), date)
                .orElseGet(() -> Pointage.builder()
                        .employe(employe)
                        .datePointage(date)
                        .dateCreation(LocalDateTime.now())
                        .statut(com.gestion.enums.StatutPointage.INCOMPLET)
                        .build());
        if (pointage.getHeureArrivee() != null) {
            throw new IllegalStateException("L'heure d'arrivee est deja enregistree.");
        }
        PlanningJour prevu = planningJourRepository
                .findByPlanningEmployeIdEmpAndDateJour(employe.getIdEmp(), date).orElse(null);
        ParametreTempsTravail parametre = parametre();
        LocalTime attendu = prevu != null && prevu.getHeureDebut() != null
                ? prevu.getHeureDebut() : parametre.getHeureDebut();
        int retard = (int) Duration.between(attendu, moment.toLocalTime()).toMinutes();
        pointage.setHeureArrivee(moment.toLocalTime());
        pointage.setPauseMinutes(prevu != null && prevu.getPauseMinutes() != null
                ? prevu.getPauseMinutes() : parametre.getPauseMinutes());
        pointage.setStatut(retard > parametre.getToleranceRetardMinutes()
                ? com.gestion.enums.StatutPointage.RETARD
                : com.gestion.enums.StatutPointage.PRESENT);
        pointage.setDateModification(LocalDateTime.now());
        Pointage enregistre = pointageRepository.save(pointage);
        if (retard > parametre.getToleranceRetardMinutes()) {
            creerAnomalie(enregistre, TypeAnomaliePointage.RETARD, retard);
        }
        return enregistre;
    }

    @Transactional
    public Pointage pointerDepart(Employe employe, LocalDateTime moment) {
        if (employe == null) {
            throw new IllegalStateException("Aucun dossier salarie n'est rattache a votre compte.");
        }
        LocalDate date = moment.toLocalDate();
        Pointage pointage = pointageRepository.findByEmployeIdEmpAndDatePointage(employe.getIdEmp(), date)
                .orElseGet(() -> Pointage.builder()
                        .employe(employe)
                        .datePointage(date)
                        .dateCreation(LocalDateTime.now())
                        .statut(com.gestion.enums.StatutPointage.INCOMPLET)
                        .build());
        if (pointage.getHeureDepart() != null) {
            throw new IllegalStateException("L'heure de depart est deja enregistree.");
        }
        pointage.setHeureDepart(moment.toLocalTime());
        pointage.setDateModification(LocalDateTime.now());
        if (pointage.getHeureArrivee() == null) {
            pointage.setStatut(com.gestion.enums.StatutPointage.INCOMPLET);
            Pointage enregistre = pointageRepository.save(pointage);
            creerAnomalie(enregistre, TypeAnomaliePointage.POINTAGE_INCOMPLET, null);
            return enregistre;
        }
        PlanningJour prevu = planningJourRepository
                .findByPlanningEmployeIdEmpAndDateJour(employe.getIdEmp(), date).orElse(null);
        ParametreTempsTravail parametre = parametre();
        int pause = pointage.getPauseMinutes() != null ? pointage.getPauseMinutes()
                : parametre.getPauseMinutes();
        int travaille = (int) Duration.between(pointage.getHeureArrivee(),
                pointage.getHeureDepart()).toMinutes() - pause;
        LocalTime finAttendue = prevu != null && prevu.getHeureFin() != null
                ? prevu.getHeureFin() : parametre.getHeureFin();
        LocalTime debutAttendu = prevu != null && prevu.getHeureDebut() != null
                ? prevu.getHeureDebut() : parametre.getHeureDebut();
        int attendu = (int) Duration.between(debutAttendu, finAttendue).toMinutes() - pause;
        boolean departAnticipe = pointage.getHeureDepart().isBefore(finAttendue);
        pointage.setStatut(departAnticipe
                ? com.gestion.enums.StatutPointage.DEPART_ANTICIPE
                : com.gestion.enums.StatutPointage.PRESENT);
        Pointage enregistre = pointageRepository.save(pointage);
        if (departAnticipe) {
            creerAnomalie(enregistre, TypeAnomaliePointage.DEPART_ANTICIPE,
                    (int) Duration.between(pointage.getHeureDepart(), finAttendue).toMinutes());
        }
        if (travaille < attendu - TOLERANCE_HEURES_MANQUANTES) {
            creerAnomalie(enregistre, TypeAnomaliePointage.HEURES_MANQUANTES, attendu - travaille);
        }
        return enregistre;
    }

    @Transactional(readOnly = true)
    public List<Pointage> pointages(Employe employe, LocalDate debut, LocalDate fin) {
        return pointageRepository.findByEmployeIdEmpAndDatePointageBetweenOrderByDatePointageAsc(
                employe.getIdEmp(), debut, fin);
    }

    @Transactional(readOnly = true)
    public List<Pointage> pointagesEquipe(Utilisateur utilisateur, LocalDate debut, LocalDate fin) {
        if (estRhOuAdmin(utilisateur)) {
            return pointageRepository.findByDatePointageBetweenOrderByEmployeNomEmpAsc(debut, fin);
        }
        if (utilisateur.getEmploye() == null || utilisateur.getEmploye().getDepartement() == null) {
            return List.of();
        }
        return pointageRepository
                .findByEmployeDepartementIdDeptAndDatePointageBetweenOrderByDatePointageAscEmployeNomEmpAsc(
                        utilisateur.getEmploye().getDepartement().getIdDept(), debut, fin);
    }

    @Transactional(readOnly = true)
    public List<AnomaliePointage> anomalies(Utilisateur utilisateur, StatutAnomalie statut) {
        List<AnomaliePointage> anomalies = statut == null
                ? anomalieRepository.findAll()
                : anomalieRepository.findByStatutOrderByDateAnomalieAsc(statut);
        if (estRhOuAdmin(utilisateur)) {
            return anomalies;
        }
        if (utilisateur.getEmploye() == null || utilisateur.getEmploye().getDepartement() == null) {
            return List.of();
        }
        Integer idDept = utilisateur.getEmploye().getDepartement().getIdDept();
        return anomalies.stream()
                .filter(a -> a.getEmploye().getDepartement() != null
                        && idDept.equals(a.getEmploye().getDepartement().getIdDept()))
                .toList();
    }

    @Transactional
    public AnomaliePointage traiterAnomalie(Integer idAnomalie, StatutAnomalie statut,
                                            String commentaire, Utilisateur utilisateur) {
        AnomaliePointage anomalie = anomalieRepository.findById(idAnomalie)
                .orElseThrow(() -> new RessourceIntrouvableException("Anomalie introuvable : " + idAnomalie));
        if (!peutGerer(utilisateur, anomalie.getEmploye())) {
            throw new AccesRefuseException("Vous ne pouvez pas traiter cette anomalie.");
        }
        anomalie.setStatut(statut == null ? StatutAnomalie.JUSTIFIEE : statut);
        anomalie.setCommentaire(texteOuNull(commentaire));
        anomalie.setTraitePar(utilisateur);
        anomalie.setDateTraitement(LocalDateTime.now());
        return anomalieRepository.save(anomalie);
    }

    @Transactional
    public List<HeureSup> declarerHeureSup(Employe employe, LocalDate date, Integer minutes,
                                           String motif) {
        if (employe == null) {
            throw new IllegalStateException("Aucun dossier salarie n'est rattache a votre compte.");
        }
        if (date == null || date.isAfter(LocalDate.now())) {
            throw new IllegalStateException("La date des heures supplementaires est invalide.");
        }
        if (minutes == null || minutes <= 0 || minutes > 600) {
            throw new IllegalStateException("Le volume doit etre compris entre 1 et 600 minutes.");
        }
        LocalDate lundi = lundi(date);
        long dejaDeclare = heureSupRepository.totalMinutes(employe.getIdEmp(), lundi, lundi.plusDays(6));
        ParametreTempsTravail parametres = parametre();
        int seuil25 = parametres == null || parametres.getSeuilTranche25Minutes() == null
                ? 480 : parametres.getSeuilTranche25Minutes();
        int resteTranche25 = (int) Math.max(0, seuil25 - dejaDeclare);
        int tranche25 = Math.min(minutes, resteTranche25);
        int tranche50 = minutes - tranche25;
        List<HeureSup> enregistrees = new ArrayList<>();
        if (tranche25 > 0) {
            enregistrees.add(heureSupRepository.save(HeureSup.builder()
                    .employe(employe).dateHeureSup(date).minutes(tranche25).tauxMajoration(25)
                    .statut(StatutHeureSup.EN_ATTENTE).motif(texteOuNull(motif))
                    .dateCreation(LocalDateTime.now()).build()));
        }
        if (tranche50 > 0) {
            enregistrees.add(heureSupRepository.save(HeureSup.builder()
                    .employe(employe).dateHeureSup(date).minutes(tranche50).tauxMajoration(50)
                    .statut(StatutHeureSup.EN_ATTENTE).motif(texteOuNull(motif))
                    .dateCreation(LocalDateTime.now()).build()));
        }
        String username = utilisateurRepository.findByEmployeIdEmp(employe.getIdEmp())
                .map(Utilisateur::getUsername).orElse("employe:" + employe.getIdEmp());
        auditService.enregistrer(username, "CREATION", "/heures-sup", "HeureSup",
                employe.getIdEmp() + ":" + date,
                "avant={}; apres={minutes:" + minutes + ",date:" + date + ",declarations:"
                        + enregistrees.stream().map(h -> h.getTauxMajoration() + "%=" + h.getMinutes()).toList() + "}", null);
        return enregistrees;
    }

    @Transactional(readOnly = true)
    public List<HeureSup> heuresSup(Employe employe, LocalDate debut, LocalDate fin) {
        return heureSupRepository.findByEmployeIdEmpAndDateHeureSupBetweenOrderByDateHeureSupAsc(
                employe.getIdEmp(), debut, fin);
    }

    @Transactional(readOnly = true)
    public List<HeureSup> heuresSupEquipe(Utilisateur utilisateur, StatutHeureSup statut) {
        List<HeureSup> heures = statut == null ? heureSupRepository.findAll()
                : heureSupRepository.findByStatutOrderByDateHeureSupAsc(statut);
        if (estRhOuAdmin(utilisateur)) {
            return heures;
        }
        if (utilisateur.getEmploye() == null || utilisateur.getEmploye().getDepartement() == null) {
            return List.of();
        }
        Integer idDept = utilisateur.getEmploye().getDepartement().getIdDept();
        return heures.stream()
                .filter(h -> h.getEmploye().getDepartement() != null
                        && idDept.equals(h.getEmploye().getDepartement().getIdDept()))
                .toList();
    }

    @Transactional
    public HeureSup traiterHeureSup(Integer idHeureSup, boolean accepter, boolean convertirEnRepos,
                                    Utilisateur utilisateur) {
        HeureSup heureSup = heureSupRepository.findById(idHeureSup)
                .orElseThrow(() -> new RessourceIntrouvableException(
                        "Heures supplementaires introuvables : " + idHeureSup));
        if (heureSup.getStatut() != StatutHeureSup.EN_ATTENTE) {
            throw new IllegalStateException("Cette declaration a deja ete traitee.");
        }
        if (!peutGerer(utilisateur, heureSup.getEmploye())) {
            throw new AccesRefuseException("Vous ne pouvez pas valider ces heures supplementaires.");
        }
        heureSup.setValidePar(utilisateur);
        heureSup.setDateValidation(LocalDateTime.now());
        if (!accepter) {
            heureSup.setStatut(StatutHeureSup.REFUSEE);
        } else if (convertirEnRepos) {
            heureSup.setStatut(StatutHeureSup.REPOS);
            Employe employe = heureSup.getEmploye();
            employe.setMinutesRecuperation((employe.getMinutesRecuperation() == null ? 0
                    : employe.getMinutesRecuperation()) + heureSup.getMinutes());
            employeRepository.save(employe);
        } else {
            heureSup.setStatut(StatutHeureSup.VALIDEE);
        }
        utilisateurRepository.findByEmployeIdEmp(heureSup.getEmploye().getIdEmp())
                .ifPresent(u -> notificationService.envoyer(u, "HEURE_SUP",
                        accepter ? "Vos heures supplementaires ont ete validees."
                                : "Vos heures supplementaires ont ete refusees.",
                        "/heures-sup"));
        return heureSupRepository.save(heureSup);
    }

    @Transactional(readOnly = true)
    public BigDecimal totalHeuresSup(Employe employe, LocalDate debut, LocalDate fin) {
        long minutes = heureSupRepository.totalMinutes(employe.getIdEmp(), debut, fin);
        return BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }

    @Transactional(readOnly = true)
    public List<JourTravaille> joursTravailles(Employe employe, LocalDate debut, LocalDate fin) {
        return jourTravailleRepository.findByEmployeIdEmpAndDateJourBetweenOrderByDateJourAsc(
                employe.getIdEmp(), debut, fin);
    }

    @Transactional(readOnly = true)
    public List<PeriodePaie> periodesCloturees() {
        return periodePaieRepository.findByStatutOrderByAnneeDescMoisDesc(StatutPeriodePaie.CLOTUREE);
    }

    @Transactional(readOnly = true)
    public List<Employe> employesActifs() { return employeRepository.findByDateDepartIsNullOrderByNomEmpAsc(); }

    @Transactional
    public HeureSup regulariserApresCloture(Integer idEmp, Integer idPeriodeSource, Integer minutes, String motif) {
        if (minutes == null || minutes <= 0 || minutes > 600) throw new IllegalStateException("Les minutes de correction sont invalides.");
        PeriodePaie source = periodePaieRepository.findById(idPeriodeSource)
                .orElseThrow(() -> new RessourceIntrouvableException("Periode source introuvable."));
        if (source.getStatut() != StatutPeriodePaie.CLOTUREE) throw new IllegalStateException("La periode source doit etre cloturee.");
        LocalDate prochainMois = source.getDateFin().plusDays(1).withDayOfMonth(1);
        PeriodePaie cible = periodePaieRepository.findByAnneeAndMois(prochainMois.getYear(), prochainMois.getMonthValue())
                .orElseThrow(() -> new IllegalStateException("Ouvrez la periode suivante avant de regulariser."));
        if (cible.getStatut() == StatutPeriodePaie.CLOTUREE) throw new IllegalStateException("La periode suivante est deja cloturee.");
        Employe employe = employeRepository.findById(idEmp)
                .orElseThrow(() -> new RessourceIntrouvableException("Salarie introuvable."));
        HeureSup correction = heureSupRepository.save(HeureSup.builder().employe(employe).dateHeureSup(cible.getDateDebut())
                .minutes(minutes).tauxMajoration(25).statut(StatutHeureSup.VALIDEE)
                .motif("Regularisation apres cloture " + source.getMois() + "/" + source.getAnnee() + " : " + texteOuNull(motif))
                .dateCreation(LocalDateTime.now()).build());
        auditService.enregistrer("system", "REGULARISATION", "/heures-sup/regularisation", "HeureSup",
                employe.getIdEmp() + ":" + correction.getDateHeureSup(), "avant={}; apres={minutes:" + minutes
                        + ",taux:25,statut:" + correction.getStatut() + ",periodeSource:" + idPeriodeSource
                        + ",periodeCible:" + cible.getIdPeriodePaie() + "}", null);
        return correction;
    }

    @Scheduled(cron = "0 30 23 * * MON-FRI")
    @Transactional
    public int detecterAbsencesDuJour() {
        return detecterAbsences(LocalDate.now());
    }

    @Transactional
    public int detecterAbsencesVeille() {
        LocalDate date = LocalDate.now().minusDays(1);
        while (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY
                || !jourFerieRepository.findByDateJourFerieBetween(date, date).isEmpty()) {
            date = date.minusDays(1);
        }
        return detecterAbsences(date);
    }

    @Transactional
    public int detecterAbsences(LocalDate date) {
        if (date == null || date.getDayOfWeek() == DayOfWeek.SATURDAY
                || date.getDayOfWeek() == DayOfWeek.SUNDAY
                || !jourFerieRepository.findByDateJourFerieBetween(date, date).isEmpty()) return 0;
        int creees = 0;
        for (Employe employe : employeRepository.findByDateDepartIsNullOrderByNomEmpAsc()) {
            PlanningJour prevu = planningJourRepository
                    .findByPlanningEmployeIdEmpAndDateJour(employe.getIdEmp(), date).orElse(null);
            if (prevu == null || prevu.getTypeJour() != TypeJourPlanning.TRAVAIL
                    || prevu.getPlanning().getStatut() != StatutPlanning.PUBLIE
                    || demandeCongeRepository.existsByEmployeIdEmpAndStatutAndDateDebutLessThanEqualAndDateFinGreaterThanEqual(
                        employe.getIdEmp(), StatutDemandeConge.VALIDEE_RH, date, date)
                    || absenceRepository.existsByEmployeIdEmpAndStatutAndDateDebutLessThanEqualAndDateFinGreaterThanEqual(
                        employe.getIdEmp(), StatutAbsence.VALIDEE_RH, date, date)
                    || pointageRepository.findByEmployeIdEmpAndDatePointage(employe.getIdEmp(), date).isPresent()
                    || anomalieRepository.existsByEmployeIdEmpAndDateAnomalieAndTypeAnomalie(
                        employe.getIdEmp(), date, TypeAnomaliePointage.ABSENCE)) continue;
            anomalieRepository.save(AnomaliePointage.builder().employe(employe).dateAnomalie(date)
                    .typeAnomalie(TypeAnomaliePointage.ABSENCE).statut(StatutAnomalie.OUVERTE).build());
            creees++;
        }
        return creees;
    }

    @Scheduled(cron = "0 10 1 * * *")
    @Transactional
    public int calculerJoursTravaillesVeille() {
        LocalDate date = LocalDate.now().minusDays(1);
        int calcules = 0;
        for (Employe employe : employeRepository.findByDateDepartIsNullOrderByNomEmpAsc()) {
            Pointage p = pointageRepository.findByEmployeIdEmpAndDatePointage(employe.getIdEmp(), date).orElse(null);
            if (p == null || p.getHeureArrivee() == null || p.getHeureDepart() == null) continue;
            int brut = (int) Math.max(0, Duration.between(p.getHeureArrivee(), p.getHeureDepart()).toMinutes());
            int net = Math.max(0, brut - (p.getPauseMinutes() == null ? 0 : p.getPauseMinutes()));
            PlanningJour prevu = planningJourRepository.findByPlanningEmployeIdEmpAndDateJour(employe.getIdEmp(), date).orElse(null);
            boolean ferie = !jourFerieRepository.findByDateJourFerieBetween(date, date).isEmpty();
            boolean absence = absenceRepository.existsByEmployeIdEmpAndStatutAndDateDebutLessThanEqualAndDateFinGreaterThanEqual(
                    employe.getIdEmp(), StatutAbsence.VALIDEE_RH, date, date);
            boolean conge = demandeCongeRepository.existsByEmployeIdEmpAndStatutAndDateDebutLessThanEqualAndDateFinGreaterThanEqual(
                    employe.getIdEmp(), StatutDemandeConge.VALIDEE_RH, date, date);
            int normal = (ferie || absence || conge || prevu == null || prevu.getHeureDebut() == null || prevu.getHeureFin() == null)
                    ? 0 : (int) Math.max(0, Duration.between(prevu.getHeureDebut(), prevu.getHeureFin()).toMinutes()
                        - (prevu.getPauseMinutes() == null ? 0 : prevu.getPauseMinutes()));
            JourTravaille record = jourTravailleRepository.findByEmployeIdEmpAndDateJour(employe.getIdEmp(), date)
                    .orElseGet(() -> JourTravaille.builder().employe(employe).dateJour(date).build());
            record.setMinutesBrutes(brut); record.setMinutesNet(net); record.setMinutesNormales(normal);
            record.setMinutesSupplementaires(Math.max(0, net - normal)); record.setDateCalcul(LocalDateTime.now());
            jourTravailleRepository.save(record); calcules++;
        }
        return calcules;
    }

    @Scheduled(cron = "0 30 23 * * SUN")
    @Transactional
    public int calculerHeuresSupSemaine() {
        return calculerHeuresSupSemaine(LocalDate.now());
    }

    @Transactional
    public int calculerHeuresSupSemaine(LocalDate aujourdHui) {
        LocalDate fin = aujourdHui.getDayOfWeek() == DayOfWeek.SUNDAY ? aujourdHui : aujourdHui.minusDays(1);
        LocalDate debut = lundi(fin);
        int creees = 0;
        ParametreTempsTravail config = parametre();
        int seuilMinutes = config.getSeuilHebdoHeuresSup().multiply(BigDecimal.valueOf(60)).intValue();
        for (Employe employe : employeRepository.findByDateDepartIsNullOrderByNomEmpAsc()) {
            List<Pointage> pointagesSemaine = pointageRepository.findByEmployeIdEmpAndDatePointageBetweenOrderByDatePointageAsc(
                    employe.getIdEmp(), debut, fin);
            int travaille = 0;
            int prevu = 0;
            java.util.Map<LocalDate, Integer> travailleParJour = new java.util.HashMap<>();
            java.util.Map<LocalDate, Integer> normalParJour = new java.util.HashMap<>();
            java.util.Map<LocalDate, Pointage> pointageParJour = new java.util.HashMap<>();
            for (LocalDate date = debut; !date.isAfter(fin); date = date.plusDays(1)) {
                LocalDate jourCourant = date;
                PlanningJour plan = planningJourRepository.findByPlanningEmployeIdEmpAndDateJour(
                        employe.getIdEmp(), date).orElse(null);
                boolean nonTravailCompense = jourFerieRepository.findByDateJourFerieBetween(date, date).size() > 0
                        || demandeCongeRepository.existsByEmployeIdEmpAndStatutAndDateDebutLessThanEqualAndDateFinGreaterThanEqual(
                            employe.getIdEmp(), StatutDemandeConge.VALIDEE_RH, date, date)
                        || absenceRepository.existsByEmployeIdEmpAndStatutAndDateDebutLessThanEqualAndDateFinGreaterThanEqual(
                            employe.getIdEmp(), StatutAbsence.VALIDEE_RH, date, date);
                int attendu = plan != null && plan.getHeureDebut() != null && plan.getHeureFin() != null
                        ? (int) Math.max(0, Duration.between(plan.getHeureDebut(), plan.getHeureFin()).toMinutes()
                            - (plan.getPauseMinutes() == null ? 0 : plan.getPauseMinutes()))
                        : (estJourOuvre(employe, date) ? seuilMinutes / Math.max(1, joursOuvresSemaine(employe)) : 0);
                if (nonTravailCompense || !estJourOuvre(employe, date)) attendu = 0;
                normalParJour.put(date, attendu);
                prevu += attendu;
                Pointage p = pointagesSemaine.stream().filter(x -> x.getDatePointage().equals(jourCourant)).findFirst().orElse(null);
                if (p == null || p.getHeureArrivee() == null || p.getHeureDepart() == null) continue;
                int minutes = (int) Math.max(0, Duration.between(p.getHeureArrivee(), p.getHeureDepart()).toMinutes()
                        - (p.getPauseMinutes() == null ? 0 : p.getPauseMinutes()));
                travaille += minutes;
                travailleParJour.put(date, minutes);
                pointageParJour.put(date, p);
            }
            int supplementaires = Math.max(0, travaille - Math.min(seuilMinutes, prevu));
            int dejaDeclare = (int) heureSupRepository.totalMinutes(employe.getIdEmp(), debut, fin);
            int aCreer = Math.max(0, supplementaires - dejaDeclare);
            if (aCreer == 0) continue;
            int seuilPalier25 = config.getSeuilTranche25Minutes() == null ? 480 : config.getSeuilTranche25Minutes();
            int palier25Restant = Math.max(0, seuilPalier25 - dejaDeclare);
            int aRepartir = aCreer;
            java.util.Map<LocalDate, Integer> allocation = new java.util.LinkedHashMap<>();
            List<LocalDate> ordre = travailleParJour.keySet().stream().sorted(java.util.Comparator.reverseOrder()).toList();
            for (LocalDate d : ordre) {
                int excedentJour = Math.max(0, travailleParJour.get(d) - normalParJour.getOrDefault(d, 0));
                int prise = Math.min(aRepartir, excedentJour);
                if (prise > 0) { allocation.put(d, prise); aRepartir -= prise; }
                if (aRepartir == 0) break;
            }
            for (LocalDate d : ordre) {
                if (aRepartir == 0) break;
                int dejaAlloue = allocation.getOrDefault(d, 0);
                int capacite = Math.max(0, travailleParJour.get(d) - dejaAlloue);
                int prise = Math.min(aRepartir, capacite);
                if (prise > 0) { allocation.merge(d, prise, Integer::sum); aRepartir -= prise; }
            }
            for (var entree : allocation.entrySet()) {
                LocalDate dateHs = entree.getKey();
                int totalJour = entree.getValue();
                int tauxSpecial = majorationSpeciale(dateHs, pointageParJour.get(dateHs), config);
                int tranche25 = Math.min(totalJour, palier25Restant);
                enregistrerHeuresCalculees(employe, dateHs, tranche25, 25 + tauxSpecial);
                enregistrerHeuresCalculees(employe, dateHs, totalJour - tranche25, 50 + tauxSpecial);
                palier25Restant -= tranche25;
            }
            creees++;
        }
        return creees;
    }

    private boolean tauxValide(Integer taux) { return taux != null && taux >= 0 && taux <= 200; }

    private int majorationSpeciale(LocalDate date, Pointage pointage, ParametreTempsTravail config) {
        if (!jourFerieRepository.findByDateJourFerieBetween(date, date).isEmpty())
            return config.getMajorationFerie() == null ? 100 : config.getMajorationFerie();
        if (date.getDayOfWeek() == DayOfWeek.SUNDAY)
            return config.getMajorationDimanche() == null ? 50 : config.getMajorationDimanche();
        if (pointage != null && (pointage.getHeureArrivee().isBefore(LocalTime.of(6, 0))
                || pointage.getHeureDepart().isAfter(LocalTime.of(21, 0))))
            return config.getMajorationNuit() == null ? 25 : config.getMajorationNuit();
        return 0;
    }

    private void enregistrerHeuresCalculees(Employe employe, LocalDate date, int minutes, int taux) {
        if (minutes <= 0) return;
        heureSupRepository.save(HeureSup.builder().employe(employe).dateHeureSup(date).minutes(minutes)
                .tauxMajoration(taux).statut(StatutHeureSup.EN_ATTENTE)
                .motif("Calcul automatique depuis les pointages hebdomadaires")
                .dateCreation(LocalDateTime.now()).build());
    }

    private boolean estJourOuvre(Employe employe, LocalDate date) {
        if (employe.getRegimeTravail() == null || employe.getRegimeTravail().getJoursOuvres() == null) {
            return date.getDayOfWeek() != DayOfWeek.SATURDAY && date.getDayOfWeek() != DayOfWeek.SUNDAY;
        }
        String code = switch (date.getDayOfWeek()) {
            case MONDAY -> "LUN"; case TUESDAY -> "MAR"; case WEDNESDAY -> "MER";
            case THURSDAY -> "JEU"; case FRIDAY -> "VEN"; case SATURDAY -> "SAM"; case SUNDAY -> "DIM";
        };
        return java.util.Arrays.stream(employe.getRegimeTravail().getJoursOuvres().toUpperCase(Locale.ROOT)
                .split("[,; ]+")).anyMatch(code::equals);
    }

    private int joursOuvresSemaine(Employe employe) {
        int total = 0;
        for (DayOfWeek day : DayOfWeek.values()) {
            LocalDate sample = LocalDate.of(2024, 1, 1).with(TemporalAdjusters.nextOrSame(day));
            if (estJourOuvre(employe, sample)) total++;
        }
        return total;
    }

    private void creerAnomalie(Pointage pointage, TypeAnomaliePointage type, Integer minutes) {
        if (anomalieRepository.existsByPointageIdPointageAndTypeAnomalie(
                pointage.getIdPointage(), type)) {
            return;
        }
        anomalieRepository.save(AnomaliePointage.builder()
                .employe(pointage.getEmploye())
                .pointage(pointage)
                .dateAnomalie(pointage.getDatePointage())
                .typeAnomalie(type)
                .minutes(minutes)
                .statut(StatutAnomalie.OUVERTE)
                .build());
    }

    private boolean peutGerer(Utilisateur utilisateur, Employe employe) {
        if (estRhOuAdmin(utilisateur)) {
            return true;
        }
        return "MANAGER".equals(role(utilisateur)) && utilisateur.getEmploye() != null
                && utilisateur.getEmploye().getDepartement() != null
                && employe.getDepartement() != null
                && utilisateur.getEmploye().getDepartement().getIdDept()
                .equals(employe.getDepartement().getIdDept());
    }

    public boolean estRhOuAdmin(Utilisateur utilisateur) {
        return List.of("ADMINISTRATEUR", "RH").contains(role(utilisateur));
    }

    public LocalDate lundi(LocalDate date) {
        LocalDate reference = date == null ? LocalDate.now() : date;
        return reference.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private String role(Utilisateur utilisateur) {
        if (utilisateur == null || utilisateur.getRole() == null
                || utilisateur.getRole().getNomRole() == null) {
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
