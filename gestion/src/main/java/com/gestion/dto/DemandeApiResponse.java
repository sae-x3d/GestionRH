package com.gestion.dto;

import com.gestion.model.Demande;

import java.time.LocalDateTime;

public record DemandeApiResponse(
        Integer idDemande,
        Integer posteId,
        String poste,
        Integer departementId,
        String departement,
        Integer motifId,
        String motif,
        Integer demandeurId,
        String demandeur,
        Integer empRemplaceId,
        Integer budget,
        LocalDateTime dateSouhaite,
        LocalDateTime dateCreation,
        String statut,
        LocalDateTime dateReponse) {

    public static DemandeApiResponse depuis(Demande demande) {
        return new DemandeApiResponse(
                demande.getIdDemande(),
                demande.getPoste() != null ? demande.getPoste().getIdPoste() : null,
                demande.getPoste() != null ? demande.getPoste().getNomPoste() : null,
                demande.getDepartement() != null ? demande.getDepartement().getIdDept() : null,
                demande.getDepartement() != null ? demande.getDepartement().getNomDept() : null,
                demande.getMotif() != null ? demande.getMotif().getIdMotif() : null,
                demande.getMotif() != null ? demande.getMotif().getLibelle() : null,
                demande.getDemandeur() != null ? demande.getDemandeur().getIdUser() : null,
                demande.getDemandeur() != null ? demande.getDemandeur().getUsername() : null,
                demande.getEmpRemplace() != null ? demande.getEmpRemplace().getIdEmp() : null,
                demande.getBudget(),
                demande.getDateSouhaite(),
                demande.getDateCreation(),
                demande.getDemandeStatus() != null ? demande.getDemandeStatus().getLibelleStatus() : null,
                demande.getDateReponse());
    }
}
