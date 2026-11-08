package com.gestion.service;

import com.gestion.enums.StatutDemandeConge;
import com.gestion.enums.StatutAbsence;
import com.gestion.exception.AccesRefuseException;
import com.gestion.exception.RessourceIntrouvableException;
import com.gestion.model.ConsommationConge;
import com.gestion.model.DemandeConge;
import com.gestion.model.DroitConge;
import com.gestion.model.Employe;
import com.gestion.model.JourFerie;
import com.gestion.model.SoldeConge;
import com.gestion.model.TypeConge;
import com.gestion.service.StockageDocumentService;
import com.gestion.model.PlanningJour;
import com.gestion.enums.TypeJourPlanning;
import com.gestion.model.Utilisateur;
import com.gestion.repository.ConsommationCongeRepository;
import com.gestion.repository.AbsenceRepository;
import com.gestion.repository.DemandeCongeRepository;
import com.gestion.repository.DepartementRepository;
import com.gestion.repository.DroitCongeRepository;
import com.gestion.repository.EmployeRepository;
import com.gestion.repository.JourFerieRepository;
import com.gestion.repository.SoldeCongeRepository;
import com.gestion.repository.TypeCongeRepository;
import com.gestion.repository.PlanningJourRepository;
import com.gestion.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Path;

@Service
@RequiredArgsConstructor
public class CongeService {

    @Value("${app.conges.seuil-absents-equipe:0.30}")
    private double seuilAbsentsEquipe = 0.30;

    private static final Logger log = LoggerFactory.getLogger(CongeService.class);
    private static final BigDecimal ZERO = new BigDecimal("0.0");
    private static final BigDecimal DEMI = new BigDecimal("0.5");
    private static final Set<StatutDemandeConge> STATUTS_ACTIFS = EnumSet.of(
            StatutDemandeConge.SOUMISE, StatutDemandeConge.VALIDEE_MANAGER,
            StatutDemandeConge.ATTENTE_DIRECTION, StatutDemandeConge.VALIDEE_RH);

    private final DemandeCongeRepository demandeRepository;
    private final SoldeCongeRepository soldeRepository;
    private final ConsommationCongeRepository consommationRepository;
    private final TypeCongeRepository typeCongeRepository;
    private final PlanningJourRepository planningJourRepository;
    private final StockageDocumentService stockageDocumentService;
    private final AuditService auditService;
    private final AbsenceRepository absenceRepository;
    private final JourFerieRepository jourFerieRepository;
    private final DroitCongeRepository droitCongeRepository;
    private final EmployeRepository employeRepository;
    private final DepartementRepository departementRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public BigDecimal calculerJours(LocalDate debut, LocalDate fin,
                                    boolean demiJourDebut, boolean demiJourFin) {
        if (debut == null || fin == null) {
            throw new IllegalStateException("Les dates de debut et de fin sont obligatoires.");
        }
        if (fin.isBefore(debut)) {
            throw new IllegalStateException("La date de fin doit etre posterieure a la date de debut.");
        }
        List<JourFerie> feries = jourFerieRepository.findByDateJourFerieBetween(debut, fin);
        if (debut.equals(fin)) {
            if (estJourTravaille(debut, feries)) {
                if (demiJourDebut || demiJourFin) {
                    return DEMI;
                }
                return new BigDecimal("1.0");
            }
            throw new IllegalStateException("La date choisie n'est pas un jour travaille.");
        }
        BigDecimal total = ZERO;
        LocalDate curseur = debut;
        while (!curseur.isAfter(fin)) {
            if (estJourTravaille(curseur, feries)) {
                total = total.add(BigDecimal.ONE);
            }
            curseur = curseur.plusDays(1);
        }
        if (demiJourDebut && estJourTravaille(debut, feries)) {
            total = total.subtract(DEMI);
        }
        if (demiJourFin && estJourTravaille(fin, feries)) {
            total = total.subtract(DEMI);
        }
        if (total.signum() <= 0) {
            throw new IllegalStateException("La periode ne contient aucun jour travaille.");
        }
        return total;
    }

    @Transactional
    public DemandeConge soumettre(Employe employe, Integer idTypeConge, LocalDate debut, LocalDate fin,
                                  boolean demiJourDebut, boolean demiJourFin, String motif,
                                  Utilisateur demandeur) {
        return soumettre(employe, idTypeConge, debut, fin, demiJourDebut, demiJourFin,
                motif, demandeur, null);
    }

