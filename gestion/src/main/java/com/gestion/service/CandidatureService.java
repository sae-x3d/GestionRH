package com.gestion.service;

import com.gestion.dto.CreateCandidatureDTO;
import com.gestion.dto.LigneParcoursDTO;
import com.gestion.enums.StatutOffre;
import com.gestion.exception.RessourceIntrouvableException;
import com.gestion.model.Candidature;
import com.gestion.model.CandidatureStatus;
import com.gestion.model.Info;
import com.gestion.model.Niveau;
import com.gestion.model.OffreEmploi;
import com.gestion.model.Parcours;
import com.gestion.model.Utilisateur;
import com.gestion.repository.CandidatureRepository;
import com.gestion.repository.NiveauRepository;
import com.gestion.repository.OffreEmploiRepository;
import com.gestion.repository.ParcoursRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CandidatureService {

    private final CandidatureRepository candidatureRepository;
    private final OffreEmploiRepository offreEmploiRepository;
    private final ParcoursRepository parcoursRepository;
    private final NiveauRepository niveauRepository;
    private final CandidatureStatusService candidatureStatusService;
    private final StockageCvService stockageCvService;

    public CandidatureService(CandidatureRepository candidatureRepository,
                              OffreEmploiRepository offreEmploiRepository,
                              ParcoursRepository parcoursRepository,
                              NiveauRepository niveauRepository,
                              CandidatureStatusService candidatureStatusService,
                              StockageCvService stockageCvService) {
        this.candidatureRepository = candidatureRepository;
        this.offreEmploiRepository = offreEmploiRepository;
        this.parcoursRepository = parcoursRepository;
        this.niveauRepository = niveauRepository;
        this.candidatureStatusService = candidatureStatusService;
        this.stockageCvService = stockageCvService;
    }

    @Transactional(readOnly = true)
    public List<OffreEmploi> listerOffresOuvertes() {
        return offreEmploiRepository.findByStatutOrderByDatePublicationDesc(StatutOffre.PUBLIEE);
    }

    @Transactional(readOnly = true)
    public List<Candidature> listerParUtilisateur(Integer idUser) {
        return candidatureRepository.findByUtilisateurIdUserOrderByDateCandidatureDesc(idUser);
    }

    @Transactional(readOnly = true)
    public List<Candidature> listerParOffre(Integer idOffre) {
        return candidatureRepository.findByOffreIdOffreOrderByDateCandidatureDesc(idOffre);
    }

    @Transactional(readOnly = true)
    public List<Parcours> listerParcours() {
        return parcoursRepository.findAllByOrderByTypeAsc();
    }

    @Transactional(readOnly = true)
    public List<Niveau> listerNiveaux() {
        return niveauRepository.findAllByOrderByIdNiveauAsc();
    }

    @Transactional(readOnly = true)
    public boolean aDejaPostule(Integer idUser, Integer idOffre) {
        return candidatureRepository.existsByUtilisateurIdUserAndOffreIdOffre(idUser, idOffre);
    }

    @Transactional
    public Candidature postuler(CreateCandidatureDTO dto, MultipartFile cv, Utilisateur utilisateur) {
        OffreEmploi offre = offreEmploiRepository.findById(dto.getOffreId())
                .orElseThrow(() -> new RessourceIntrouvableException("Offre introuvable : " + dto.getOffreId()));

        verifierOffreOuverte(offre);

        if (candidatureRepository.existsByUtilisateurIdUserAndOffreIdOffre(
                utilisateur.getIdUser(), offre.getIdOffre())) {
            throw new IllegalStateException("Vous avez deja postule a cette offre.");
        }

        CandidatureStatus statutInitial = candidatureStatusService.trouverStatutInitial()
                .orElseThrow(() -> new RessourceIntrouvableException(
                        "Statut de candidature initial introuvable : \""
                                + candidatureStatusService.getLibelleStatutInitial() + "\""));

        Candidature candidature = Candidature.builder()
                .utilisateur(utilisateur)
                .offre(offre)
                .dateCandidature(LocalDateTime.now())
                .statut(statutInitial)
                .build();

        for (LigneParcoursDTO ligne : dto.getParcours()) {
            Parcours parcours = parcoursRepository.findById(ligne.getParcoursId())
                    .orElseThrow(() -> new RessourceIntrouvableException(
                            "Parcours introuvable : " + ligne.getParcoursId()));
            Niveau niveau = niveauRepository.findById(ligne.getNiveauId())
                    .orElseThrow(() -> new RessourceIntrouvableException(
                            "Niveau introuvable : " + ligne.getNiveauId()));

            candidature.getParcours().add(Info.builder()
                    .candidature(candidature)
                    .parcours(parcours)
                    .niveau(niveau)
                    .domaine(texteOuNull(ligne.getDomaine()))
                    .commentaire(texteOuNull(ligne.getCommentaire()))
                    .build());
        }

        Candidature enregistree = candidatureRepository.save(candidature);

        String nomCv = stockageCvService.enregistrer(cv, utilisateur.getUsername(), enregistree.getIdCandidature());
        enregistree.setCvNum(nomCv);
        return candidatureRepository.save(enregistree);
    }

    private void verifierOffreOuverte(OffreEmploi offre) {
        if (offre.getStatut() != StatutOffre.PUBLIEE) {
            throw new IllegalStateException("Cette offre n'est pas ouverte aux candidatures.");
        }
        if (offre.getDateCloture() != null && offre.getDateCloture().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Cette offre est fermee : la date de cloture est depassee.");
        }
    }

    private String texteOuNull(String valeur) {
        if (valeur == null || valeur.isBlank()) {
            return null;
        }
        return valeur.trim();
    }
}
