package com.gestion.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "question")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_question")
    private Integer idQuestion;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_offre", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private OffreEmploi offre;

    @Column(name = "libelle", length = 255, nullable = false)
    private String libelle;

    @Column(name = "ordre", nullable = false)
    private Integer ordre;
}
