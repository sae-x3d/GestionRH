package com.gestion.model;

import com.gestion.enums.StatutAnomalie;
import com.gestion.enums.TypeAnomaliePointage;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "anomalie_pointage", indexes = {
        @Index(name = "idx_anomalie_emp_date", columnList = "id_emp,date_anomalie"),
        @Index(name = "idx_anomalie_statut", columnList = "statut")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnomaliePointage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_anomalie")
    private Integer idAnomalie;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_emp", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Employe employe;

    @ManyToOne
    @JoinColumn(name = "id_pointage")
    private Pointage pointage;

    @Column(name = "date_anomalie", nullable = false)
    private LocalDate dateAnomalie;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_anomalie", nullable = false, length = 40)
    private TypeAnomaliePointage typeAnomalie;

    @Column(name = "minutes")
    private Integer minutes;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    private StatutAnomalie statut;

    @Column(name = "commentaire", length = 500)
    private String commentaire;

    @ManyToOne
    @JoinColumn(name = "id_traite_par")
    private Utilisateur traitePar;

    @Column(name = "date_traitement")
    private LocalDateTime dateTraitement;
}
