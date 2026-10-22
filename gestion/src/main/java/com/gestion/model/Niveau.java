package com.gestion.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "niveau")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Niveau {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_niveau")
    private Integer idNiveau;

    @Column(name = "libelle", length = 50, nullable = false)
    private String libelle;
}
