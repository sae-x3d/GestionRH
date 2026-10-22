package com.gestion.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "parcours")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Parcours {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_parcours")
    private Integer idParcours;

    @Column(name = "type", length = 50, nullable = false)
    private String type;
}
