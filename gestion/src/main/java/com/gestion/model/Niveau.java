package com.gestion.model;

import com.gestion.enums.CategorieNiveau;
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

    @Enumerated(EnumType.STRING)
    @Column(name = "categorie", length = 20, nullable = false)
    private CategorieNiveau categorie;
}
