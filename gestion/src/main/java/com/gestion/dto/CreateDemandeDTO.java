package com.gestion.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * Donnees saisies pour creer une demande de recrutement.
 * Le demandeur et le statut initial sont determines par le backend
 * (utilisateur authentifie et statut configure), ils ne font donc pas partie du formulaire.
 */
public class CreateDemandeDTO {

    @NotNull(message = "Le poste est obligatoire")
    @Positive(message = "L'identifiant du poste doit etre positif")
    private Integer posteId;

    @NotNull(message = "Le departement est obligatoire")
    @Positive(message = "L'identifiant du departement doit etre positif")
    private Integer departementId;

    @NotNull(message = "Le motif est obligatoire")
    @Positive(message = "L'identifiant du motif doit etre positif")
    private Integer motifId;

    @Positive(message = "L'identifiant de l'employe remplace doit etre positif")
    private Integer empRemplaceId;

    @PositiveOrZero(message = "Le budget ne peut pas etre negatif")
    private Integer budget;

    @FutureOrPresent(message = "La date souhaitee ne peut pas etre dans le passe")
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
