package com.gestion.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.ArrayList;
import java.util.List;

public class CreateCandidatureDTO {

    @NotNull(message = "L'offre est obligatoire")
    @Positive(message = "L'identifiant de l'offre doit etre positif")
    private Integer offreId;

    @Valid
    @NotEmpty(message = "Au moins un parcours est obligatoire")
    private List<LigneParcoursDTO> parcours = new ArrayList<>();

    public CreateCandidatureDTO() {
    }

    public Integer getOffreId() {
        return offreId;
    }

    public void setOffreId(Integer offreId) {
        this.offreId = offreId;
    }

    public List<LigneParcoursDTO> getParcours() {
        return parcours;
    }

    public void setParcours(List<LigneParcoursDTO> parcours) {
        this.parcours = parcours;
    }
}
