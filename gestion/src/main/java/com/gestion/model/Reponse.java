package com.gestion.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "reponse")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_reponse")
    private Integer idReponse;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_candidature", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Candidature candidature;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_question", nullable = false)
    private Question question;

    @Column(name = "valeur", columnDefinition = "TEXT", nullable = false)
    private String valeur;
}
