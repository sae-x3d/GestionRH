package com.gestion.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "poste")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Demande> demandes = new ArrayList<>();

    @OneToMany(mappedBy = "poste")
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Employe> employes = new ArrayList<>();

    @OneToMany(mappedBy = "poste")
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<PosteParDept> postesParDepartement = new ArrayList<>();
}
