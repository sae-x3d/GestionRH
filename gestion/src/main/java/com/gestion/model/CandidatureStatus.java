package com.gestion.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "candidature_status")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidatureStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_candidature_status")
    private Integer idCandidatureStatus;

    @Column(name = "libelle", length = 50, nullable = false)
    private String libelle;
}
