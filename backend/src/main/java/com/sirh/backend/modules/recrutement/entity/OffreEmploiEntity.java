package com.sirh.backend.modules.recrutement.entity;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "offres_emploi")
public class OffreEmploiEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(name = "demande_id", nullable = false, unique = true)
    private Integer demandeId;
    @Column(name = "cree_par_id", nullable = false)
    private Integer creeParId;
    @Column(nullable = false, length = 150)
    private String titre;
    @Column(columnDefinition = "TEXT")
    private String description;
    @Column(columnDefinition = "TEXT")
    private String competences;
    @Column(name = "statut_id", nullable = false)
    private Integer statutId;
    @Column(name = "date_publication")
    private LocalDate datePublication;
    @Column(name = "date_cloture")
    private LocalDate dateCloture;
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected OffreEmploiEntity() { }
    public OffreEmploiEntity(Integer demandeId, Integer creeParId, Integer statutId,
            String titre, String description, String competences, LocalDate dateCloture) {
        this.demandeId = demandeId;
        this.creeParId = creeParId;
        this.statutId = statutId;
        this.titre = titre;
        this.description = description;
        this.competences = competences;
        this.dateCloture = dateCloture;
        this.datePublication = LocalDate.now();
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = this.createdAt;
    }
    public Integer getId() { return id; }
    public String getTitre() { return titre; }
    public Integer getStatutId() { return statutId; }
}
