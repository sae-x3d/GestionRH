package com.gestion.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "jour_ferie")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JourFerie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_jour_ferie")
    private Integer idJourFerie;

    @Column(name = "date_jour_ferie", nullable = false, unique = true)
    private LocalDate dateJourFerie;

    @Column(name = "libelle", nullable = false, length = 100)
    private String libelle;
}
