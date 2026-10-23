package com.gestion.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "contrat")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Contrat {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_contrat") private Integer idContrat;
    @OneToOne(optional = false) @JoinColumn(name = "id_candidature", nullable = false, unique = true)
    private Candidature candidature;
    @ManyToOne @JoinColumn(name = "id_emp") private Employe employe;
    @Column(name = "type_contrat", nullable = false, length = 20) private String typeContrat;
    @Column(name = "nom_salarie", nullable = false, length = 200) private String nomSalarie;
    @Column(name = "adresse_salarie", nullable = false, length = 500) private String adresseSalarie;
    @Column(name = "nom_employeur", nullable = false, length = 200) private String nomEmployeur;
    @Column(name = "adresse_employeur", nullable = false, length = 500) private String adresseEmployeur;
    @Column(name = "fonction", nullable = false, length = 200) private String fonction;
    @Column(name = "categorie_professionnelle", nullable = false, length = 150) private String categorieProfessionnelle;
    @Column(name = "indice_classification", nullable = false, length = 100) private String indiceClassification;
    @Column(name = "salaire", nullable = false) private Integer salaire;
    @Column(name = "duree_travail", nullable = false, length = 150) private String dureeTravail;
    @Column(name = "lieu_travail", nullable = false, length = 200) private String lieuTravail;
    @Column(name = "date_effet", nullable = false) private LocalDate dateEffet;
    @Column(name = "date_fin") private LocalDate dateFin;
    @Column(name = "motif_cdd", length = 500) private String motifCdd;
    @Column(name = "statut", nullable = false, length = 30) private String statut;
    @Column(name = "date_generation") private LocalDateTime dateGeneration;
    @Column(name = "date_acceptation") private LocalDateTime dateAcceptation;
    @Column(name = "pdf", columnDefinition = "bytea") private byte[] pdf;
}
