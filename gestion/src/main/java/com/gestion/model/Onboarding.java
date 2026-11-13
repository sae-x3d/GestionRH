package com.gestion.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "onboarding", indexes = {
        @Index(name = "idx_onboarding_statut", columnList = "statut"),
        @Index(name = "idx_onboarding_employe", columnList = "id_emp")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Onboarding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_onboarding")
    private Integer idOnboarding;

    @OneToOne(optional = false)
    @JoinColumn(name = "id_contrat", nullable = false, unique = true)
    private Contrat contrat;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_emp", nullable = false)
    private Employe employe;

    @OneToOne
    @JoinColumn(name = "id_affectation")
    private Affectation affectation;

    @Column(name = "statut", nullable = false, length = 40)
    private String statut;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_cloture")
    private LocalDate dateCloture;

    @Column(name = "decision_essai", length = 40)
    private String decisionEssai;

    @Column(name = "commentaire", length = 1000)
    private String commentaire;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation;

    @OneToMany(mappedBy = "onboarding", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordre ASC")
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<OnboardingTache> taches = new ArrayList<>();

    @OneToMany(mappedBy = "onboarding", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("dateUpload ASC")
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Document> documents = new ArrayList<>();
}