    @Transactional
    public DemandeConge soumettre(Employe employe, Integer idTypeConge, LocalDate debut, LocalDate fin,
                                  boolean demiJourDebut, boolean demiJourFin, String motif,
                                  Utilisateur demandeur, MultipartFile justificatif) {
        if (employe == null) {
            throw new IllegalStateException("Aucun dossier salarie n'est rattache a votre compte.");
        }
        if (employe.getDepartement() != null && employe.getDepartement().getIdDept() != null)
            departementRepository.verrouillerPourReservation(employe.getDepartement().getIdDept());
        employeRepository.verrouillerPourDemande(employe.getIdEmp());
        if (debut == null || fin == null || fin.isBefore(debut)) {
            throw new IllegalStateException("Les dates de debut et de fin sont obligatoires et valides.");
        }
        TypeConge type = typeCongeRepository.findById(idTypeConge)
                .orElseThrow(() -> new RessourceIntrouvableException("Type de conge introuvable."));
        if (!type.isActif()) {
            throw new IllegalStateException("Ce type de conge n'est plus actif.");
        }
        if (type.isJustificatifRequis() && (justificatif == null || justificatif.isEmpty())) {
            throw new IllegalStateException("Un justificatif est obligatoire pour ce type de conge.");
        }
        if (type.getPreavisJours() != null && debut.isBefore(LocalDate.now().plusDays(type.getPreavisJours()))) {
            throw new IllegalStateException("Le preavis minimal pour ce type de conge est de " + type.getPreavisJours() + " jour(s).");
        }
        if (demandeRepository.existsChevauchement(employe.getIdEmp(), debut, fin, STATUTS_ACTIFS)) {
            throw new IllegalStateException("Une demande de conge chevauche deja cette periode.");
        }
        if (absenceRepository.existsByEmployeIdEmpAndStatutAndDateDebutLessThanEqualAndDateFinGreaterThanEqual(
                    employe.getIdEmp(), StatutAbsence.SOUMISE, fin, debut)
                || absenceRepository.existsByEmployeIdEmpAndStatutAndDateDebutLessThanEqualAndDateFinGreaterThanEqual(
                    employe.getIdEmp(), StatutAbsence.VALIDEE_RH, fin, debut)) {
            throw new IllegalStateException("Une absence chevauche deja cette periode.");
        }
        verifierSeuilAbsentsEquipe(employe, debut, fin);
        BigDecimal jours = calculerJours(debut, fin, demiJourDebut, demiJourFin);
        if (type.getDureeMaxJours() != null && jours.compareTo(type.getDureeMaxJours()) > 0) {
            throw new IllegalStateException("La duree maximale pour ce type de conge est de " + type.getDureeMaxJours() + " jour(s).");
        }
        if (type.isDecompteSolde()) {
            BigDecimal disponible = soldeRepository.totalDisponible(
                    employe.getIdEmp(), type.getIdTypeConge(), LocalDate.now());
            if (disponible.compareTo(jours) < 0) {
                throw new IllegalStateException("Solde insuffisant : " + disponible + " jour(s) disponible(s) pour "
                        + jours + " demande(s).");
            }
        }
        String justificatifChemin = justificatif == null || justificatif.isEmpty() ? null
                : stockageDocumentService.enregistrer(justificatif, "CONGE", employe.getIdEmp());
        DemandeConge demande = demandeRepository.save(DemandeConge.builder()
                .employe(employe)
                .typeConge(type)
                .dateDebut(debut)
                .dateFin(fin)
                .demiJourDebut(demiJourDebut)
                .demiJourFin(demiJourFin)
                .nombreJours(jours)
                .motif(texteOuNull(motif))
                .justificatifChemin(justificatifChemin)
                .justificatifNom(justificatif == null ? null : justificatif.getOriginalFilename())
                .statut(StatutDemandeConge.SOUMISE)
                .dateSoumission(LocalDateTime.now())
                .dateCreation(LocalDateTime.now())
                .build());
        if (type.isDecompteSolde()) {
            consommerFifo(employe, type, jours, demande);
        }
        auditService.enregistrer(demandeur == null ? "system" : demandeur.getUsername(), "CREATION",
                "/conges", "DemandeConge", String.valueOf(demande.getIdDemandeConge()),
                "avant={}; apres={type:" + type.getCode() + ",dateDebut:" + debut + ",dateFin:" + fin
                        + ",jours:" + jours + ",statut:" + demande.getStatut() + "}", null);
        notifierManagers(demande);
        return demande;
    }

