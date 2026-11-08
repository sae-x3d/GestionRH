package com.gestion.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "type_conge")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TypeConge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_type_conge")
    private Integer idTypeConge;

    @Column(name = "code", nullable = false, unique = true, length = 40)
    private String code;

    @Column(name = "libelle", nullable = false, length = 100)
    private String libelle;

    @Column(name = "decompte_solde", nullable = false)
    @Builder.Default
    private boolean decompteSolde = true;

    @Column(name = "justificatif_requis", nullable = false)
    @Builder.Default
    private boolean justificatifRequis = false;

    @Column(name = "preavis_jours")
    @Builder.Default
    private Integer preavisJours = 0;

    @Column(name = "duree_max_jours", precision = 6, scale = 1)
    private BigDecimal dureeMaxJours;

    @Column(name = "validation_direction")
    @Builder.Default
    private Boolean validationDirection = false;

    @Column(name = "direction_a_partir_jours", precision = 6, scale = 1)
    private BigDecimal directionAPartirJours;

    @Column(name = "actif", nullable = false)
    @Builder.Default
    private boolean actif = true;
}
