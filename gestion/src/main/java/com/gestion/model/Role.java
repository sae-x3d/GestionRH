package com.gestion.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "role")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_role")
    private Integer idRole;

    @Column(name = "nom_role", length = 100)
    private String nomRole;

    @OneToMany(mappedBy = "role")
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Poste> postes = new ArrayList<>();
}
