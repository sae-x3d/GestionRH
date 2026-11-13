package com.gestion.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "accreditation", indexes = {
        @Index(name = "idx_accreditation_user", columnList = "id_user"),
        @Index(name = "idx_accreditation_candidature", columnList = "id_candidature"),
        @Index(name = "idx_accreditation_statut", columnList = "statut")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Accreditation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_accreditation")
    private Integer idAccreditation;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_user", nullable = false)
    private Utilisateur utilisateur;

    @ManyToOne
    @JoinColumn(name = "id_candidature")
    private Candidature candidature;

    @Column(name = "type_accreditation", nullable = false, length = 150)
    private String typeAccreditation;

    @Column(name = "organisme", nullable = false, length = 200)
    private String organisme;

    @Column(name = "numero", length = 100)
    private String numero;

    @Column(name = "date_obtention", nullable = false)
    private LocalDate dateObtention;

    @Column(name = "date_expiration")
    private LocalDate dateExpiration;

    @Column(name = "statut", nullable = false, length = 40)
    private String statut;

    @Column(name = "document_nom_original", length = 255)
    private String documentNomOriginal;

    @Column(name = "document_chemin", length = 500)
    private String documentChemin;

    @Column(name = "commentaire", length = 1000)
    private String commentaire;

    @ManyToOne
    @JoinColumn(name = "id_verificateur")
    private Utilisateur verificateur;

    @Column(name = "date_verification")
    private LocalDateTime dateVerification;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation;
}