    private void verifierSeuilAbsentsEquipe(Employe employe, LocalDate debut, LocalDate fin) {
        if (employe.getDepartement() == null || employe.getDepartement().getIdDept() == null) return;
        long effectif = employeRepository.countByDepartementIdDeptAndDateDepartIsNull(
                employe.getDepartement().getIdDept());
        if (effectif == 0) return;
        for (LocalDate jour = debut; !jour.isAfter(fin); jour = jour.plusDays(1)) {
            Set<Integer> absents = new java.util.HashSet<>(demandeRepository.employesAbsentsEquipe(
                    employe.getDepartement().getIdDept(), jour, STATUTS_ACTIFS));
            absents.addAll(absenceRepository.employesAbsentsEquipe(employe.getDepartement().getIdDept(), jour,
                    List.of(StatutAbsence.SOUMISE, StatutAbsence.VALIDEE_RH)));
            absents.add(employe.getIdEmp());
            int plafond = Math.max(1, (int) Math.ceil(effectif * Math.max(0, Math.min(1, seuilAbsentsEquipe))));
            if (absents.size() > plafond)
                throw new IllegalStateException("Le seuil d'absents autorise dans l'equipe serait depasse le " + jour + ".");
        }
    }

    @Transactional(readOnly = true)
    public Path cheminJustificatif(Integer idDemande, Utilisateur utilisateur) {
        DemandeConge demande = trouver(idDemande);
        if (!estRhOuAdmin(utilisateur) && !estProprietaire(demande, utilisateur)) {
            throw new AccesRefuseException("Vous n'avez pas acces a ce justificatif.");
        }
        if (demande.getJustificatifChemin() == null) {
            throw new RessourceIntrouvableException("Cette demande ne contient pas de justificatif.");
        }
        return stockageDocumentService.chemin(demande.getJustificatifChemin());
    }

    @Transactional
    public DemandeConge annuler(Integer idDemande, Utilisateur utilisateur) {
        DemandeConge demande = trouver(idDemande);
        if (!demande.getEmploye().getIdEmp().equals(employeId(utilisateur))) {
            throw new AccesRefuseException("Vous ne pouvez annuler que vos propres demandes.");
        }
        if (demande.getStatut() != StatutDemandeConge.SOUMISE
                && demande.getStatut() != StatutDemandeConge.VALIDEE_MANAGER
                && demande.getStatut() != StatutDemandeConge.ATTENTE_DIRECTION) {
            throw new IllegalStateException("Cette demande ne peut plus etre annulee.");
        }
        demande.setStatut(StatutDemandeConge.ANNULEE);
        recrediter(demande);
        return demandeRepository.save(demande);
    }

    @Transactional
    public DemandeConge validerManager(Integer idDemande, boolean accepter, String commentaire,
                                       Utilisateur validateur) {
        DemandeConge demande = trouver(idDemande);
        if (demande.getStatut() != StatutDemandeConge.SOUMISE) {
            throw new IllegalStateException("Cette demande n'est pas en attente de validation manager.");
        }
        if (!peutValiderManager(demande.getEmploye(), validateur)) {
            throw new AccesRefuseException("Vous n'etes pas le manager de ce salarie.");
        }
        demande.setManagerValidateur(validateur);
        demande.setDateValidationManager(LocalDateTime.now());
        demande.setCommentaireManager(texteOuNull(commentaire));
        if (accepter) {
            boolean directionNecessaire = Boolean.TRUE.equals(demande.getTypeConge().getValidationDirection())
                    || (demande.getTypeConge().getDirectionAPartirJours() != null
                    && demande.getNombreJours().compareTo(demande.getTypeConge().getDirectionAPartirJours()) >= 0);
            if (directionNecessaire) {
                demande.setStatut(StatutDemandeConge.ATTENTE_DIRECTION);
                notifierDirection(demande);
            } else {
                demande.setStatut(StatutDemandeConge.VALIDEE_MANAGER);
                notifierRh(demande);
            }
        } else {
            demande.setStatut(StatutDemandeConge.REFUSEE);
            recrediter(demande);
            notifierEmploye(demande, "Votre demande de conge a ete refusee par votre manager.");
        }
        return demandeRepository.save(demande);
    }

