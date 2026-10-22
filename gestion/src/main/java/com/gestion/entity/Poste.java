package com.gestion.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "poste")
public class Poste {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_poste")
    private Integer idPoste;

    @Column(name = "nom_poste", length = 100)
    private String nomPoste;

    @ManyToOne
    @JoinColumn(name = "id_role")
    private Role role;

    @Column(name = "salaire_max")
    private Integer salaireMax;

    @Column(name = "salaire_min")
    private Integer salaireMin;

    @OneToMany(mappedBy = "poste")
    private List<Demande> demandes = new ArrayList<>();

    @OneToMany(mappedBy = "poste")
    private List<Employe> employes = new ArrayList<>();

    @OneToMany(mappedBy = "poste")
    private List<PosteParDept> postesParDepartement = new ArrayList<>();

    public Poste() {
    }

    public Integer getIdPoste() {
        return idPoste;
    }

    public void setIdPoste(Integer idPoste) {
        this.idPoste = idPoste;
    }

    public String getNomPoste() {
        return nomPoste;
    }

    public void setNomPoste(String nomPoste) {
        this.nomPoste = nomPoste;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Integer getSalaireMax() {
        return salaireMax;
    }

    public void setSalaireMax(Integer salaireMax) {
        this.salaireMax = salaireMax;
    }

    public Integer getSalaireMin() {
        return salaireMin;
    }

    public void setSalaireMin(Integer salaireMin) {
        this.salaireMin = salaireMin;
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

    public List<PosteParDept> getPostesParDepartement() {
        return postesParDepartement;
    }

    public void setPostesParDepartement(List<PosteParDept> postesParDepartement) {
        this.postesParDepartement = postesParDepartement;
    }
}
