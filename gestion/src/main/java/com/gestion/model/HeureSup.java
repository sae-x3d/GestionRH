package com.gestion.model;

import com.gestion.enums.StatutHeureSup;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "heure_sup", indexes = {
        @Index(name = "idx_heure_sup_emp_date", columnList = "id_emp,date_heure_sup"),
        @Index(name = "idx_heure_sup_statut", columnList = "statut")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HeureSup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_heure_sup")
    private Integer idHeureSup;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_emp", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Employe employe;

    @Column(name = "date_heure_sup", nullable = false)
    private LocalDate dateHeureSup;

    @Column(name = "minutes", nullable = false)
    private Integer minutes;

    @Column(name = "taux_majoration", nullable = false)
    private Integer tauxMajoration;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    private StatutHeureSup statut;

    @Column(name = "motif", length = 500)
    private String motif;

    @ManyToOne
    @JoinColumn(name = "id_periode_paie")
    private PeriodePaie periodePaie;

    @ManyToOne
    @JoinColumn(name = "id_valide_par")
    private Utilisateur validePar;

    @Column(name = "date_validation")
    private LocalDateTime dateValidation;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation;
}
