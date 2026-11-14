package com.sirh.backend.modules.recrutement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "etapes_circuit_validation")
public class EtapeCircuitEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(name = "circuit_id", nullable = false)
    private Integer circuitId;
    @Column(nullable = false)
    private String code;
    @Column(nullable = false)
    private String libelle;

    protected EtapeCircuitEntity() { }
    public Integer getId() { return id; }
    public Integer getCircuitId() { return circuitId; }
    public String getCode() { return code; }
    public String getLibelle() { return libelle; }
}
