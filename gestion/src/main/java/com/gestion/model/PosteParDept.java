package com.gestion.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "poste_par_dept")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PosteParDept {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "id_dept")
    private Departement departement;

    @ManyToOne
    @JoinColumn(name = "id_poste")
    private Poste poste;

    @Column(name = "nb_poste")
    private Integer nbPoste;
}
