package com.gestion.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public class CreateEmployeDTO {

    @NotBlank(message = "Le nom de l'employe est obligatoire")
    @Size(max = 255, message = "Le nom de l'employe ne peut pas depasser 255 caracteres")
    private String nomEmp;

    @NotNull(message = "La date d'embauche est obligatoire")
    @PastOrPresent(message = "La date d'embauche ne peut pas etre dans le futur")
    private LocalDateTime dateEmbauche;

    @FutureOrPresent(message = "La date de depart ne peut pas etre dans le passe")
    private LocalDateTime dateDepart;

    @NotNull(message = "Le poste est obligatoire")
    @Positive(message = "L'identifiant du poste doit etre positif")
    private Integer posteId;

    @NotNull(message = "Le departement est obligatoire")
    @Positive(message = "L'identifiant du departement doit etre positif")
    private Integer departementId;

    @NotNull(message = "Le salaire est obligatoire")
    @PositiveOrZero(message = "Le salaire ne peut pas etre negatif")
    private Integer salaireEmp;

    @NotNull(message = "La provenance est obligatoire")
    @Positive(message = "L'identifiant de la provenance doit etre positif")
    private Integer provenanceId;

    public CreateEmployeDTO() {
    }

    public String getNomEmp() {
        return nomEmp;
    }

    public void setNomEmp(String nomEmp) {
        this.nomEmp = nomEmp;
    }

    public LocalDateTime getDateEmbauche() {
        return dateEmbauche;
    }

    public void setDateEmbauche(LocalDateTime dateEmbauche) {
        this.dateEmbauche = dateEmbauche;
    }

    public LocalDateTime getDateDepart() {
        return dateDepart;
    }

    public void setDateDepart(LocalDateTime dateDepart) {
        this.dateDepart = dateDepart;
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

    public Integer getSalaireEmp() {
        return salaireEmp;
    }

    public void setSalaireEmp(Integer salaireEmp) {
        this.salaireEmp = salaireEmp;
    }

    public Integer getProvenanceId() {
        return provenanceId;
    }

    public void setProvenanceId(Integer provenanceId) {
        this.provenanceId = provenanceId;
    }

    @AssertTrue(message = "La date de depart doit etre posterieure ou egale a la date d'embauche")
    @JsonIgnore
    public boolean isDateDepartCoherente() {
        if (dateDepart == null || dateEmbauche == null) {
            return true;
        }
        return !dateDepart.isBefore(dateEmbauche);
    }
}
