package com.sirh.backend.modules.organisation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "departements")
public class DepartementEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(nullable = false, unique = true, length = 30)
    private String code;
    @Column(name = "code_matricule", nullable = false, unique = true, length = 5)
    private String codeMatricule;
    @Column(nullable = false, unique = true, length = 150)
    private String nom;
    @Column(nullable = false)
    private boolean actif;

    protected DepartementEntity() { }
    public Integer getId() { return id; }
    public String getCode() { return code; }
    public String getCodeMatricule() { return codeMatricule; }
    public String getNom() { return nom; }
    public boolean isActif() { return actif; }
}
