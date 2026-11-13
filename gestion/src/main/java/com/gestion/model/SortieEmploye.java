package com.gestion.model;

import com.gestion.enums.StatutSortie;
import com.gestion.enums.TypeSortie;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "sortie_employe", indexes = {
        @Index(name = "idx_sortie_employe_emp", columnList = "id_emp"),
        @Index(name = "idx_sortie_employe_statut", columnList = "statut")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SortieEmploye {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_sortie")
    private Integer idSortie;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_emp", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Employe employe;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_sortie", nullable = false, length = 30)
    private TypeSortie typeSortie;

    @Column(name = "date_sortie", nullable = false)
    private LocalDate dateSortie;

    @Column(name = "date_notification")
    private LocalDate dateNotification;

    @Column(name = "motif", length = 500)
    private String motif;

    @Column(name = "preavis_jours")
    private Integer preavisJours;

    @Column(name = "solde_tout_compte")
    private Integer soldeToutCompte;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 30)
    private StatutSortie statut;

    @Column(name = "commentaire", length = 1000)
    private String commentaire;

    @ManyToOne
    @JoinColumn(name = "id_traite_par")
    private Utilisateur traitePar;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation;

    @Column(name = "date_validation")
    private LocalDateTime dateValidation;
}
