package com.sirh.backend.modules.organisation.entity;

import java.math.BigDecimal;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "postes")
public class PosteEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(name = "departement_id")
    private Integer departementId;
    @Column(name = "code_matricule", nullable = false, length = 5)
    private String codeMatricule;
    @Column(nullable = false, length = 150)
    private String intitule;
    @Column(name = "salaire_base_min", nullable = false, precision = 14, scale = 2)
    private BigDecimal salaireBaseMin;
    @Column(name = "salaire_base_max", nullable = false, precision = 14, scale = 2)
    private BigDecimal salaireBaseMax;
    @Column(nullable = false)
    private boolean actif;

    protected PosteEntity() { }
    public Integer getId() { return id; }
    public Integer getDepartementId() { return departementId; }
    public String getCodeMatricule() { return codeMatricule; }
    public String getIntitule() { return intitule; }
    public BigDecimal getSalaireBaseMin() { return salaireBaseMin; }
    public BigDecimal getSalaireBaseMax() { return salaireBaseMax; }
    public boolean isActif() { return actif; }
}
