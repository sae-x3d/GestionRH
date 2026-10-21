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
@Table(name = "demande_status")
public class DemandeStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_demande_status")
    private Integer idDemandeStatus;

    @Column(name = "libelle_status", length = 50)
    private String libelleStatus;

    @OneToMany(mappedBy = "demandeStatus")
    private List<Demande> demandes = new ArrayList<>();

    public DemandeStatus() {
    }

    public Integer getIdDemandeStatus() {
        return idDemandeStatus;
    }

    public void setIdDemandeStatus(Integer idDemandeStatus) {
        this.idDemandeStatus = idDemandeStatus;
    }

    public String getLibelleStatus() {
        return libelleStatus;
    }

    public void setLibelleStatus(String libelleStatus) {
        this.libelleStatus = libelleStatus;
    }

    public List<Demande> getDemandes() {
        return demandes;
    }

    public void setDemandes(List<Demande> demandes) {
        this.demandes = demandes;
    }
}
