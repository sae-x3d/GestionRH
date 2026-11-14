package com.sirh.backend.modules.recrutement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "circuits_validation_demande")
public class CircuitValidationEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(nullable = false, unique = true, length = 40)
    private String code;
    @Column(nullable = false)
    private boolean actif;

    protected CircuitValidationEntity() { }
    public Integer getId() { return id; }
    public boolean isActif() { return actif; }
}
