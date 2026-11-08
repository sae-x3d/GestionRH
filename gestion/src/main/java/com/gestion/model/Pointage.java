package com.gestion.model;

import com.gestion.enums.StatutPointage;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "pointage", uniqueConstraints = {
        @UniqueConstraint(name = "uk_pointage_emp_date", columnNames = {"id_emp", "date_pointage"})
}, indexes = {
        @Index(name = "idx_pointage_emp_date", columnList = "id_emp,date_pointage")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pointage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pointage")
    private Integer idPointage;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_emp", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Employe employe;

    @Column(name = "date_pointage", nullable = false)
    private LocalDate datePointage;

    @Column(name = "heure_arrivee")
    private LocalTime heureArrivee;

    @Column(name = "heure_depart")
    private LocalTime heureDepart;

    @Column(name = "pause_minutes")
    private Integer pauseMinutes;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 30)
    private StatutPointage statut;

    @Column(name = "commentaire", length = 500)
    private String commentaire;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation;

    @Column(name = "date_modification")
    private LocalDateTime dateModification;
}
