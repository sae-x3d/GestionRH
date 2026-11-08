package com.gestion.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "consommation_conge")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConsommationConge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_consommation_conge")
    private Integer idConsommationConge;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_solde_conge", nullable = false)
    private SoldeConge soldeConge;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_demande_conge", nullable = false)
    private DemandeConge demandeConge;

    @Column(name = "quantite", nullable = false, precision = 6, scale = 1)
    private BigDecimal quantite;

    @Column(name = "date_consommation", nullable = false)
    private LocalDateTime dateConsommation;
}
