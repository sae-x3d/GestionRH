package com.gestion.model;

import com.gestion.enums.TypeVariablePaie;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "variable_paie", indexes = {
        @Index(name = "idx_variable_paie_periode", columnList = "id_periode_paie"),
        @Index(name = "idx_variable_paie_emp", columnList = "id_emp")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VariablePaie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_variable_paie")
    private Integer idVariablePaie;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_periode_paie", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private PeriodePaie periodePaie;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_emp", nullable = false)
    private Employe employe;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_variable", nullable = false, length = 30)
    private TypeVariablePaie typeVariable;

    @Column(name = "libelle", nullable = false, length = 120)
    private String libelle;

    @Column(name = "quantite", precision = 8, scale = 2)
    private BigDecimal quantite;

    @Column(name = "montant", precision = 12, scale = 2)
    private BigDecimal montant;

    @Column(name = "reference", length = 80)
    private String reference;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation;
}
