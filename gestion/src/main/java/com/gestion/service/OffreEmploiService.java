package com.gestion.service;

import com.gestion.dto.CreateOffreDTO;
import com.gestion.enums.StatutOffre;
import com.gestion.exception.RessourceIntrouvableException;
import com.gestion.model.Demande;
import com.gestion.model.DemandeStatus;
import com.gestion.model.OffreEmploi;
import com.gestion.model.Utilisateur;
import com.gestion.repository.DemandeRepository;
import com.gestion.repository.DemandeStatusRepository;
import com.gestion.repository.OffreEmploiRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OffreEmploiService {

    private final OffreEmploiRepository offreEmploiRepository;
    private final DemandeRepository demandeRepository;
    private final DemandeStatusRepository demandeStatusRepository;
    private final Integer idStatutDemandeValidee;

    public OffreEmploiService(OffreEmploiRepository offreEmploiRepository,
                              DemandeRepository demandeRepository,
                              DemandeStatusRepository demandeStatusRepository,
                              @Value("${app.offre.id-statut-demande-validee:2}") Integer idStatutDemandeValidee) {
        this.offreEmploiRepository = offreEmploiRepository;
        this.demandeRepository = demandeRepository;
        this.demandeStatusRepository = demandeStatusRepository;
        this.idStatutDemandeValidee = idStatutDemandeValidee;
    }

    @Transactional(readOnly = true)
    public List<OffreEmploi> listerPubliees() {
        return offreEmploiRepository.findByStatutOrderByDatePublicationDesc(StatutOffre.PUBLIEE);
    }

    @Transactional(readOnly = true)
    public List<OffreEmploi> listerToutes() {
        return offreEmploiRepository.findAllByOrderByDateCreationDesc();
    }

    @Transactional(readOnly = true)
    public List<Demande> listerDemandesEligibles() {
        return demandeRepository.findSansOffrePourStatut(idStatutDemandeValidee);
    }

    @Transactional(readOnly = true)
    public DemandeStatus trouverStatutDemandeValidee() {
        return demandeStatusRepository.findById(idStatutDemandeValidee)
                .orElseThrow(() -> new RessourceIntrouvableException(
                        "Statut de demande validee introuvable : " + idStatutDemandeValidee));
    }

    @Transactional(readOnly = true)
    public OffreEmploi trouverParId(Integer idOffre) {
        return offreEmploiRepository.findById(idOffre)
                .orElseThrow(() -> new RessourceIntrouvableException("Offre d'emploi introuvable : " + idOffre));
    }

    @Transactional(readOnly = true)
    public boolean estPubliee(OffreEmploi offre) {
        return offre.getStatut() == StatutOffre.PUBLIEE;
    }

    @Transactional(readOnly = true)
    public CreateOffreDTO preparerFormulaire(Integer idDemande) {
        CreateOffreDTO dto = new CreateOffreDTO();
        if (idDemande == null) {
            dto.setTypeContrat("CDI");
            return dto;
        }

        Demande demande = demandeRepository.findById(idDemande)
                .orElseThrow(() -> new RessourceIntrouvableException("Demande introuvable : " + idDemande));
        verifierDemandeEligible(demande);

        String nomPoste = demande.getPoste() == null ? "Poste" : demande.getPoste().getNomPoste();
        String nomDepartement = demande.getDepartement() == null ? "" : demande.getDepartement().getNomDept();

        dto.setDemandeId(demande.getIdDemande());
        dto.setTitre(nomDepartement.isEmpty() ? nomPoste : nomPoste + " - " + nomDepartement);
        dto.setDescription("Nous recherchons un(e) " + nomPoste
                + (nomDepartement.isEmpty() ? "" : " pour le departement " + nomDepartement) + ".");
        dto.setLieu(nomDepartement);
        dto.setTypeContrat("CDI");
        dto.setSalaire(demande.getBudget());
        return dto;
    }

    @Transactional(readOnly = true)
    public CreateOffreDTO versDto(OffreEmploi offre) {
        CreateOffreDTO dto = new CreateOffreDTO();
        dto.setDemandeId(offre.getDemande().getIdDemande());
        dto.setTitre(offre.getTitre());
        dto.setDescription(offre.getDescription());
        dto.setLieu(offre.getLieu());
        dto.setTypeContrat(offre.getTypeContrat());
        dto.setSalaire(offre.getSalaire());
        dto.setDateCloture(offre.getDateCloture());
        return dto;
    }

    @Transactional
    public OffreEmploi creer(CreateOffreDTO dto, Utilisateur auteur) {
        Demande demande = demandeRepository.findById(dto.getDemandeId())
                .orElseThrow(() -> new RessourceIntrouvableException("Demande introuvable : " + dto.getDemandeId()));
        verifierDemandeEligible(demande);

        OffreEmploi offre = OffreEmploi.builder()
                .demande(demande)
                .titre(dto.getTitre().trim())
                .description(dto.getDescription().trim())
                .lieu(texteOuNull(dto.getLieu()))
                .typeContrat(dto.getTypeContrat().trim())
                .salaire(dto.getSalaire())
                .dateCreation(LocalDateTime.now())
                .dateCloture(dto.getDateCloture())
                .statut(StatutOffre.BROUILLON)
                .auteur(auteur)
                .build();

        return offreEmploiRepository.save(offre);
    }

    @Transactional
    public OffreEmploi modifier(Integer idOffre, CreateOffreDTO dto) {
        OffreEmploi offre = trouverParId(idOffre);

        if (offre.getStatut() == StatutOffre.CLOTUREE) {
            throw new IllegalStateException("Une offre cloturee ne peut plus etre modifiee.");
        }
        if (dto.getDemandeId() != null && !dto.getDemandeId().equals(offre.getDemande().getIdDemande())) {
            throw new IllegalStateException("La demande liee a une offre ne peut pas etre modifiee.");
        }

        offre.setTitre(dto.getTitre().trim());
        offre.setDescription(dto.getDescription().trim());
        offre.setLieu(texteOuNull(dto.getLieu()));
        offre.setTypeContrat(dto.getTypeContrat().trim());
        offre.setSalaire(dto.getSalaire());
        offre.setDateCloture(dto.getDateCloture());

        return offreEmploiRepository.save(offre);
    }

    @Transactional
    public OffreEmploi publier(Integer idOffre) {
        OffreEmploi offre = trouverParId(idOffre);

        if (offre.getStatut() == StatutOffre.PUBLIEE) {
            throw new IllegalStateException("Cette offre est deja publiee.");
        }
        if (offre.getStatut() == StatutOffre.CLOTUREE) {
            throw new IllegalStateException("Une offre cloturee ne peut pas etre publiee.");
        }
        if (offre.getDateCloture() != null && offre.getDateCloture().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("La date de cloture est depassee.");
        }

        offre.setStatut(StatutOffre.PUBLIEE);
        offre.setDatePublication(LocalDateTime.now());

        return offreEmploiRepository.save(offre);
    }

    private void verifierDemandeEligible(Demande demande) {
        if (demande.getDemandeStatus() == null
                || !idStatutDemandeValidee.equals(demande.getDemandeStatus().getIdDemandeStatus())) {
            throw new IllegalStateException(
                    "La demande doit etre validee par la direction avant de creer une offre.");
        }
        if (offreEmploiRepository.existsByDemandeIdDemande(demande.getIdDemande())) {
            throw new IllegalStateException("Une offre existe deja pour cette demande.");
        }
    }

    private String texteOuNull(String valeur) {
        if (valeur == null || valeur.isBlank()) {
            return null;
        }
        return valeur.trim();
    }
}
