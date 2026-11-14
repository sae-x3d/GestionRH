package com.sirh.backend.modules.recrutement.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import com.sirh.backend.modules.organisation.entity.DepartementEntity;
import com.sirh.backend.modules.organisation.entity.PosteEntity;
import com.sirh.backend.modules.organisation.repository.DepartementRepository;
import com.sirh.backend.modules.organisation.repository.PosteRepository;
import com.sirh.backend.modules.auth.repository.AuthUserRepository;
import com.sirh.backend.modules.notifications.service.NotificationCenterService;
import com.sirh.backend.modules.recrutement.dto.CreateDemandeRequest;
import com.sirh.backend.modules.recrutement.dto.DemandeSummaryResponse;
import com.sirh.backend.modules.recrutement.dto.DemandeDetailResponse;
import com.sirh.backend.modules.recrutement.dto.ManagerOptionsResponse;
import com.sirh.backend.modules.recrutement.dto.ManagerOptionsResponse.DepartmentOption;
import com.sirh.backend.modules.recrutement.dto.ManagerOptionsResponse.PosteOption;
import com.sirh.backend.modules.recrutement.dto.ManagerOptionsResponse.ReferenceOption;
import com.sirh.backend.modules.recrutement.dto.ValidationHistoryResponse;
import com.sirh.backend.modules.recrutement.entity.DemandeRecrutementEntity;
import com.sirh.backend.modules.recrutement.repository.CircuitValidationRepository;
import com.sirh.backend.modules.recrutement.repository.DemandeRecrutementRepository;
import com.sirh.backend.modules.recrutement.repository.MotifRecrutementRepository;
import com.sirh.backend.modules.recrutement.repository.StatutDemandeRepository;
import com.sirh.backend.modules.recrutement.repository.ValidationDemandeRepository;
import com.sirh.backend.modules.recrutement.repository.EtapeCircuitRepository;
import com.sirh.backend.modules.recrutement.repository.DecisionValidationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ManagerRecrutementService {
    private final DepartementRepository departements;
    private final PosteRepository postes;
    private final MotifRecrutementRepository motifs;
    private final StatutDemandeRepository statuts;
    private final CircuitValidationRepository circuits;
    private final DemandeRecrutementRepository demandes;
    private final ValidationDemandeRepository validations;
    private final EtapeCircuitRepository etapes;
    private final DecisionValidationRepository decisions;
    private final AuthUserRepository users;
    private final NotificationCenterService notificationCenter;
    private final JdbcTemplate jdbc;

    public ManagerRecrutementService(DepartementRepository departements, PosteRepository postes,
            MotifRecrutementRepository motifs, StatutDemandeRepository statuts,
            CircuitValidationRepository circuits, DemandeRecrutementRepository demandes,
            ValidationDemandeRepository validations, EtapeCircuitRepository etapes,
            DecisionValidationRepository decisions, AuthUserRepository users,
            NotificationCenterService notificationCenter, JdbcTemplate jdbc) {
        this.departements = departements;
        this.postes = postes;
        this.motifs = motifs;
        this.statuts = statuts;
        this.circuits = circuits;
        this.demandes = demandes;
        this.validations = validations;
        this.etapes = etapes;
        this.decisions = decisions;
        this.users = users;
        this.notificationCenter = notificationCenter;
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public ManagerOptionsResponse getOptions(Integer managerId) {
        DepartementEntity departement = managerDepartment(managerId);
        List<PosteOption> postesActifs = postes.findAllByDepartementIdAndActifTrueOrderByIntitule(departement.getId()).stream()
            .map(poste -> capacity(poste.getId(), poste.getIntitule(), poste.getSalaireBaseMin(), poste.getSalaireBaseMax()))
            .toList();
        List<ReferenceOption> motifsDisponibles = motifs.findAllByOrderByLibelleAsc().stream()
            .map(motif -> new ReferenceOption(motif.getId(), motif.getCode(), motif.getLibelle()))
            .toList();
        return new ManagerOptionsResponse(
            new DepartmentOption(departement.getId(), departement.getCode(), departement.getNom()),
            postesActifs, motifsDisponibles);
    }

    @Transactional
    public DemandeSummaryResponse submit(Integer managerId, CreateDemandeRequest request) {
        if (request.dateSouhaitee() == null || !request.dateSouhaitee().isAfter(LocalDate.now().plusDays(1))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "La date souhaitée doit laisser au moins un jour pour la clôture des candidatures.");
        }
        DepartementEntity departement = managerDepartment(managerId);
        PosteEntity poste = postes.findById(request.posteId())
            .filter(PosteEntity::isActif)
            .filter(value -> departement.getId().equals(value.getDepartementId()))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Le poste est inactif ou n'appartient pas à votre département."));
        validateBudget(request.budgetSalaireBase(), poste.getSalaireBaseMin(), poste.getSalaireBaseMax());
        var motif = motifs.findById(request.motifId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Motif invalide."));
        Integer cibleAutorisee = jdbc.queryForObject(
            "SELECT effectif_autorise FROM postes WHERE id = ? FOR UPDATE", Integer.class, poste.getId());
        int actifs = activeEmployees(poste.getId(), request.dateSouhaitee());
        int reserves = reservedSeats(poste.getId(), request.dateSouhaitee());
        int vacants = Math.max(0, cibleAutorisee - actifs - reserves);
        if ("POSTE_VACANT".equals(motif.getCode()) && request.effectif() > vacants) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "La demande de remplacement dépasse les postes réellement vacants. Effectif autorisé : "
                    + cibleAutorisee + ", salariés actifs à la date souhaitée : " + actifs + ", places déjà réservées : "
                    + reserves + ", postes vacants disponibles : " + vacants
                    + ". Choisissez le motif augmentation de l’effectif pour demander des postes supplémentaires.");
        }
        var statut = status("A_VALIDER_RH");
        var circuit = circuits.findFirstByActifTrue()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Circuit actif manquant."));

        OffsetDateTime maintenant = OffsetDateTime.now();
        DemandeRecrutementEntity saved = demandes.save(new DemandeRecrutementEntity(
            departement.getId(), managerId, poste.getId(), motif.getId(), request.effectif(), request.budgetSalaireBase(),
            request.justification().trim(), request.dateSouhaitee(), statut.getId(), circuit.getId(), maintenant));
        notificationCenter.notifyRole("rh", "DEMANDE_A_VALIDER", "Nouvelle demande à valider",
            "Une demande de recrutement pour « " + poste.getIntitule() + " » attend une validation RH.",
            "/espace/rh/recrutement/demandes");
        notificationCenter.notifyUser(managerId, "DEMANDE_STATUT", "Demande transmise aux RH",
            "Votre demande n°" + saved.getId() + " est en attente de validation RH.",
            "/espace/manager/demandes/nouvelle");
        return summary(saved, poste.getIntitule(), motif.getLibelle(), statut.getLibelle(), departement.getNom(), users.findUsernameById(managerId));
    }

    @Transactional(readOnly = true)
    public List<DemandeSummaryResponse> getDepartmentDemands(Integer managerId) {
        Integer departementId = managerDepartment(managerId).getId();
        return demandes.findAllByDepartementIdOrderByCreatedAtDesc(departementId).stream()
            .map(demande -> summary(demande,
                postes.findById(demande.getPosteId()).map(PosteEntity::getIntitule).orElse("Poste inconnu"),
                motifs.findById(demande.getMotifId()).map(value -> value.getLibelle()).orElse("Motif inconnu"),
                statuts.findById(demande.getStatutId()).map(value -> value.getLibelle()).orElse("Statut inconnu"),
                departements.findById(demande.getDepartementId()).map(DepartementEntity::getNom).orElse("Département inconnu"),
                users.findUsernameById(demande.getDemandeurId())))
            .toList();
    }

    @Transactional(readOnly = true)
    public DemandeDetailResponse getDepartmentDemand(Integer managerId, Integer demandeId) {
        DemandeRecrutementEntity demande = departmentDemand(managerId, demandeId);
        String statusCode = statuts.findById(demande.getStatutId()).map(value -> value.getCode()).orElse("");
        return new DemandeDetailResponse(demande.getId(),
            departements.findById(demande.getDepartementId()).map(DepartementEntity::getNom).orElse("Département inconnu"),
            users.findUsernameById(demande.getDemandeurId()),
            postes.findById(demande.getPosteId()).map(PosteEntity::getIntitule).orElse("Poste inconnu"),
            motifs.findById(demande.getMotifId()).map(value -> value.getLibelle()).orElse("Motif inconnu"),
            demande.getEffectif(), demande.getBudgetSalaireBase(), demande.getJustification(), demande.getDateSouhaitee(),
            statuts.findById(demande.getStatutId()).map(value -> value.getLibelle()).orElse("Statut inconnu"),
            demande.getSoumiseAt(), ("A_VALIDER_RH".equals(statusCode) || "SOUMISE".equals(statusCode))
                && !validations.existsByDemandeId(demandeId), false, history(demandeId));
    }

    @Transactional
    public void cancel(Integer managerId, Integer demandeId) {
        Integer departementId = managerDepartment(managerId).getId();
        DemandeRecrutementEntity demande = demandes.findByIdAndDepartementIdForUpdate(demandeId, departementId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Demande introuvable."));
        String statusCode = statuts.findById(demande.getStatutId()).map(value -> value.getCode()).orElse("");
        if (!("A_VALIDER_RH".equals(statusCode) || "SOUMISE".equals(statusCode)) || validations.existsByDemandeId(demandeId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Cette demande ne peut plus être annulée car son traitement a commencé.");
        }
        var cancelledStatus = statuts.findByCode("ANNULEE")
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                "Statut ANNULEE manquant. Réimportez le fichier database/data-13-11-26.sql."));
        demande.cancel(cancelledStatus.getId(), OffsetDateTime.now());
        demandes.save(demande);
        notificationCenter.notifyUser(demande.getDemandeurId(), "DEMANDE_STATUT", "Demande annulée",
            "Votre demande n°" + demandeId + " a été annulée.", "/espace/manager/demandes/nouvelle");
    }

    private DemandeRecrutementEntity departmentDemand(Integer managerId, Integer demandeId) {
        Integer departementId = managerDepartment(managerId).getId();
        return demandes.findByIdAndDepartementId(demandeId, departementId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Demande introuvable."));
    }

    private DepartementEntity managerDepartment(Integer managerId) {
        List<DepartementEntity> rattachements = departements.findActiveForEmployeeUser(managerId);
        if (rattachements.size() != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Le compte manager doit être rattaché à un seul département actif.");
        }
        return rattachements.getFirst();
    }

    private void validateBudget(BigDecimal budget, BigDecimal minimum, BigDecimal maximum) {
        if (minimum == null || maximum == null || minimum.compareTo(maximum) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Le poste n'a pas de fourchette salariale valide. Contactez les RH.");
        }
        if (budget.compareTo(minimum) < 0 || budget.compareTo(maximum) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Le budget de salaire de base par personne doit être compris entre les salaires de base minimum et maximum du poste.");
        }
    }

    private PosteOption capacity(Integer posteId, String title, BigDecimal minimumSalary, BigDecimal maximumSalary) {
        Integer target = jdbc.queryForObject("SELECT effectif_autorise FROM postes WHERE id = ?", Integer.class, posteId);
        int active = activeEmployees(posteId, LocalDate.now());
        int reserved = reservedSeats(posteId, LocalDate.now());
        Integer minimum = jdbc.queryForObject("""
            SELECT COALESCE((SELECT r.minimum_presence FROM regles_effectif_minimal r
                WHERE r.poste_id = ? AND r.actif = TRUE AND r.date_debut <= CURRENT_DATE
                  AND (r.date_fin IS NULL OR r.date_fin >= CURRENT_DATE)
                ORDER BY r.date_debut DESC LIMIT 1), 0)
            """, Integer.class, posteId);
        return new PosteOption(posteId, title, minimumSalary, maximumSalary, target, active,
            Math.max(0, target - active - reserved), minimum);
    }

    private int activeEmployees(Integer posteId, LocalDate date) {
        Integer count = jdbc.queryForObject("""
            SELECT COUNT(*) FROM employes e
            LEFT JOIN situation_administrative_employe sa ON sa.employe_id = e.id
            LEFT JOIN statuts_administratifs_employe statut ON statut.id = sa.statut_id
            WHERE e.poste_id = ? AND e.date_embauche <= ?
              AND COALESCE(statut.code, 'ACTIF') NOT IN ('SORTI', 'EN_PREPARATION')
            """, Integer.class, posteId, date);
        return count == null ? 0 : count;
    }

    private int reservedSeats(Integer posteId, LocalDate date) {
        Integer count = jdbc.queryForObject("""
            SELECT COALESCE(SUM(GREATEST(d.effectif - d.effectif_recrute, 0)), 0)::integer
            FROM demandes_recrutement d
            JOIN motifs_recrutement motif ON motif.id = d.motif_id
            JOIN statuts_demande_recrutement statut ON statut.id = d.statut_id
            WHERE d.poste_id = ? AND d.date_souhaitee <= ?
              AND statut.code NOT IN ('REFUSEE', 'ANNULEE')
              AND (motif.code = 'POSTE_VACANT'
                   OR (motif.code = 'AUGMENT_EFFECTIF' AND statut.code = 'OFFRE_CREEE'))
            """, Integer.class, posteId, date);
        return count == null ? 0 : count;
    }

    private DemandeSummaryResponse summary(DemandeRecrutementEntity demande, String poste, String motif,
            String statut, String department, String requester) {
        return new DemandeSummaryResponse(demande.getId(), poste, motif, demande.getEffectif(),
            demande.getBudgetSalaireBase(), demande.getDateSouhaitee(), statut, demande.getSoumiseAt(), department, requester);
    }

    private List<ValidationHistoryResponse> history(Integer demandId) {
        return validations.findAllByDemandeIdOrderByDecideeAtAsc(demandId).stream().map(validation -> {
            var stage = etapes.findById(validation.getEtapeCircuitId()).map(value -> value.getLibelle()).orElse("Validation");
            var decision = decisions.findById(validation.getDecisionId()).map(value -> value.getCode()).orElse("Décision");
            var validator = validation.getValidateurId() == null ? "" : users.findUsernameById(validation.getValidateurId());
            return new ValidationHistoryResponse(stage, decision, validator, validation.getCommentaire(), validation.getDecideeAt());
        }).toList();
    }

    private com.sirh.backend.modules.recrutement.entity.StatutDemandeEntity status(String code) {
        return statuts.findByCode(code).orElseThrow(() -> new ResponseStatusException(
            HttpStatus.INTERNAL_SERVER_ERROR, "Statut " + code + " manquant. Réimportez database/data-13-11-26.sql."));
    }
}
