package com.gestion.model;

import com.gestion.enums.TypeMouvementCarriere;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "historique_carriere")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistoriqueCarriere {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_historique_carriere")
    private Integer idHistoriqueCarriere;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_emp", nullable = false)
    private Employe employe;

    @ManyToOne
    @JoinColumn(name = "id_poste")
    private Poste poste;

    @ManyToOne
    @JoinColumn(name = "id_dept")
    private Departement departement;

    @ManyToOne
    @JoinColumn(name = "id_manager")
    private Employe manager;

    @Column(name = "date_effet", nullable = false)
    private LocalDate dateEffet;

    @Enumerated(EnumType.STRING)
    @Column(name = "motif", nullable = false, length = 30)
    private TypeMouvementCarriere motif;

    @Column(name = "commentaire", length = 500)
    private String commentaire;

    @ManyToOne
    @JoinColumn(name = "id_auteur")
    private Utilisateur auteur;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation;
}
