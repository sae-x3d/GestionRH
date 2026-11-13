package com.gestion.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "affectation", indexes = {
        @Index(name = "idx_affectation_employe", columnList = "id_emp"),
        @Index(name = "idx_affectation_departement", columnList = "id_dept"),
        @Index(name = "idx_affectation_manager", columnList = "id_manager")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Affectation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_affectation")
    private Integer idAffectation;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_emp", nullable = false)
    private Employe employe;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_poste", nullable = false)
    private Poste poste;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_dept", nullable = false)
    private Departement departement;

    @ManyToOne
    @JoinColumn(name = "id_manager")
    private Utilisateur manager;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin")
    private LocalDate dateFin;

    @Column(name = "statut", nullable = false, length = 30)
    private String statut;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation;
}
