package com.gestion.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class LigneParcoursDTO {

    @NotNull(message = "Le type de parcours est obligatoire")
    @Positive(message = "L'identifiant du parcours doit etre positif")
    private Integer parcoursId;

    @NotNull(message = "Le niveau est obligatoire")
    @Positive(message = "L'identifiant du niveau doit etre positif")
    private Integer niveauId;

    @Size(max = 255, message = "Le domaine ne peut pas depasser 255 caracteres")
    private String domaine;

    private String commentaire;

    public LigneParcoursDTO() {
    }

    public Integer getParcoursId() {
        return parcoursId;
    }

    public void setParcoursId(Integer parcoursId) {
        this.parcoursId = parcoursId;
    }

    public Integer getNiveauId() {
        return niveauId;
    }

    public void setNiveauId(Integer niveauId) {
        this.niveauId = niveauId;
    }

    public String getDomaine() {
        return domaine;
    }

    public void setDomaine(String domaine) {
        this.domaine = domaine;
    }

    public String getCommentaire() {
        return commentaire;
    }

    public void setCommentaire(String commentaire) {
        this.commentaire = commentaire;
    }
}
