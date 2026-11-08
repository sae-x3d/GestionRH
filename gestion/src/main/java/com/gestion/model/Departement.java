package com.gestion.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "departement")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Departement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_dept")
    private Integer idDept;

    @Column(name = "nom_dept", length = 100)
    private String nomDept;

    @Column(name = "effectif")
    private Integer effectif;

    @Column(name = "enveloppe_budgetaire")
    private Integer enveloppeBudgetaire;

    @Column(name = "budget_consomme")
    private Integer budgetConsomme;

    @OneToMany(mappedBy = "departement")
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<PosteParDept> postesParDepartement = new ArrayList<>();

    @OneToMany(mappedBy = "departement")
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Demande> demandes = new ArrayList<>();

    @OneToMany(mappedBy = "departement")
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Employe> employes = new ArrayList<>();
}
