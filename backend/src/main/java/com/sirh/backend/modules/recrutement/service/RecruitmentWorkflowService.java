package com.sirh.backend.modules.recrutement.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;

import com.sirh.backend.modules.auth.repository.AuthUserRepository;
import com.sirh.backend.modules.notifications.service.NotificationCenterService;
import com.sirh.backend.modules.organisation.entity.DepartementEntity;
import com.sirh.backend.modules.organisation.entity.PosteEntity;
import com.sirh.backend.modules.organisation.repository.DepartementRepository;
import com.sirh.backend.modules.organisation.repository.PosteRepository;
import com.sirh.backend.modules.recrutement.dto.CreateOfferRequest;
import com.sirh.backend.modules.recrutement.dto.CreatedOfferResponse;
import com.sirh.backend.modules.recrutement.dto.DecisionDemandeRequest;
import com.sirh.backend.modules.recrutement.dto.DemandeDetailResponse;
import com.sirh.backend.modules.recrutement.dto.DemandeSummaryResponse;
import com.sirh.backend.modules.recrutement.dto.ValidationHistoryResponse;
import com.sirh.backend.modules.recrutement.entity.DemandeRecrutementEntity;
import com.sirh.backend.modules.recrutement.entity.OffreEmploiEntity;
import com.sirh.backend.modules.recrutement.entity.ValidationDemandeEntity;
import com.sirh.backend.modules.recrutement.repository.DecisionValidationRepository;
import com.sirh.backend.modules.recrutement.repository.DemandeRecrutementRepository;
import com.sirh.backend.modules.recrutement.repository.EtapeCircuitRepository;
import com.sirh.backend.modules.recrutement.repository.MotifRecrutementRepository;
import com.sirh.backend.modules.recrutement.repository.OffreEmploiRepository;
import com.sirh.backend.modules.recrutement.repository.StatutDemandeRepository;
import com.sirh.backend.modules.recrutement.repository.StatutOffreRepository;
import com.sirh.backend.modules.recrutement.repository.ValidationDemandeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RecruitmentWorkflowService {
    private final DemandeRecrutementRepository demandes;
    private final DepartementRepository departements;
    private final PosteRepository postes;
    private final MotifRecrutementRepository motifs;
    private final StatutDemandeRepository statuts;
    private final StatutOffreRepository statutOffres;
    private final EtapeCircuitRepository etapes;
    private final DecisionValidationRepository decisions;
    private final ValidationDemandeRepository validations;
    private final OffreEmploiRepository offres;
    private final AuthUserRepository users;
    private final NotificationCenterService notifications;
    private final JdbcTemplate jdbc;

    public RecruitmentWorkflowService(DemandeRecrutementRepository demandes, DepartementRepository departements,
            PosteRepository postes, MotifRecrutementRepository motifs, StatutDemandeRepository statuts,
            StatutOffreRepository statutOffres,
            EtapeCircuitRepository etapes, DecisionValidationRepository decisions,
            ValidationDemandeRepository validations, OffreEmploiRepository offres,
            AuthUserRepository users, NotificationCenterService notifications, JdbcTemplate jdbc) {
        this.demandes = demandes;
        this.departements = departements;
        this.postes = postes;
        this.motifs = motifs;
        this.statuts = statuts;
        this.statutOffres = statutOffres;
        this.etapes = etapes;
        this.decisions = decisions;
        this.validations = validations;
        this.offres = offres;
        this.users = users;
        this.notifications = notifications;
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public List<DemandeSummaryResponse> pending(String actorRole, Integer actorId) {
        if ("FINANCE".equals(actorRole)) requireFinanceAssignment(actorId);
        List<String> visibleStatuses = switch (actorRole) {
            case "RH" -> List.of("A_VALIDER_RH", "SOUMISE", "A_VALIDER_FINANCE", "A_CREER_OFFRE", "OFFRE_CREEE", "REFUSEE", "ANNULEE");
            case "FINANCE" -> List.of("A_VALIDER_FINANCE", "A_CREER_OFFRE", "OFFRE_CREEE", "REFUSEE");
            case "ADMINISTRATION" -> List.of("A_CREER_OFFRE", "OFFRE_CREEE");
            default -> throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Rôle non autorisé.");
        };
        List<Integer> visibleStatusIds = visibleStatuses.stream().map(code -> status(code).getId()).toList();
        return demandes.findAllByStatutIdInOrderByCreatedAtDesc(visibleStatusIds).stream()
            .filter(demand -> canView(actorRole, demand))
            .map(this::summary).toList();
    }

    @Transactional(readOnly = true)
    public DemandeDetailResponse detail(String actorRole, Integer actorId, Integer demandId) {
        DemandStage stage = stage(actorRole);
        if (stage == DemandStage.FINANCE) requireFinanceAssignment(actorId);
        var demand = demandes.findById(demandId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Demande introuvable."));
        if (!canView(actorRole, demand)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Demande introuvable dans votre périmètre.");
        return detailResponse(demand, isActionable(demand, stage));
    }

    @Transactional
    public DemandeDetailResponse decide(String actorRole, Integer actorId, Integer demandId,
            DecisionDemandeRequest request) {
        DemandStage stage = stage(actorRole);
        if (stage == DemandStage.ADMINISTRATION) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "L’administration crée l’offre à cette étape.");
        if (stage == DemandStage.FINANCE) requireFinanceAssignment(actorId);
        var demand = demandsAtStageForUpdate(demandId, stage);
        String decisionCode = request.decision().trim().toUpperCase(Locale.ROOT);
        String comment = request.commentaire() == null ? null : request.commentaire().trim();
        if ("REFUSEE".equals(decisionCode) && (comment == null || comment.isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Un commentaire est obligatoire pour refuser une demande.");
        }
        var circuitStep = etapes.findByCircuitIdAndCode(demand.getCircuitId(), stage.stepCode)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                "Étape de circuit manquante : " + stage.stepCode + ". Réimportez database/data-13-11-26.sql."));
        var decision = decisions.findByCode(decisionCode)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Décision de validation manquante."));
        validations.save(new ValidationDemandeEntity(demandId, circuitStep.getId(), actorId,
            decision.getId(), comment, OffsetDateTime.now()));

        String nextCode;
        String nextLabel;
        String postTitle = postes.findById(demand.getPosteId()).map(PosteEntity::getIntitule).orElse("Poste");
        if ("REFUSEE".equals(decisionCode)) {
            nextCode = "REFUSEE";
            nextLabel = "Demande refusée";
        } else if (stage == DemandStage.RH) {
            nextCode = "A_VALIDER_FINANCE";
            nextLabel = "Demande à valider par la Finance";
        } else {
            nextCode = "A_CREER_OFFRE";
            nextLabel = "Demande approuvée, offre à créer";
        }
        demand.updateStatus(status(nextCode).getId(), OffsetDateTime.now());
        demandes.save(demand);

        String managerSummary = "Le statut de votre demande n°" + demandId + " a changé : " + status(nextCode).getLibelle() + ".";
        notifications.notifyUser(demand.getDemandeurId(), "DEMANDE_STATUT", "Mise à jour de votre demande", managerSummary,
            "/espace/manager/demandes/nouvelle");
        if ("A_VALIDER_FINANCE".equals(nextCode)) {
            notifications.notifyDepartment("FINANCE", "DEMANDE_A_VALIDER", "Demande à valider",
                "La demande pour « " + postTitle + " » est en attente de validation Finance.",
                "/espace/finance/recrutement/demandes");
        } else if ("A_CREER_OFFRE".equals(nextCode)) {
            notifications.notifyRole("administration", "OFFRE_A_CREER", "Offre à créer",
                "La demande pour « " + postTitle + " » a été approuvée et attend la création de son offre.",
                "/espace/administration/recrutement/demandes");
        }
        return detailResponse(demand, false);
    }

    @Transactional
    public CreatedOfferResponse createOffer(String actorRole, Integer actorId, Integer demandId, CreateOfferRequest request) {
        if (!"ADMINISTRATION".equals(actorRole)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Seule l’administration peut créer l’offre à cette étape.");
        }
        var demand = demandsAtStageForUpdate(demandId, DemandStage.ADMINISTRATION);
        if (offres.findByDemandeId(demandId).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Une offre existe déjà pour cette demande.");
        }
        if (demand.getDateSouhaitee() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "La demande doit préciser une date souhaitée avant la création de l’offre.");
        }
        if (request.dateCloture() == null || !request.dateCloture().isAfter(java.time.LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "La date limite de candidature doit être après la date de création de l’offre.");
        }
        if (!request.dateCloture().isBefore(demand.getDateSouhaitee())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "La date limite de candidature doit être avant la date souhaitée de recrutement.");
        }
        var offerStatus = statutOffres.findByCode("PUBLIEE").orElseThrow(() -> new ResponseStatusException(
            HttpStatus.INTERNAL_SERVER_ERROR, "Statut PUBLIEE manquant. Réimportez database/data-13-11-26.sql."));
        var offer = offres.save(new OffreEmploiEntity(demandId, actorId, offerStatus.getId(), request.titre().trim(),
            clean(request.description()), clean(request.competences()), request.dateCloture()));
        String reasonCode = motifs.findById(demand.getMotifId()).map(value -> value.getCode()).orElse("");
        if ("AUGMENT_EFFECTIF".equals(reasonCode)) {
            Integer eventTypeId = jdbc.queryForObject(
                "SELECT id FROM types_evenement_effectif WHERE code = 'AUGMENTATION_APPROUVEE'", Integer.class);
            if (!Boolean.TRUE.equals(jdbc.queryForObject("""
                    SELECT EXISTS (SELECT 1 FROM historique_effectifs_poste
                        WHERE demande_id = ? AND type_evenement_id = ?)
                    """, Boolean.class, demandId, eventTypeId))) {
                Integer oldTarget = jdbc.queryForObject(
                    "SELECT effectif_autorise FROM postes WHERE id = ? FOR UPDATE", Integer.class, demand.getPosteId());
                int newTarget = oldTarget + demand.getEffectif();
                jdbc.update("UPDATE postes SET effectif_autorise = ? WHERE id = ?", newTarget, demand.getPosteId());
                jdbc.update("""
                    INSERT INTO historique_effectifs_poste
                        (poste_id, ancien_effectif, nouvel_effectif, type_evenement_id, demande_id, acteur_id, motif)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """, demand.getPosteId(), oldTarget, newTarget, eventTypeId, demandId, actorId,
                    "Augmentation approuvée et offre créée à partir de la demande n°" + demandId);
            }
        }
        demand.updateStatus(status("OFFRE_CREEE").getId(), OffsetDateTime.now());
        demandes.save(demand);
        notifications.notifyUser(demand.getDemandeurId(), "DEMANDE_STATUT", "Offre publiée",
            "Une offre a été publiée à partir de votre demande n°" + demandId + ".",
            "/espace/manager/demandes/nouvelle");
        return new CreatedOfferResponse(offer.getId(), offer.getTitre(), offerStatus.getLibelle());
    }

    private DemandeRecrutementEntity demandsAtStage(Integer demandId, DemandStage stage) {
        var demand = demandes.findById(demandId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Demande introuvable."));
        boolean matchesStage = status(stage.statusCode).getId().equals(demand.getStatutId())
            || (stage == DemandStage.RH && status("SOUMISE").getId().equals(demand.getStatutId()));
        if (!matchesStage) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette demande ne se trouve plus dans votre file de traitement.");
        }
        return demand;
    }

    private DemandeRecrutementEntity demandsAtStageForUpdate(Integer demandId, DemandStage stage) {
        var demand = demandes.findByIdForUpdate(demandId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Demande introuvable."));
        if (!status(stage.statusCode).getId().equals(demand.getStatutId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette demande ne se trouve plus dans votre file de traitement.");
        }
        return demand;
    }

    private DemandeDetailResponse detailResponse(DemandeRecrutementEntity demand, boolean actionable) {
        String code = statuts.findById(demand.getStatutId()).map(value -> value.getCode()).orElse("");
        return new DemandeDetailResponse(demand.getId(),
            departements.findById(demand.getDepartementId()).map(DepartementEntity::getNom).orElse("Département inconnu"),
            users.findUsernameById(demand.getDemandeurId()),
            postes.findById(demand.getPosteId()).map(PosteEntity::getIntitule).orElse("Poste inconnu"),
            motifs.findById(demand.getMotifId()).map(value -> value.getLibelle()).orElse("Motif inconnu"),
            demand.getEffectif(), demand.getBudgetSalaireBase(), demand.getJustification(), demand.getDateSouhaitee(),
            statuts.findById(demand.getStatutId()).map(value -> value.getLibelle()).orElse("Statut inconnu"),
            demand.getSoumiseAt(), false, actionable, validations.findAllByDemandeIdOrderByDecideeAtAsc(demand.getId()).stream()
                .map(validation -> new ValidationHistoryResponse(
                    etapes.findById(validation.getEtapeCircuitId()).map(value -> value.getLibelle()).orElse("Validation"),
                    decisions.findById(validation.getDecisionId()).map(value -> value.getCode()).orElse("Décision"),
                    validation.getValidateurId() == null ? "" : users.findUsernameById(validation.getValidateurId()),
                    validation.getCommentaire(), validation.getDecideeAt())).toList());
    }

    private DemandeSummaryResponse summary(DemandeRecrutementEntity demand) {
        return new DemandeSummaryResponse(demand.getId(),
            postes.findById(demand.getPosteId()).map(PosteEntity::getIntitule).orElse("Poste inconnu"),
            motifs.findById(demand.getMotifId()).map(value -> value.getLibelle()).orElse("Motif inconnu"),
            demand.getEffectif(), demand.getBudgetSalaireBase(), demand.getDateSouhaitee(),
            statuts.findById(demand.getStatutId()).map(value -> value.getLibelle()).orElse("Statut inconnu"),
            demand.getSoumiseAt(),
            departements.findById(demand.getDepartementId()).map(DepartementEntity::getNom).orElse("Département inconnu"),
            users.findUsernameById(demand.getDemandeurId()));
    }

    private void requireFinanceAssignment(Integer actorId) {
        boolean financeMember = departements.findActiveForEmployeeUser(actorId).stream()
            .anyMatch(department -> "FINANCE".equalsIgnoreCase(department.getCode()));
        if (!financeMember) throw new ResponseStatusException(HttpStatus.FORBIDDEN,
            "Votre compte doit être affecté au département Finance pour traiter cette demande.");
    }

    private boolean canView(String actorRole, DemandeRecrutementEntity demand) {
        String statusCode = statuts.findById(demand.getStatutId()).map(value -> value.getCode()).orElse("");
        return switch (actorRole) {
            case "RH" -> List.of("A_VALIDER_RH", "SOUMISE", "A_VALIDER_FINANCE", "A_CREER_OFFRE", "OFFRE_CREEE", "REFUSEE", "ANNULEE").contains(statusCode);
            case "FINANCE" -> List.of("A_VALIDER_FINANCE", "A_CREER_OFFRE", "OFFRE_CREEE").contains(statusCode)
                || hasFinanceDecision(demand);
            case "ADMINISTRATION" -> List.of("A_CREER_OFFRE", "OFFRE_CREEE").contains(statusCode);
            default -> false;
        };
    }

    private boolean hasFinanceDecision(DemandeRecrutementEntity demand) {
        var financeStep = etapes.findByCircuitIdAndCode(demand.getCircuitId(), "VALIDATION_FINANCE");
        return financeStep.isPresent() && validations.existsByDemandeIdAndEtapeCircuitId(demand.getId(), financeStep.get().getId());
    }

    private boolean isActionable(DemandeRecrutementEntity demand, DemandStage stage) {
        String currentCode = statuts.findById(demand.getStatutId()).map(value -> value.getCode()).orElse("");
        return stage.statusCode.equals(currentCode) || (stage == DemandStage.RH && "SOUMISE".equals(currentCode));
    }

    private DemandStage stage(String role) {
        return switch (role) {
            case "RH" -> DemandStage.RH;
            case "FINANCE" -> DemandStage.FINANCE;
            case "ADMINISTRATION" -> DemandStage.ADMINISTRATION;
            default -> throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Rôle non autorisé.");
        };
    }

    private com.sirh.backend.modules.recrutement.entity.StatutDemandeEntity status(String code) {
        return statuts.findByCode(code).orElseThrow(() -> new ResponseStatusException(
            HttpStatus.INTERNAL_SERVER_ERROR, "Statut " + code + " manquant. Réimportez database/data-13-11-26.sql."));
    }

    private String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }

    private enum DemandStage {
        RH("A_VALIDER_RH", "VALIDATION_RH"),
        FINANCE("A_VALIDER_FINANCE", "VALIDATION_FINANCE"),
        ADMINISTRATION("A_CREER_OFFRE", null);
        private final String statusCode;
        private final String stepCode;
        DemandStage(String statusCode, String stepCode) {
            this.statusCode = statusCode;
            this.stepCode = stepCode;
        }
    }
}
