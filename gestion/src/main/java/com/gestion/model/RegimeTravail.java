package com.gestion.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "regime_travail")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegimeTravail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_regime")
    private Integer idRegime;

    @Column(name = "libelle", nullable = false, length = 100)
    private String libelle;

    @Column(name = "heures_hebdomadaires", precision = 4, scale = 1)
    private BigDecimal heuresHebdomadaires;

    @Column(name = "jours_ouvres", length = 40)
    @Builder.Default
    private String joursOuvres = "LUN,MAR,MER,JEU,VEN";

    @Column(name = "actif", nullable = false)
    @Builder.Default
    private boolean actif = true;
}
