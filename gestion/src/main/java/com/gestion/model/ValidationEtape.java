package com.gestion.model;

import com.gestion.enums.RoleValidation;
import com.gestion.enums.StatutValidation;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "validation_etape",
        uniqueConstraints = @UniqueConstraint(name = "uk_validation_etape_ordre",
                columnNames = {"id_dossier", "ordre"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValidationEtape {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_etape")
    private Integer idEtape;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_dossier", nullable = false)
    private ValidationDossier dossier;

    @Column(name = "ordre", nullable = false)
    private Integer ordre;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_requis", nullable = false, length = 30)
    private RoleValidation roleRequis;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 30)
    private StatutValidation statut;

    @ManyToOne
    @JoinColumn(name = "id_decideur")
    private Utilisateur decideur;

    @Column(name = "date_decision")
    private LocalDateTime dateDecision;

    @Column(name = "commentaire", length = 1000)
    private String commentaire;
}