    @Transactional
    public DemandeConge validerRh(Integer idDemande, boolean accepter, String commentaire,
                                  Utilisateur validateur) {
        DemandeConge demande = trouver(idDemande);
        if (demande.getStatut() != StatutDemandeConge.VALIDEE_MANAGER) {
            throw new IllegalStateException("Cette demande n'est pas en attente de validation RH.");
        }
        if (!estRhOuAdmin(validateur)) {
            throw new AccesRefuseException("Seule la RH peut valider cette demande.");
        }
        demande.setRhValidateur(validateur);
        demande.setDateValidationRh(LocalDateTime.now());
        demande.setCommentaireRh(texteOuNull(commentaire));
        if (accepter) {
            demande.setStatut(StatutDemandeConge.VALIDEE_RH);
            LocalDate date = demande.getDateDebut();
            while (!date.isAfter(demande.getDateFin())) {
                PlanningJour jour = planningJourRepository
                        .findByPlanningEmployeIdEmpAndDateJour(demande.getEmploye().getIdEmp(), date)
                        .orElse(null);
                if (jour != null && jour.getTypeJour() == TypeJourPlanning.TRAVAIL) {
                    jour.setTypeJour(TypeJourPlanning.CONGE);
                    jour.setHeureDebut(null);
                    jour.setHeureFin(null);
                    jour.setPauseMinutes(null);
                    planningJourRepository.save(jour);
                }
                date = date.plusDays(1);
            }
            notifierEmploye(demande, "Votre demande de conge est definitivement validee.");
        } else {
            demande.setStatut(StatutDemandeConge.REFUSEE);
            recrediter(demande);
            notifierEmploye(demande, "Votre demande de conge a ete refusee par la RH.");
        }
        return demandeRepository.save(demande);
    }

    @Transactional(readOnly = true)
    public List<DemandeConge> demandesEmploye(Integer idEmp) {
        return demandeRepository.findByEmployeIdEmpOrderByDateCreationDesc(idEmp);
    }

    @Transactional(readOnly = true)
    public List<DemandeConge> demandesEnAttenteManager(Utilisateur utilisateur) {
        if (estRhOuAdmin(utilisateur)) {
            return demandeRepository.findByStatutOrderByDateSoumissionAscIdDemandeCongeAsc(StatutDemandeConge.SOUMISE);
        }
        if (utilisateur.getEmploye() == null || utilisateur.getEmploye().getDepartement() == null) {
            return List.of();
        }
        List<DemandeConge> resultat = new ArrayList<>();
        for (Utilisateur manager : utilisateurRepository.trouverManagersParDepartement(
                utilisateur.getEmploye().getDepartement().getIdDept())) {
            if (manager.getIdUser().equals(utilisateur.getIdUser())) {
                resultat.addAll(demandeRepository.findByStatutInAndEmployeDepartementIdDeptOrderByDateSoumissionAscIdDemandeCongeAsc(
                        List.of(StatutDemandeConge.SOUMISE),
                        utilisateur.getEmploye().getDepartement().getIdDept()));
                break;
            }
        }
        return resultat;
    }

    @Transactional(readOnly = true)
    public List<DemandeConge> demandesEnAttenteRh() {
        return demandeRepository.findByStatutOrderByDateSoumissionAscIdDemandeCongeAsc(StatutDemandeConge.VALIDEE_MANAGER);
    }

    @Transactional(readOnly = true)
    public List<DemandeConge> demandesEnAttenteDirection() {
        return demandeRepository.findByStatutOrderByDateSoumissionAscIdDemandeCongeAsc(StatutDemandeConge.ATTENTE_DIRECTION);
    }

    @Transactional
    public DemandeConge validerDirection(Integer id, boolean accepter, String commentaire, Utilisateur validateur) {
        DemandeConge demande = trouver(id);
        if (demande.getStatut() != StatutDemandeConge.ATTENTE_DIRECTION)
            throw new IllegalStateException("Cette demande n'attend pas une decision de la Direction.");
        if (!"DIRECTION".equals(role(validateur)) && !"ADMINISTRATEUR".equals(role(validateur)))
            throw new AccesRefuseException("Seule la Direction peut valider cette demande.");
        demande.setDirectionValidateur(validateur);
        demande.setDateValidationDirection(LocalDateTime.now());
        demande.setCommentaireDirection(texteOuNull(commentaire));
        if (accepter) {
            demande.setStatut(StatutDemandeConge.VALIDEE_MANAGER);
            notifierRh(demande);
        } else {
            demande.setStatut(StatutDemandeConge.REFUSEE);
            recrediter(demande);
            notifierEmploye(demande, "Votre demande de conge a ete refusee par la Direction.");
        }
        return demandeRepository.save(demande);
    }

