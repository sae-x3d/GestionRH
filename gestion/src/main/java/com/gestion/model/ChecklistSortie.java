package com.gestion.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "checklist_sortie")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChecklistSortie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_checklist")
    private Integer idChecklist;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_sortie", nullable = false)
    private SortieEmploye sortie;

    @Column(name = "libelle", nullable = false, length = 200)
    private String libelle;

    @Column(name = "responsable", length = 40)
    private String responsable;

    @Column(name = "fait", nullable = false)
    @Builder.Default
    private boolean fait = false;

    @Column(name = "date_fait")
    private LocalDateTime dateFait;

    @Column(name = "commentaire", length = 500)
    private String commentaire;
}
