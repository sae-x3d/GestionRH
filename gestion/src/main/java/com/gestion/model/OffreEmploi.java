package com.gestion.model;

import com.gestion.enums.StatutOffre;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "offre_emploi")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OffreEmploi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_offre")
    private Integer idOffre;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_demande", nullable = false, unique = true)
    private Demande demande;

    @Column(name = "titre", length = 150, nullable = false)
    private String titre;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "lieu", length = 150)
    private String lieu;

    @Column(name = "type_contrat", length = 50)
    private String typeContrat;

    @Column(name = "salaire")
    private Integer salaire;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation;

    @Column(name = "date_publication")
    private LocalDateTime datePublication;

    @Column(name = "date_cloture")
    private LocalDateTime dateCloture;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", length = 30, nullable = false)
    private StatutOffre statut;

    @ManyToOne
    @JoinColumn(name = "id_auteur")
    private Utilisateur auteur;
}