    @Transactional(readOnly = true)
    public List<DemandeConge> calendrier(LocalDate debut, LocalDate fin, Utilisateur utilisateur) {
        List<DemandeConge> validees = demandeRepository
                .findByStatutAndDateDebutLessThanEqualAndDateFinGreaterThanEqualOrderByDateDebutAsc(
                        StatutDemandeConge.VALIDEE_RH, fin, debut);
        if (estRhOuAdmin(utilisateur) || utilisateur.getEmploye() == null
                || utilisateur.getEmploye().getDepartement() == null) {
            return validees;
        }
        Integer idDept = utilisateur.getEmploye().getDepartement().getIdDept();
        return validees.stream()
                .filter(d -> d.getEmploye().getDepartement() != null
                        && idDept.equals(d.getEmploye().getDepartement().getIdDept()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SoldeConge> soldes(Integer idEmp) {
        return soldeRepository.findByEmployeIdEmpOrderByDateExpirationAscDateAcquisitionAsc(idEmp);
    }

    @Transactional(readOnly = true)
    public BigDecimal totalDisponible(Integer idEmp, Integer idTypeConge) {
        return soldeRepository.totalDisponible(idEmp, idTypeConge, LocalDate.now());
    }

    @Scheduled(cron = "0 0 1 1 * *")
    @Transactional
    public int crediterDroitsAnnuels() {
        int annee = LocalDate.now().getYear();
        LocalDate debut = LocalDate.of(annee, 1, 1);
        LocalDate fin = LocalDate.of(annee, 12, 31);
        int credits = 0;
        for (Employe employe : employeRepository.findByDateDepartIsNullOrderByNomEmpAsc()) {
            int ancienneteMois = ancienneteEnMois(employe.getDateEmbauche());
            for (DroitConge droit : droitCongeRepository.findAllByOrderByAncienneteMinMoisAsc()) {
                if (droit.getAncienneteMinMois() != null && ancienneteMois < droit.getAncienneteMinMois()) {
                    continue;
                }
                Integer idType = droit.getTypeConge().getIdTypeConge();
                if (soldeRepository.existsByEmployeIdEmpAndTypeCongeIdTypeCongeAndDateAcquisitionBetween(
                        employe.getIdEmp(), idType, debut, fin)) {
                    continue;
                }
                BigDecimal quantite = droit.getJoursParAn() == null ? ZERO : droit.getJoursParAn();
                if (quantite.signum() <= 0) {
                    continue;
                }
                soldeRepository.save(SoldeConge.builder()
                        .employe(employe)
                        .typeConge(droit.getTypeConge())
                        .dateAcquisition(LocalDate.now())
                        .dateExpiration(LocalDate.of(annee + 1, 12, 31))
                        .quantiteInitiale(quantite)
                        .quantiteRestante(quantite)
                        .dateCreation(LocalDateTime.now())
                        .build());
                credits++;
            }
        }
        log.info("Credits de conges annuels crees : {}", credits);
        return credits;
    }

    @Scheduled(cron = "0 30 2 * * *")
    @Transactional
    public int expirerSoldes() {
        List<SoldeConge> lots = soldeRepository.findByDateExpirationBeforeAndQuantiteRestanteGreaterThan(
                LocalDate.now(), ZERO);
        for (SoldeConge lot : lots) {
            lot.setQuantiteRestante(ZERO);
            soldeRepository.save(lot);
        }
        if (!lots.isEmpty()) {
            log.info("Lots de conges expires : {}", lots.size());
        }
        return lots.size();
    }

    private void consommerFifo(Employe employe, TypeConge type, BigDecimal jours, DemandeConge demande) {
        BigDecimal reste = jours;
        List<SoldeConge> lots = soldeRepository.verrouillerLotsFifo(employe.getIdEmp(), type.getIdTypeConge());
        for (SoldeConge lot : lots) {
            if (reste.signum() <= 0) {
                break;
            }
            if (lot.getQuantiteRestante() == null || lot.getQuantiteRestante().signum() <= 0) {
                continue;
            }
            if (lot.getDateExpiration() != null && lot.getDateExpiration().isBefore(LocalDate.now())) {
                continue;
            }
            BigDecimal prise = lot.getQuantiteRestante().min(reste);
            lot.setQuantiteRestante(lot.getQuantiteRestante().subtract(prise));
            soldeRepository.save(lot);
            consommationRepository.save(ConsommationConge.builder()
                    .soldeConge(lot)
                    .demandeConge(demande)
                    .quantite(prise)
                    .dateConsommation(LocalDateTime.now())
                    .build());
            reste = reste.subtract(prise);
        }
        if (reste.signum() > 0) {
            throw new IllegalStateException("Solde insuffisant pour reserver " + jours + " jour(s).");
        }
    }

    private void recrediter(DemandeConge demande) {
        List<ConsommationConge> consommations =
                consommationRepository.findByDemandeCongeIdDemandeConge(demande.getIdDemandeConge());
        for (ConsommationConge consommation : consommations) {
            SoldeConge lot = consommation.getSoldeConge();
            lot.setQuantiteRestante(lot.getQuantiteRestante().add(consommation.getQuantite()));
            soldeRepository.save(lot);
        }
        consommationRepository.deleteByDemandeCongeIdDemandeConge(demande.getIdDemandeConge());
    }

    private void notifierManagers(DemandeConge demande) {
        if (demande.getEmploye().getDepartement() == null) {
            notifierRh(demande);
            return;
        }
        for (Utilisateur manager : utilisateurRepository.trouverManagersParDepartement(
                demande.getEmploye().getDepartement().getIdDept())) {
            notificationService.envoyer(manager, "CONGE",
                    "Nouvelle demande de conge de " + demande.getEmploye().getNomEmp()
                            + " du " + demande.getDateDebut() + " au " + demande.getDateFin() + ".",
                    "/conges/validation/manager");
        }
    }

    private void notifierRh(DemandeConge demande) {
        for (Utilisateur rh : utilisateurRepository.trouverGestionnairesCandidatures()) {
            notificationService.envoyer(rh, "CONGE",
                    "Demande de conge de " + demande.getEmploye().getNomEmp()
                            + " en attente de validation RH.",
                    "/conges/validation/rh");
        }
    }

    private void notifierDirection(DemandeConge demande) {
        for (Utilisateur direction : utilisateurRepository.findAll().stream()
                .filter(u -> "DIRECTION".equals(role(u))).toList()) {
            notificationService.envoyer(direction, "CONGE", "Demande de conge exceptionnelle en attente de Direction.",
                    "/conges/validation/direction");
        }
    }

    private void notifierEmploye(DemandeConge demande, String message) {
        utilisateurRepository.findByEmployeIdEmp(demande.getEmploye().getIdEmp())
                .ifPresent(u -> notificationService.envoyer(u, "CONGE", message, "/conges"));
    }

    private boolean estJourTravaille(LocalDate date, List<JourFerie> feries) {
        if (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return false;
        }
        return feries.stream().noneMatch(f -> date.equals(f.getDateJourFerie()));
    }

    private int ancienneteEnMois(LocalDateTime embauche) {
        if (embauche == null) {
            return 0;
        }
        return (int) ChronoUnit.MONTHS.between(embauche.toLocalDate(), LocalDate.now());
    }

    private boolean peutValiderManager(Employe employe, Utilisateur utilisateur) {
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

    public boolean estManager(Utilisateur utilisateur) {
        return "MANAGER".equals(role(utilisateur));
    }

    public boolean estProprietaire(DemandeConge demande, Utilisateur utilisateur) {
        return demande != null && utilisateur != null
                && demande.getEmploye().getIdEmp().equals(employeId(utilisateur));
    }

    private Integer employeId(Utilisateur utilisateur) {
        return utilisateur == null || utilisateur.getEmploye() == null
                ? null : utilisateur.getEmploye().getIdEmp();
    }

    private String role(Utilisateur utilisateur) {
        if (utilisateur == null || utilisateur.getRole() == null) {
            return "";
        }
        return Normalizer.normalize(utilisateur.getRole().getNomRole(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").replace("ROLE_", "")
                .toUpperCase(Locale.ROOT).trim();
    }

    private DemandeConge trouver(Integer id) {
        return demandeRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Demande de conge introuvable : " + id));
    }

    private String texteOuNull(String valeur) {
        return valeur == null || valeur.isBlank() ? null : valeur.trim();
    }
}
