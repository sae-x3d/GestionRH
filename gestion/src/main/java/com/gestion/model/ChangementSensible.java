package com.gestion.model;

import com.gestion.enums.ChampSensible;
import com.gestion.enums.StatutChangementSensible;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "changement_sensible")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChangementSensible {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_changement")
    private Integer idChangement;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_emp", nullable = false)
    private Employe employe;

    @Enumerated(EnumType.STRING)
    @Column(name = "champ", nullable = false, length = 40)
    private ChampSensible champ;

    @Column(name = "ancienne_valeur", length = 500)
    private String ancienneValeur;

    @Column(name = "nouvelle_valeur", length = 500)
    private String nouvelleValeur;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 30)
    private StatutChangementSensible statut;

    @ManyToOne
    @JoinColumn(name = "id_demandeur")
    private Utilisateur demandeur;

    @ManyToOne
    @JoinColumn(name = "id_validateur")
    private Utilisateur validateur;

    @Column(name = "motif", length = 500)
    private String motif;

    @Column(name = "date_soumission", nullable = false)
    private LocalDateTime dateSoumission;

    @Column(name = "date_validation")
    private LocalDateTime dateValidation;
}
