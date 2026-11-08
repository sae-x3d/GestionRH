package com.gestion.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalTime;

@Entity
@Table(name = "parametre_temps_travail")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParametreTempsTravail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_parametre")
    private Integer idParametre;

    @Column(name = "heures_hebdomadaires", nullable = false, precision = 5, scale = 1)
    @Builder.Default
    private BigDecimal heuresHebdomadaires = new BigDecimal("35.0");

    @Column(name = "heure_debut", nullable = false)
    @Builder.Default
    private LocalTime heureDebut = LocalTime.of(8, 0);

    @Column(name = "heure_fin", nullable = false)
    @Builder.Default
    private LocalTime heureFin = LocalTime.of(17, 0);

    @Column(name = "pause_minutes", nullable = false)
    @Builder.Default
    private Integer pauseMinutes = 60;

    @Column(name = "tolerance_retard_minutes", nullable = false)
    @Builder.Default
    private Integer toleranceRetardMinutes = 10;

    @Column(name = "seuil_hebdo_heures_sup", nullable = false, precision = 5, scale = 1)
    @Builder.Default
    private BigDecimal seuilHebdoHeuresSup = new BigDecimal("35.0");

    @Column(name = "seuil_tranche_25_minutes")
    @Builder.Default
    private Integer seuilTranche25Minutes = 480;

    @Column(name = "majoration_nuit") @Builder.Default private Integer majorationNuit = 25;
    @Column(name = "majoration_dimanche") @Builder.Default private Integer majorationDimanche = 50;
    @Column(name = "majoration_ferie") @Builder.Default private Integer majorationFerie = 100;

    @Column(name = "actif", nullable = false)
    @Builder.Default
    private boolean actif = true;
}
