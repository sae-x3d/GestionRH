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
@Table(name = "motif")
public class Motif {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_motif")
    private Integer idMotif;

    @Column(name = "libelle", length = 100)
    private String libelle;

    @OneToMany(mappedBy = "motif")
    private List<Demande> demandes = new ArrayList<>();

    public Motif() {
    }

    public Integer getIdMotif() {
        return idMotif;
    }

    public void setIdMotif(Integer idMotif) {
        this.idMotif = idMotif;
    }

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    public List<Demande> getDemandes() {
        return demandes;
    }

    public void setDemandes(List<Demande> demandes) {
        this.demandes = demandes;
    }
}
