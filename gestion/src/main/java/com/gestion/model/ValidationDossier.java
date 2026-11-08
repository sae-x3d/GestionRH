package com.gestion.model;

import com.gestion.enums.StatutDossierValidation;
import com.gestion.enums.TypeObjetValidation;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "validation_dossier",
        uniqueConstraints = @UniqueConstraint(name = "uk_validation_dossier_objet",
                columnNames = {"type_objet", "id_objet", "numero_tentative"}),
        indexes = {
                @Index(name = "idx_validation_dossier_statut", columnList = "statut"),
                @Index(name = "idx_validation_dossier_objet", columnList = "type_objet,id_objet")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValidationDossier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_dossier")
    private Integer idDossier;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_objet", nullable = false, length = 30)
    private TypeObjetValidation typeObjet;

    @Column(name = "id_objet", nullable = false)
    private Integer idObjet;

    @Column(name = "numero_tentative", nullable = false)
    private Integer numeroTentative;

    @Column(name = "departement_id")
    private Integer departementId;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 30)
    private StatutDossierValidation statut;

    @Column(name = "etape_courante", nullable = false)
    private Integer etapeCourante;

    @ManyToOne
    @JoinColumn(name = "id_createur")
    private Utilisateur createur;

    @Column(name = "lien", length = 500)
    private String lien;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation;

    @Column(name = "date_cloture")
    private LocalDateTime dateCloture;

    @OneToMany(mappedBy = "dossier", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordre ASC")
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<ValidationEtape> etapes = new ArrayList<>();
}
