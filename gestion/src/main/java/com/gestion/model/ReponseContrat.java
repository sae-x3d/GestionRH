package com.gestion.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "reponse_contrat")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReponseContrat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_reponse_contrat")
    private Integer idReponseContrat;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_candidature", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Candidature candidature;

    @ManyToOne
    @JoinColumn(name = "id_question_contrat")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private QuestionContrat question;

    @Column(name = "code", length = 30)
    private String code;

    @Column(name = "accepte", nullable = false)
    private boolean accepte;

    @Column(name = "proposition", columnDefinition = "TEXT")
    private String proposition;

    @Column(name = "numero_tour", nullable = false)
    private Integer numeroTour;

    @Column(name = "auteur", length = 20, nullable = false)
    private String auteur;

    @Column(name = "date_reponse", nullable = false)
    private LocalDateTime dateReponse;
}
