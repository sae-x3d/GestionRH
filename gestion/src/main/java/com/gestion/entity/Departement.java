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
@Table(name = "departement")
public class Departement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_dept")
    private Integer idDept;

    @Column(name = "nom_dept", length = 100)
    private String nomDept;

    @OneToMany(mappedBy = "departement")
    private List<PosteParDept> postesParDepartement = new ArrayList<>();

    @OneToMany(mappedBy = "departement")
    private List<Demande> demandes = new ArrayList<>();

    @OneToMany(mappedBy = "departement")
    private List<Employe> employes = new ArrayList<>();

    public Departement() {
    }

    public Integer getIdDept() {
        return idDept;
    }

    public void setIdDept(Integer idDept) {
        this.idDept = idDept;
    }

    public String getNomDept() {
        return nomDept;
    }

    public void setNomDept(String nomDept) {
        this.nomDept = nomDept;
    }

    public List<PosteParDept> getPostesParDepartement() {
        return postesParDepartement;
    }

    public void setPostesParDepartement(List<PosteParDept> postesParDepartement) {
        this.postesParDepartement = postesParDepartement;
    }

    public List<Demande> getDemandes() {
        return demandes;
    }

    public void setDemandes(List<Demande> demandes) {
        this.demandes = demandes;
    }

    public List<Employe> getEmployes() {
        return employes;
    }

    public void setEmployes(List<Employe> employes) {
        this.employes = employes;
    }
}
