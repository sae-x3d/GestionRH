package com.gestion.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "motif")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Motif {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_motif")
    private Integer idMotif;

    @Column(name = "libelle", length = 100)
    private String libelle;

    @OneToMany(mappedBy = "motif")
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Demande> demandes = new ArrayList<>();
}
