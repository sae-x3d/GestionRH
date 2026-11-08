package com.gestion.model;

import com.gestion.enums.TypeJourPlanning;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "planning_jour", uniqueConstraints = {
        @UniqueConstraint(name = "uk_planning_jour_date", columnNames = {"id_planning", "date_jour"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanningJour {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_planning_jour")
    private Integer idPlanningJour;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_planning", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Planning planning;

    @Column(name = "date_jour", nullable = false)
    private LocalDate dateJour;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_jour", nullable = false, length = 20)
    private TypeJourPlanning typeJour;

    @Column(name = "heure_debut")
    private LocalTime heureDebut;

    @Column(name = "heure_fin")
    private LocalTime heureFin;

    @Column(name = "pause_minutes")
    private Integer pauseMinutes;
}
