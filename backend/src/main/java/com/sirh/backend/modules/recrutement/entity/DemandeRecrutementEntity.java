package com.sirh.backend.modules.recrutement.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "demandes_recrutement")
public class DemandeRecrutementEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(name = "departement_id", nullable = false)
    private Integer departementId;
    @Column(name = "demandeur_id", nullable = false)
    private Integer demandeurId;
    @Column(name = "poste_id", nullable = false)
    private Integer posteId;
    @Column(name = "motif_id", nullable = false)
    private Integer motifId;
    @Column(nullable = false)
    private Integer effectif;
    @Column(name = "budget_salaire_base", precision = 14, scale = 2)
    private BigDecimal budgetSalaireBase;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String justification;
    @Column(name = "date_souhaitee", nullable = false)
    private LocalDate dateSouhaitee;
    @Column(name = "statut_id", nullable = false)
    private Integer statutId;
    @Column(name = "circuit_id", nullable = false)
    private Integer circuitId;
    @Column(name = "soumise_at")
    private OffsetDateTime soumiseAt;
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected DemandeRecrutementEntity() { }

    public DemandeRecrutementEntity(Integer departementId, Integer demandeurId, Integer posteId,
            Integer motifId, Integer effectif, BigDecimal budgetSalaireBase, String justification,
            LocalDate dateSouhaitee, Integer statutId, Integer circuitId, OffsetDateTime now) {
        this.departementId = departementId;
        this.demandeurId = demandeurId;
        this.posteId = posteId;
        this.motifId = motifId;
        this.effectif = effectif;
        this.budgetSalaireBase = budgetSalaireBase;
        this.justification = justification;
        this.dateSouhaitee = dateSouhaitee;
        this.statutId = statutId;
        this.circuitId = circuitId;
        this.soumiseAt = now;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public Integer getId() { return id; }
    public Integer getDepartementId() { return departementId; }
    public Integer getPosteId() { return posteId; }
    public Integer getMotifId() { return motifId; }
    public Integer getStatutId() { return statutId; }
    public Integer getEffectif() { return effectif; }
    public BigDecimal getBudgetSalaireBase() { return budgetSalaireBase; }
    public String getJustification() { return justification; }
    public LocalDate getDateSouhaitee() { return dateSouhaitee; }
    public OffsetDateTime getSoumiseAt() { return soumiseAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public Integer getDemandeurId() { return demandeurId; }
    public Integer getCircuitId() { return circuitId; }
    public void updateStatus(Integer statusId, OffsetDateTime now) {
        this.statutId = statusId;
        this.updatedAt = now;
    }
    public void cancel(Integer statutId, OffsetDateTime now) {
        this.statutId = statutId;
        this.updatedAt = now;
    }
}
