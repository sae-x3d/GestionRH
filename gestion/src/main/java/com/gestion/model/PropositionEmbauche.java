package com.gestion.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "proposition_embauche")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PropositionEmbauche {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_proposition")
    private Integer idProposition;

    @OneToOne
    @JoinColumn(name = "id_candidature", nullable = false, unique = true)
    private Candidature candidature;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_offre", nullable = false)
    private OffreEmploi offre;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_candidat_user", nullable = false)
    private Utilisateur candidat;

    @ManyToOne
    @JoinColumn(name = "id_rh_user")
    private Utilisateur rh;

    // Conditions d'embauche proposées
    @Column(name = "poste_propose", length = 150)
    private String postePropose;

    @Column(name = "departement_propose", length = 150)
    private String departementPropose;

    @Column(name = "lieu_travail", length = 150)
    private String lieuTravail;

    @Column(name = "type_contrat", length = 50)
    private String typeContrat;

    @Column(name = "salaire_propose")
    private Integer salairePropose;

    @Column(name = "periode_essai_mois")
    private Integer periodeEssaiMois;

    @Column(name = "date_embauche_souhaitee")
    private LocalDate dateEmbaucheSouhaitee;

    @Column(name = "avantages", columnDefinition = "TEXT")
    private String avantages;

    @Column(name = "autres_conditions", columnDefinition = "TEXT")
    private String autresConditions;

    // Suivi proposition
    @Column(name = "date_proposition", nullable = false)
    private LocalDateTime dateProposition;

    @Column(name = "date_expiration")
    private LocalDateTime dateExpiration;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "statut", length = 20, nullable = false)
    private String statut;

    @Column(name = "date_reponse")
    private LocalDateTime dateReponse;

    @Column(name = "motif_refus", columnDefinition = "TEXT")
    private String motifRefus;
}
