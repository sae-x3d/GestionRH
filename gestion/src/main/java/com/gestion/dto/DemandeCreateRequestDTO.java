package com.gestion.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;

/**
 * Corps de POST /api/demandes.
 *
 * Le client n'envoie que ce qu'il décide : le statut, la date de création
 * et l'identifiant sont fixés par le serveur.
 */
public record DemandeCreateRequestDTO(
        @NotNull(message = "Le poste est obligatoire.")
        @Positive(message = "L'identifiant du poste doit être positif.")
        Integer idPoste,

        @NotNull(message = "Le département est obligatoire.")
        @Positive(message = "L'identifiant du département doit être positif.")
        Integer idDept,

        @NotNull(message = "Le motif est obligatoire.")
        @Positive(message = "L'identifiant du motif doit être positif.")
        Integer idMotif,

        @NotNull(message = "Le budget est obligatoire.")
        @PositiveOrZero(message = "Le budget ne peut pas être négatif.")
        Integer budget,

        @NotNull(message = "La date souhaitée est obligatoire.")
        LocalDateTime dateSouhaite
) 
{
}
