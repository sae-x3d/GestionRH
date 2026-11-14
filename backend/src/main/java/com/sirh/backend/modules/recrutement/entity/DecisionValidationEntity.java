package com.sirh.backend.modules.recrutement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "decisions_validation_demande")
public class DecisionValidationEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(nullable = false, unique = true)
    private String code;

    protected DecisionValidationEntity() { }
    public Integer getId() { return id; }
    public String getCode() { return code; }
}
