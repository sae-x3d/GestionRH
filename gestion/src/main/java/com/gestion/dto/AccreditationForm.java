package com.gestion.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
public class AccreditationForm {

    private Integer idCandidature;

    private Integer idUtilisateur;

    private String typeAccreditation;

    private String organisme;

    private String numero;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateObtention;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateExpiration;

    private String commentaire;
}
