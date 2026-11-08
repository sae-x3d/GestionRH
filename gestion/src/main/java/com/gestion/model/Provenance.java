package com.gestion.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "provenance")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Provenance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_provenance")
    private Integer idProvenance;

    @Column(name = "libelle", length = 100)
    private String libelle;

    @OneToMany(mappedBy = "provenance")
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Employe> employes = new ArrayList<>();
}
