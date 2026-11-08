package com.gestion.event;

import com.gestion.enums.RoleValidation;
import com.gestion.enums.StatutDossierValidation;
import com.gestion.enums.TypeObjetValidation;
import com.gestion.model.Utilisateur;

public record DecisionValidationEvent(
        TypeObjetValidation typeObjet,
        Integer idObjet,
        StatutDossierValidation statut,
        RoleValidation etapeValidee,
        Utilisateur decideur,
        String commentaire,
        String lien
) {
}
