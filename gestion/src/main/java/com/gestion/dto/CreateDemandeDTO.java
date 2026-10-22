package com.gestion.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * Données saisies pour créer une demande de recrutement.
 * Le demandeur et le statut initial sont déterminés par le backend
 * (utilisateur authentifié et statut configuré), ils ne font donc pas partie du formulaire.
 */
public class CreateDemandeDTO {

    @NotNull(message = "Le poste est obligatoire")
    @Positive(message = "L'identifiant du poste doit être positif")
    private Integer posteId;

    @NotNull(message = "Le département est obligatoire")
    @Positive(message = "L'identifiant du département doit être positif")
    private Integer departementId;

    @NotNull(message = "Le motif est obligatoire")
    @Positive(message = "L'identifiant du motif doit être positif")
    private Integer motifId;

    @Positive(message = "L'identifiant de l'employé remplacé doit être positif")
    private Integer empRemplaceId;

    @PositiveOrZero(message = "Le budget ne peut pas être négatif")
    private Integer budget;

    @FutureOrPresent(message = "La date souhaitée ne peut pas être dans le passé")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime dateSouhaite;

    public CreateDemandeDTO() {
    }

    public Integer getPosteId() {
        return posteId;
    }

    public void setPosteId(Integer posteId) {
        this.posteId = posteId;
    }

    public Integer getDepartementId() {
        return departementId;
    }

    public void setDepartementId(Integer departementId) {
        this.departementId = departementId;
    }

    public Integer getMotifId() {
        return motifId;
    }

    public void setMotifId(Integer motifId) {
        this.motifId = motifId;
    }

    public Integer getEmpRemplaceId() {
        return empRemplaceId;
    }

    public void setEmpRemplaceId(Integer empRemplaceId) {
        this.empRemplaceId = empRemplaceId;
    }

    public Integer getBudget() {
        return budget;
    }

    public void setBudget(Integer budget) {
        this.budget = budget;
    }

    public LocalDateTime getDateSouhaite() {
        return dateSouhaite;
    }

    public void setDateSouhaite(LocalDateTime dateSouhaite) {
        this.dateSouhaite = dateSouhaite;
    }
}