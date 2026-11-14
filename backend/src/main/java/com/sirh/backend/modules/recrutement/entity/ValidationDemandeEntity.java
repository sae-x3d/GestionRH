package com.sirh.backend.modules.recrutement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "validations_demande")
public class ValidationDemandeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "demande_id", nullable = false)
    private Integer demandeId;

    @Column(name = "etape_circuit_id", nullable = false)
    private Integer etapeCircuitId;

    @Column(name = "validateur_id")
    private Integer validateurId;

    @Column(name = "decision_id", nullable = false)
    private Integer decisionId;

    @Column(columnDefinition = "TEXT")
    private String commentaire;

    @Column(name = "decidee_at", nullable = false)
    private OffsetDateTime decideeAt;

    protected ValidationDemandeEntity() { }

    public ValidationDemandeEntity(Integer demandeId, Integer etapeCircuitId, Integer validateurId,
            Integer decisionId, String commentaire, OffsetDateTime decideeAt) {
        this.demandeId = demandeId;
        this.etapeCircuitId = etapeCircuitId;
        this.validateurId = validateurId;
        this.decisionId = decisionId;
        this.commentaire = commentaire;
        this.decideeAt = decideeAt;
    }

    public Integer getEtapeCircuitId() { return etapeCircuitId; }
    public Integer getValidateurId() { return validateurId; }
    public Integer getDecisionId() { return decisionId; }
    public String getCommentaire() { return commentaire; }
    public OffsetDateTime getDecideeAt() { return decideeAt; }
}
