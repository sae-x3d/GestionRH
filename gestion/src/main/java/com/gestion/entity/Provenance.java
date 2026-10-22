package com.gestion.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "provenance")
public class Provenance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_provenance")
    private Integer idProvenance;

    @Column(name = "libelle", length = 100)
    private String libelle;

    @OneToMany(mappedBy = "provenance")
    private List<Employe> employes = new ArrayList<>();

    public Provenance() {
    }

    public Integer getIdProvenance() {
        return idProvenance;
    }

    public void setIdProvenance(Integer idProvenance) {
        this.idProvenance = idProvenance;
    }

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    public List<Employe> getEmployes() {
        return employes;
    }

    public void setEmployes(List<Employe> employes) {
        this.employes = employes;
    }
}
