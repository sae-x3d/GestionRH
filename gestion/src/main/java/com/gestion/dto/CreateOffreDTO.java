package com.gestion.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

public class CreateOffreDTO {

    @NotNull(message = "La demande est obligatoire")
    @Positive(message = "L'identifiant de la demande doit etre positif")
    private Integer demandeId;

    @NotBlank(message = "Le titre est obligatoire")
    @Size(max = 150, message = "Le titre ne peut pas depasser 150 caracteres")
    private String titre;

    @NotBlank(message = "La description est obligatoire")
    private String description;

    @Size(max = 150, message = "Le lieu ne peut pas depasser 150 caracteres")
    private String lieu;

    @NotBlank(message = "Le type de contrat est obligatoire")
    @Size(max = 50, message = "Le type de contrat ne peut pas depasser 50 caracteres")
    private String typeContrat;

    @PositiveOrZero(message = "Le salaire ne peut pas etre negatif")
    private Integer salaire;

    @Future(message = "La date de cloture doit etre dans le futur")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime dateCloture;

    public CreateOffreDTO() {
    }

    public Integer getDemandeId() {
        return demandeId;
    }

    public void setDemandeId(Integer demandeId) {
        this.demandeId = demandeId;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLieu() {
        return lieu;
    }

    public void setLieu(String lieu) {
        this.lieu = lieu;
    }

    public String getTypeContrat() {
        return typeContrat;
    }

    public void setTypeContrat(String typeContrat) {
        this.typeContrat = typeContrat;
    }

    public Integer getSalaire() {
        return salaire;
    }

    public void setSalaire(Integer salaire) {
        this.salaire = salaire;
    }

    public LocalDateTime getDateCloture() {
        return dateCloture;
    }

    public void setDateCloture(LocalDateTime dateCloture) {
        this.dateCloture = dateCloture;
    }
}
