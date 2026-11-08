package com.gestion.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity @Table(name="jour_travaille", uniqueConstraints=@UniqueConstraint(name="uk_jour_travaille_emp_date",columnNames={"id_emp","date_jour"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class JourTravaille {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="id_jour_travaille") private Integer idJourTravaille;
    @ManyToOne(optional=false) @JoinColumn(name="id_emp",nullable=false) private Employe employe;
    @Column(name="date_jour",nullable=false) private LocalDate dateJour;
    @Column(name="minutes_brutes",nullable=false) private Integer minutesBrutes;
    @Column(name="minutes_net",nullable=false) private Integer minutesNet;
    @Column(name="minutes_normales",nullable=false) private Integer minutesNormales;
    @Column(name="minutes_supplementaires",nullable=false) private Integer minutesSupplementaires;
    @Column(name="date_calcul",nullable=false) private LocalDateTime dateCalcul;
}
