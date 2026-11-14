package com.sirh.backend.modules.recrutement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "statuts_demande_recrutement")
public class StatutDemandeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(nullable = false, unique = true, length = 30)
    private String code;
    @Column(nullable = false, length = 100)
    private String libelle;

    protected StatutDemandeEntity() { }
    public Integer getId() { return id; }
    public String getCode() { return code; }
    public String getLibelle() { return libelle; }
}
