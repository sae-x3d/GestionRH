package com.gestion.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "demande_status")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DemandeStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_demande_status")
    private Integer idDemandeStatus;

    @Column(name = "libelle_status", length = 50)
    private String libelleStatus;

    @OneToMany(mappedBy = "demandeStatus")
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Demande> demandes = new ArrayList<>();
}
