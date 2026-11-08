package com.gestion.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "onboarding_tache", indexes = {
        @Index(name = "idx_onboarding_tache_onboarding", columnList = "id_onboarding")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OnboardingTache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tache")
    private Integer idTache;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_onboarding", nullable = false)
    private Onboarding onboarding;

    @Column(name = "libelle", nullable = false, length = 255)
    private String libelle;

    @Column(name = "ordre", nullable = false)
    private Integer ordre;

    @Column(name = "obligatoire", nullable = false)
    private boolean obligatoire;

    @Column(name = "responsable", length = 40)
    private String responsable;

    @Column(name = "date_echeance")
    private LocalDate dateEcheance;

    @Column(name = "fait", nullable = false)
    private boolean fait;

    @Column(name = "date_realisation")
    private LocalDate dateRealisation;

    @Column(name = "commentaire", length = 500)
    private String commentaire;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation;
}
