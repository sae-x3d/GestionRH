package com.gestion.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "droit_conge")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DroitConge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_droit_conge")
    private Integer idDroitConge;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_type_conge", nullable = false)
    private TypeConge typeConge;

    @Column(name = "anciennete_min_mois", nullable = false)
    @Builder.Default
    private Integer ancienneteMinMois = 0;

    @Column(name = "jours_par_an", nullable = false, precision = 6, scale = 1)
    private BigDecimal joursParAn;
}
