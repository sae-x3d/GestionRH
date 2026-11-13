package com.gestion.model;

import com.gestion.enums.StatutDemandeConge;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "demande_conge")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DemandeConge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_demande_conge")
    private Integer idDemandeConge;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_emp", nullable = false)
    private Employe employe;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_type_conge", nullable = false)
    private TypeConge typeConge;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin", nullable = false)
    private LocalDate dateFin;

    @Column(name = "demi_jour_debut", nullable = false)
    @Builder.Default
    private boolean demiJourDebut = false;

    @Column(name = "demi_jour_fin", nullable = false)
    @Builder.Default
    private boolean demiJourFin = false;

    @Column(name = "nombre_jours", nullable = false, precision = 6, scale = 1)
    private BigDecimal nombreJours;

    @Column(name = "motif", length = 500)
    private String motif;

    @Column(name = "justificatif_chemin", length = 500)
    private String justificatifChemin;

    @Column(name = "justificatif_nom", length = 255)
    private String justificatifNom;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 30)
    private StatutDemandeConge statut;

    @ManyToOne
    @JoinColumn(name = "id_manager_validateur")
    private Utilisateur managerValidateur;

    @Column(name = "date_validation_manager")
    private LocalDateTime dateValidationManager;

    @Column(name = "commentaire_manager", length = 500)
    private String commentaireManager;

    @ManyToOne
    @JoinColumn(name = "id_rh_validateur")
    private Utilisateur rhValidateur;

    @ManyToOne
    @JoinColumn(name = "id_direction_validateur")
    private Utilisateur directionValidateur;

    @Column(name = "date_validation_direction")
    private LocalDateTime dateValidationDirection;

    @Column(name = "commentaire_direction", length = 500)
    private String commentaireDirection;

    @Column(name = "date_validation_rh")
    private LocalDateTime dateValidationRh;

    @Column(name = "commentaire_rh", length = 500)
    private String commentaireRh;

    @Column(name = "date_soumission")
    private LocalDateTime dateSoumission;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation;
}
