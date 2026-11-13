package com.gestion.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "emp")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Employe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_emp")
    private Integer idEmp;

    @Column(name = "nom_emp", length = 255)
    private String nomEmp;

    @Column(name = "matricule", length = 30, unique = true)
    private String matricule;

    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "statut", length = 20)
    @Builder.Default
    private String statut = "ACTIF";

    @Column(name = "date_embauche")
    private LocalDateTime dateEmbauche;

    @Column(name = "date_depart")
    private LocalDateTime dateDepart;

    @ManyToOne
    @JoinColumn(name = "id_poste")
    private Poste poste;

    @ManyToOne
    @JoinColumn(name = "id_dept")
    private Departement departement;

    @ManyToOne
    @JoinColumn(name = "id_regime")
    private RegimeTravail regimeTravail;

    @Column(name = "salaire_emp")
    private Integer salaireEmp;

    @Column(name = "date_naissance")
    private java.time.LocalDate dateNaissance;

    @Column(name = "sexe", length = 20)
    private String sexe;

    @Column(name = "nationalite", length = 100)
    private String nationalite;

    @Column(name = "etat_civil", length = 50)
    private String etatCivil;

    @Column(name = "piece_identite", length = 100)
    private String pieceIdentite;

    @Column(name = "adresse", length = 255)
    private String adresse;

    @Column(name = "telephone", length = 50)
    private String telephone;

    @Column(name = "contact_urgence", length = 255)
    private String contactUrgence;

    @Column(name = "rib", length = 100)
    private String rib;

    @Column(name = "minutes_recuperation")
    @Builder.Default
    private Integer minutesRecuperation = 0;

    @ManyToOne
    @JoinColumn(name = "id_provenance")
    private Provenance provenance;

    @ManyToOne
    @JoinColumn(name = "id_candidature")
    private Candidature candidature;

    @OneToMany(mappedBy = "employe")
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Utilisateur> utilisateurs = new ArrayList<>();
}
