package com.gestion.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "historique_salaire")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistoriqueSalaire {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_historique_salaire")
    private Integer idHistoriqueSalaire;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_emp", nullable = false)
    private Employe employe;

    @Column(name = "montant", nullable = false)
    private Integer montant;

    @Column(name = "date_effet", nullable = false)
    private LocalDate dateEffet;

    @Column(name = "motif", length = 255)
    private String motif;

    @ManyToOne
    @JoinColumn(name = "id_auteur")
    private Utilisateur auteur;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation;
}
