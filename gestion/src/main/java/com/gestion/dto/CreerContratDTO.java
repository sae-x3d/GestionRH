package com.gestion.dto;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import lombok.Getter; import lombok.Setter;
@Getter @Setter
public class CreerContratDTO {
    @NotBlank @Size(max=200) private String nomSalarie;
    @NotBlank @Size(max=500) private String adresseSalarie;
    @NotBlank @Size(max=200) private String nomEmployeur;
    @NotBlank @Size(max=500) private String adresseEmployeur;
    @NotBlank private String typeContrat;
    @NotBlank @Size(max=150) private String categorieProfessionnelle;
    @NotBlank @Size(max=100) private String indiceClassification;
    @NotBlank @Size(max=150) private String dureeTravail;
    @NotBlank @Size(max=200) private String lieuTravail;
    @NotNull @FutureOrPresent private LocalDate dateEffet;
    private LocalDate dateFin;
    @Size(max=500) private String motifCdd;
}
