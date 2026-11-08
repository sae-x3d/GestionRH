package com.gestion.model;

import com.gestion.enums.StatutAbsence;
import com.gestion.enums.TypeAbsence;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "absence", indexes = @Index(name = "idx_absence_emp_dates", columnList = "id_emp,date_debut,date_fin"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Absence {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_absence") private Integer idAbsence;
    @ManyToOne(optional = false) @JoinColumn(name = "id_emp", nullable = false)
    private Employe employe;
    @Enumerated(EnumType.STRING) @Column(name = "type_absence", nullable = false, length = 30)
    private TypeAbsence typeAbsence;
    @Column(name = "date_debut", nullable = false) private LocalDate dateDebut;
    @Column(name = "date_fin", nullable = false) private LocalDate dateFin;
    @Column(name = "justificatif_chemin", nullable = false, length = 500) private String justificatifChemin;
    @Column(name = "justificatif_nom", length = 255) private String justificatifNom;
    @Enumerated(EnumType.STRING) @Column(name = "statut", nullable = false, length = 20)
    @Builder.Default private StatutAbsence statut = StatutAbsence.SOUMISE;
    @ManyToOne @JoinColumn(name = "id_valide_par") private Utilisateur validePar;
    @Column(name = "date_validation") private LocalDateTime dateValidation;
    @Column(name = "date_creation", nullable = false) private LocalDateTime dateCreation;
}
