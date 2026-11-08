package com.gestion.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "question_contrat")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionContrat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_question_contrat")
    private Integer idQuestionContrat;

    @Column(name = "libelle", length = 255, nullable = false)
    private String libelle;

    @Column(name = "ordre", nullable = false)
    private Integer ordre;

    @Column(name = "actif", nullable = false)
    @Builder.Default
    private boolean actif = true;
}
