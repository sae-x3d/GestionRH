package com.gestion.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_candidature_status", nullable = false)
    private CandidatureStatus statut;

    @OneToMany(mappedBy = "candidature", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Info> parcours = new ArrayList<>();
}
