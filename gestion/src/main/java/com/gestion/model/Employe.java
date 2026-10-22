package com.gestion.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "emp")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Employe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_emp")
    private Integer idEmp;

    @Column(name = "nom_emp", length = 255)
    private String nomEmp;

    @Column(name = "date_embauche")
    private LocalDateTime dateEmbauche;

    @Column(name = "date_depart")
    private LocalDateTime dateDepart;

    @ManyToOne
    @JoinColumn(name = "id_poste")
    private Poste poste;

    @ManyToOne
    @JoinColumn(name = "id_dept")
    private Departement departement;

    @Column(name = "salaire_emp")
    private Integer salaireEmp;

    @ManyToOne
    @JoinColumn(name = "id_provenance")
    private Provenance provenance;

    @OneToMany(mappedBy = "employe")
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Utilisateur> utilisateurs = new ArrayList<>();
}
