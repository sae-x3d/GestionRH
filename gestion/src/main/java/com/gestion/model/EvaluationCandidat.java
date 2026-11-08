package com.gestion.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "evaluation_candidat", indexes = {
        @Index(name = "idx_evaluation_entretien", columnList = "id_entretien")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvaluationCandidat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_evaluation")
    private Integer idEvaluation;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_entretien", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Entretien entretien;

    @Column(name = "note", nullable = false)
    private Integer note;

    @Column(name = "grille", columnDefinition = "TEXT")
    private String grille;

    @Column(name = "commentaire", length = 1000)
    private String commentaire;

    @ManyToOne
    @JoinColumn(name = "id_evaluateur")
    private Utilisateur evaluateur;

    @Column(name = "date_evaluation", nullable = false)
    private LocalDateTime dateEvaluation;
}
