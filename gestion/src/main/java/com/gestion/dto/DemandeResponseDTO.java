package com.gestion.dto;

import java.time.LocalDateTime;

public class DemandeResponseDTO {

    private Integer idDemande;
    private Integer posteId;
    private Integer departementId;
    private Integer motifId;
    private Integer budget;
    private LocalDateTime dateCreation;
    private LocalDateTime dateSouhaite;
    private String statut;

    public DemandeResponseDTO() {
    }

    public DemandeResponseDTO(Integer idDemande, Integer posteId, Integer departementId, Integer motifId, Integer budget, LocalDateTime dateCreation,
    LocalDateTime dateSouhaite, String statut) {
        this.idDemande = idDemande;
        this.posteId = posteId;
        this.departementId = departementId;
        this.motifId = motifId;
        this.budget = budget;
        this.dateCreation = dateCreation;
        this.dateSouhaite = dateSouhaite;
        this.statut = statut;
    }

    public Integer getIdDemande() {
        return idDemande;
    }

    public Integer getPosteId() {
        return posteId;
    }

    public Integer getDepartementId() {
        return departementId;
    }

    public Integer getMotifId() {
        return motifId;
    }

    public Integer getBudget() {
        return budget;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public LocalDateTime getDateSouhaite() {
        return dateSouhaite;
    }

    public String getStatut() {
        return statut;
    }
}