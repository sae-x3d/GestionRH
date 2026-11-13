package com.gestion.model;

import com.gestion.enums.StatutDiffusion;
import com.gestion.enums.TypeDiffusion;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "diffusion_offre", indexes = {
        @Index(name = "idx_diffusion_offre", columnList = "id_offre")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiffusionOffre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_diffusion")
    private Integer idDiffusion;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_offre", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private OffreEmploi offre;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private TypeDiffusion type;

    @Column(name = "canal", length = 150)
    private String canal;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 30)
    private StatutDiffusion statut;

    @Column(name = "date_diffusion", nullable = false)
    private LocalDateTime dateDiffusion;

    @Column(name = "reference", length = 300)
    private String reference;

    @Column(name = "commentaire", length = 500)
    private String commentaire;
}
