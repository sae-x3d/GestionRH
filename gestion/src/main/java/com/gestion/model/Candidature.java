package com.gestion.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "candidature")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Candidature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_candidature")
    private Integer idCandidature;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_user", nullable = false)
    private Utilisateur utilisateur;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_offre", nullable = false)
    private OffreEmploi offre;

    @Column(name = "cv_num", length = 255)
    private String cvNum;

    @Column(name = "date_candidature", nullable = false)
    private LocalDateTime dateCandidature;

    @Column(name = "date_decision")
    private LocalDateTime dateDecision;

    @Column(name = "salaire_convenu")
    private Integer salaireConvenu;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_candidature_status", nullable = false)
    private CandidatureStatus statut;

    @OneToMany(mappedBy = "candidature", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Info> parcours = new ArrayList<>();

    @OneToMany(mappedBy = "candidature", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<Reponse> reponses = new HashSet<>();

    @OneToMany(mappedBy = "candidature", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<Entretien> entretiens = new HashSet<>();

    @OneToMany(mappedBy = "candidature", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<ReponseContrat> reponsesContrat = new ArrayList<>();
}
