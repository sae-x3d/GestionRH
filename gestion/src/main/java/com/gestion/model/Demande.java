package com.gestion.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "demande")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Demande {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_demande")
    private Integer idDemande;

    @ManyToOne
    @JoinColumn(name = "id_poste")
    private Poste poste;

    @ManyToOne
    @JoinColumn(name = "id_dept")
    private Departement departement;

    @ManyToOne
    @JoinColumn(name = "id_demandeur")
    private Utilisateur demandeur;

    @ManyToOne
    @JoinColumn(name = "id_emp_remplace")
    private Employe empRemplace;

    @Column(name = "date_creation")
    private LocalDateTime dateCreation;

    @ManyToOne
    @JoinColumn(name = "id_motif")
    private Motif motif;

    @Column(name = "budget")
    private Integer budget;

    @Column(name = "date_souhaite")
    private LocalDateTime dateSouhaite;

    @ManyToOne
    @JoinColumn(name = "id_demande_status")
    private DemandeStatus demandeStatus;

    @Column(name = "date_reponse")
    private LocalDateTime dateReponse;
}
