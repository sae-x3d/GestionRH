package com.gestion.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "info")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Info {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_info")
    private Integer idInfo;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_candidature", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Candidature candidature;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_parcours", nullable = false)
    private Parcours parcours;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_niveau", nullable = false)
    private Niveau niveau;

    @Column(name = "domaine", length = 255)
    private String domaine;

    @Column(name = "commentaire", columnDefinition = "TEXT")
    private String commentaire;
}
