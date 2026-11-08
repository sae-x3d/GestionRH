package com.gestion.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "solde_conge")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SoldeConge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_solde_conge")
    private Integer idSoldeConge;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_emp", nullable = false)
    private Employe employe;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_type_conge", nullable = false)
    private TypeConge typeConge;

    @Column(name = "date_acquisition", nullable = false)
    private LocalDate dateAcquisition;

    @Column(name = "date_expiration")
    private LocalDate dateExpiration;

    @Column(name = "quantite_initiale", nullable = false, precision = 6, scale = 1)
    private BigDecimal quantiteInitiale;

    @Column(name = "quantite_restante", nullable = false, precision = 6, scale = 1)
    private BigDecimal quantiteRestante;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation;
}
