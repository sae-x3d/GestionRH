package com.gestion.model;

import com.gestion.enums.TypeEntretien;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "entretien")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Entretien {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_entretien")
    private Integer idEntretien;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_candidature", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Candidature candidature;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 20, nullable = false)
    private TypeEntretien type;

    @Column(name = "date_heure", nullable = false)
    private LocalDateTime dateHeure;

    @Column(name = "lieu", length = 255)
    private String lieu;

    @Column(name = "participants", length = 255)
    private String participants;

    @Column(name = "resultat", columnDefinition = "TEXT")
    private String resultat;

    @Column(name = "evaluation", length = 255)
    private String evaluation;

    public boolean isRealise() {
        return resultat != null && !resultat.isBlank();
    }
}
