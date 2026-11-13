package com.gestion.model;

import com.gestion.enums.StatutPaie;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "inscription_paie", indexes = {
        @Index(name = "idx_inscription_paie_employe", columnList = "id_emp")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InscriptionPaie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_inscription_paie")
    private Integer idInscriptionPaie;

    @OneToOne(optional = false)
    @JoinColumn(name = "id_emp", nullable = false, unique = true)
    private Employe employe;

    @Column(name = "numero_paie", unique = true, length = 50)
    private String numeroPaie;

    @Column(name = "organisme", length = 150)
    private String organisme;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 30)
    private StatutPaie statut;

    @Column(name = "date_inscription", nullable = false)
    private LocalDateTime dateInscription;

    @Column(name = "date_debut")
    private LocalDate dateDebut;

    @Column(name = "commentaire", length = 500)
    private String commentaire;
}
