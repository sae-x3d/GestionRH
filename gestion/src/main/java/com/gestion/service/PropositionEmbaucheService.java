package com.gestion.service;

import com.gestion.exception.RessourceIntrouvableException;
import com.gestion.model.*;
import com.gestion.repository.CandidatureRepository;
import com.gestion.repository.EmployeRepository;
import com.gestion.repository.PropositionEmbaucheRepository;
import com.gestion.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PropositionEmbaucheService {

    public static final String STATUT_EN_ATTENTE = "EN_ATTENTE";
    public static final String STATUT_ACCEPTEE = "ACCEPTEE";
    public static final String STATUT_REFUSEE = "REFUSEE";
    public static final String STATUT_EXPIREE = "EXPIREE";
    public static final String STATUT_ATTENTE_VALIDATION_RH_FINAL = "EN_ATTENTE_VALIDATION_RH_FINAL";

    private static final String MESSAGE_PAR_DEFAUT = "Nous avons le plaisir de vous adresser notre proposition d'embauche. "
            + "Nous vous invitons à prendre connaissance des conditions ci-dessous et à nous faire connaître votre réponse dans le délai imparti.";

    private final PropositionEmbaucheRepository propositionRepository;
    private final CandidatureRepository candidatureRepository;
    private final CandidatureStatusService candidatureStatusService;
    private final EmployeRepository employeRepository;
    private final UtilisateurRepository utilisateurRepository;

    @Value("${app.candidature.validation-rh-finale:false}")
    private boolean validationRhFinaleRequise;

    /**
     * Derniere proposition d'embauche d'une candidature (la plus recente).
     */
    @Transactional(readOnly = true)
    public Optional<PropositionEmbauche> trouverParCandidature(Integer idCandidature) {
        return listerParCandidature(idCandidature).stream().findFirst();
    }

    /**
     * Historique complet des propositions d'une candidature, de la plus recente a la plus ancienne.
     */
    @Transactional(readOnly = true)
    public List<PropositionEmbauche> listerParCandidature(Integer idCandidature) {
        return propositionRepository.findByCandidatureIdAvecDetails(idCandidature);
    }

    @Transactional(readOnly = true)
    public Optional<PropositionEmbauche> trouverParCandidat(Utilisateur utilisateur) {
        if (utilisateur == null) {
            return Optional.empty();
        }
        return propositionRepository.findByCandidatIdUserAvecDetails(utilisateur.getIdUser())
                .stream()
                .findFirst();
    }

    /**
     * Indique si une nouvelle proposition peut etre soumise au candidat apres negociation.
     * Une proposition en attente de reponse ne peut pas etre revisee, sauf si elle a expire.
     */
    @Transactional(readOnly = true)
    public boolean peutEtreRevisee(PropositionEmbauche proposition) {
        if (proposition == null || proposition.getStatut() == null || proposition.getCandidature() == null
                || proposition.getCandidature().getStatut() == null) {
            return false;
        }

        String statutCandidature = proposition.getCandidature().getStatut().getLibelle();
        if (!"ACCEPTEE".equalsIgnoreCase(statutCandidature)
                && !"REFUSEE".equalsIgnoreCase(statutCandidature)) {
            return false;
        }

        if (STATUT_REFUSEE.equalsIgnoreCase(proposition.getStatut())
                || STATUT_EXPIREE.equalsIgnoreCase(proposition.getStatut())) {
            return true;
        }
        return STATUT_EN_ATTENTE.equalsIgnoreCase(proposition.getStatut())
                && proposition.getDateExpiration() != null
                && proposition.getDateExpiration().isBefore(LocalDateTime.now());
    }

    @Transactional
    public PropositionEmbauche genererProposition(Integer idCandidature, Utilisateur rh,
                                                 String postePropose, String departementPropose, String lieuTravail,
                                                 Integer salairePropose, String typeContrat,
                                                 Integer periodeEssaiMois, LocalDate dateEmbaucheSouhaitee,
                                                 Integer delaiReponseJours, String avantages, String autresConditions,
                                                 String message, String motifRevision) {
        Candidature candidature = candidatureRepository.findById(idCandidature)
                .orElseThrow(() -> new RessourceIntrouvableException("Candidature introuvable : " + idCandidature));

        List<PropositionEmbauche> existantes = propositionRepository.findByCandidatureIdAvecDetails(idCandidature);
        PropositionEmbauche precedente = existantes.isEmpty() ? null : existantes.get(0);
        LocalDateTime now = LocalDateTime.now();

        if (precedente == null) {
            verifierCandidatureAcceptee(candidature);
        } else {
            verifierRevisionPossible(candidature, precedente, now);
        }

        Integer numeroRevision = precedente == null || precedente.getNumeroRevision() == null
                ? 1
                : precedente.getNumeroRevision() + 1;

        PropositionEmbauche proposition = PropositionEmbauche.builder()
                .candidature(candidature)
                .offre(candidature.getOffre())
                .candidat(candidature.getUtilisateur())
                .rh(rh)
                .postePropose(posteAvecRepli(postePropose, precedente, candidature))
                .departementPropose(departementAvecRepli(departementPropose, precedente, candidature))
                .lieuTravail(lieuAvecRepli(lieuTravail, precedente))
                .typeContrat(typeContratAvecRepli(typeContrat, precedente, candidature))
                .salairePropose(salairePropose != null ? salairePropose
                        : (precedente != null ? precedente.getSalairePropose() : null))
                .periodeEssaiMois(periodeEssaiMois != null ? periodeEssaiMois
                        : (precedente != null ? precedente.getPeriodeEssaiMois() : null))
                .dateEmbaucheSouhaitee(dateEmbaucheSouhaitee != null ? dateEmbaucheSouhaitee
                        : (precedente != null ? precedente.getDateEmbaucheSouhaitee() : null))
                .avantages(texteOuNull(avantages))
                .autresConditions(texteOuNull(autresConditions))
                .dateProposition(now)
                .dateExpiration(delaiReponseJours == null || delaiReponseJours <= 0
                        ? now.plusDays(7)
                        : now.plusDays(delaiReponseJours))
                .message(message != null && !message.isBlank() ? message.trim() : MESSAGE_PAR_DEFAUT)
                .statut(STATUT_EN_ATTENTE)
                .numeroRevision(numeroRevision)
                .motifRevision(texteOuNull(motifRevision))
                .build();

        return propositionRepository.save(proposition);
    }

    @Transactional
    public PropositionEmbauche accepterProposition(Integer idProposition, Utilisateur candidat) {
        PropositionEmbauche proposition = propositionRepository.findById(idProposition)
                .orElseThrow(() -> new RessourceIntrouvableException("Proposition introuvable : " + idProposition));

        verifierPropositionPourCandidat(proposition, candidat);
        verifierPropositionValide(proposition);

        LocalDateTime now = LocalDateTime.now();
        proposition.setStatut(STATUT_ACCEPTEE);
        proposition.setDateReponse(now);

        Candidature candidature = proposition.getCandidature();
        if (candidature != null) {
            if (validationRhFinaleRequise) {
                candidatureStatusService.trouverParLibelle(STATUT_ATTENTE_VALIDATION_RH_FINAL).ifPresent(statut -> {
                    candidature.setStatut(statut);
                    if (candidature.getDateDecision() == null) {
                        candidature.setDateDecision(now);
                    }
                    candidatureRepository.save(candidature);
                });
            } else {
                if (candidature.getStatut() == null || !"ACCEPTEE".equalsIgnoreCase(candidature.getStatut().getLibelle())) {
                    candidatureStatusService.trouverParLibelle("ACCEPTEE").ifPresent(statutAccepte -> {
                        candidature.setStatut(statutAccepte);
                        if (candidature.getDateDecision() == null) {
                            candidature.setDateDecision(now);
                        }
                        candidatureRepository.save(candidature);
                    });
                } else if (candidature.getDateDecision() == null) {
                    candidature.setDateDecision(now);
                    candidatureRepository.save(candidature);
                }

                creerDossierEmployeSiNecessaire(proposition, candidature, now);
            }
        }

        return propositionRepository.save(proposition);
    }

    @Transactional
    public PropositionEmbauche validerEmbaucheApresAcceptationCandidat(Integer idCandidature, Utilisateur rh) {
        return deciderEmbaucheApresAcceptationCandidat(idCandidature, rh, true, null);
    }

    @Transactional
    public PropositionEmbauche refuserEmbaucheApresAcceptationCandidat(Integer idCandidature, Utilisateur rh, String motifRefus) {
        return deciderEmbaucheApresAcceptationCandidat(idCandidature, rh, false, motifRefus);
    }

    @Transactional
    public PropositionEmbauche deciderEmbaucheApresAcceptationCandidat(Integer idCandidature, Utilisateur rh, boolean accepter) {
        return deciderEmbaucheApresAcceptationCandidat(idCandidature, rh, accepter, null);
    }

    @Transactional
    public PropositionEmbauche deciderEmbaucheApresAcceptationCandidat(Integer idCandidature, Utilisateur rh, boolean accepter, String motifRefus) {
        if (!validationRhFinaleRequise) {
            throw new IllegalStateException("La validation RH finale n'est pas activee par la configuration.");
        }

        Candidature candidature = candidatureRepository.findById(idCandidature)
                .orElseThrow(() -> new RessourceIntrouvableException("Candidature introuvable : " + idCandidature));

        if (candidature.getStatut() == null || !STATUT_ATTENTE_VALIDATION_RH_FINAL.equalsIgnoreCase(candidature.getStatut().getLibelle())) {
            throw new IllegalStateException("Cette candidature n'est pas en attente de validation RH finale.");
        }

        PropositionEmbauche proposition = trouverParCandidature(idCandidature)
                .orElseThrow(() -> new RessourceIntrouvableException("Aucune proposition trouvee pour cette candidature."));

        LocalDateTime now = LocalDateTime.now();

        if (accepter) {
            candidatureStatusService.trouverParLibelle("ACCEPTEE").ifPresent(statutAccepte -> {
                candidature.setStatut(statutAccepte);
                if (candidature.getDateDecision() == null) {
                    candidature.setDateDecision(now);
                }
                candidatureRepository.save(candidature);
            });

            creerDossierEmployeSiNecessaire(proposition, candidature, now);

            if (proposition.getDateReponse() == null) {
                proposition.setDateReponse(now);
            }
            if (proposition.getStatut() == null || !STATUT_ACCEPTEE.equalsIgnoreCase(proposition.getStatut())) {
                proposition.setStatut(STATUT_ACCEPTEE);
            }
            proposition.setMotifRefus(null);
            return propositionRepository.save(proposition);
        } else {
            candidatureStatusService.trouverParLibelle("REFUSEE").ifPresent(statutRefuse -> {
                candidature.setStatut(statutRefuse);
                if (candidature.getDateDecision() == null) {
                    candidature.setDateDecision(now);
                }
                candidatureRepository.save(candidature);
            });
            if (proposition.getDateReponse() == null) {
                proposition.setDateReponse(now);
            }
            proposition.setStatut(STATUT_REFUSEE);
            if (motifRefus != null && !motifRefus.isBlank()) {
                proposition.setMotifRefus(motifRefus.trim());
            } else if (proposition.getMotifRefus() == null || proposition.getMotifRefus().isBlank()) {
                proposition.setMotifRefus("Refuse par la RH lors de la validation finale.");
            }
            return propositionRepository.save(proposition);
        }
    }

    @Transactional
    public PropositionEmbauche refuserProposition(Integer idProposition, Utilisateur candidat, String motifRefus) {
        PropositionEmbauche proposition = propositionRepository.findById(idProposition)
                .orElseThrow(() -> new RessourceIntrouvableException("Proposition introuvable : " + idProposition));

        verifierPropositionPourCandidat(proposition, candidat);
        verifierPropositionValide(proposition);

        LocalDateTime now = LocalDateTime.now();
        proposition.setStatut(STATUT_REFUSEE);
        proposition.setDateReponse(now);
        proposition.setMotifRefus(motifRefus != null && !motifRefus.isBlank() ? motifRefus.trim() : null);

        Candidature candidature = proposition.getCandidature();
        if (candidature != null) {
            if (candidature.getStatut() == null || !"REFUSEE".equalsIgnoreCase(candidature.getStatut().getLibelle())) {
                candidatureStatusService.trouverParLibelle("REFUSEE").ifPresent(statutRefuse -> {
                    candidature.setStatut(statutRefuse);
                    if (candidature.getDateDecision() == null) {
                        candidature.setDateDecision(now);
                    }
                    candidatureRepository.save(candidature);
                });
            } else if (candidature.getDateDecision() == null) {
                candidature.setDateDecision(now);
                candidatureRepository.save(candidature);
            }
        }

        return propositionRepository.save(proposition);
    }

    private void verifierCandidatureAcceptee(Candidature candidature) {
        if (candidature.getStatut() == null
                || !"ACCEPTEE".equalsIgnoreCase(candidature.getStatut().getLibelle())) {
            throw new IllegalStateException("La proposition ne peut etre generee que pour une candidature acceptee.");
        }
    }

    /**
     * Une nouvelle proposition ne peut remplacer que :
     * - une proposition refusee par le candidat (negociation),
     * - une proposition expiree,
     * - une proposition encore en attente mais dont le delai de reponse est depasse.
     */
    private void verifierRevisionPossible(Candidature candidature, PropositionEmbauche precedente, LocalDateTime now) {
        if (precedente.getStatut() == null) {
            throw new IllegalStateException("Le statut de la proposition precedente est introuvable.");
        }

        if (STATUT_ACCEPTEE.equalsIgnoreCase(precedente.getStatut())) {
            throw new IllegalStateException(
                    "La proposition a deja ete acceptee par le candidat : elle ne peut plus etre revisee.");
        }

        if (STATUT_EN_ATTENTE.equalsIgnoreCase(precedente.getStatut())) {
            if (precedente.getDateExpiration() == null || !precedente.getDateExpiration().isBefore(now)) {
                throw new IllegalStateException(
                        "Une proposition est deja en attente de reponse. Le candidat doit d'abord repondre avant toute revision.");
            }
            precedente.setStatut(STATUT_EXPIREE);
            propositionRepository.save(precedente);
        } else if (!STATUT_REFUSEE.equalsIgnoreCase(precedente.getStatut())
                && !STATUT_EXPIREE.equalsIgnoreCase(precedente.getStatut())) {
            throw new IllegalStateException("La proposition ne peut pas etre revisee dans son statut actuel.");
        }

        String statutCandidature = candidature.getStatut() == null ? "" : candidature.getStatut().getLibelle();

        if ("REFUSEE".equalsIgnoreCase(statutCandidature)) {
            // Le candidat a refuse la proposition : on reouvre le dossier pour la negociation.
            candidatureStatusService.trouverParLibelle("ACCEPTEE").ifPresent(statutAccepte -> {
                candidature.setStatut(statutAccepte);
                candidature.setDateDecision(null);
                candidatureRepository.save(candidature);
            });
        } else if (!"ACCEPTEE".equalsIgnoreCase(statutCandidature)) {
            throw new IllegalStateException(
                    "La candidature doit etre acceptee (ou refusee apres negociation) pour reviser la proposition.");
        }
    }

    private void creerDossierEmployeSiNecessaire(PropositionEmbauche proposition, Candidature candidature,
                                                 LocalDateTime now) {
        Utilisateur utilisateurCandidat = proposition.getCandidat();
        if (utilisateurCandidat == null) {
            return;
        }

        utilisateurCandidat = utilisateurRepository.findById(utilisateurCandidat.getIdUser())
                .orElse(utilisateurCandidat);
        if (utilisateurCandidat.getEmploye() != null) {
            return;
        }

        String nomEmp = utilisateurCandidat.getUsername();
        if (candidature.getUtilisateur() != null && candidature.getUtilisateur().getEmploye() != null) {
            nomEmp = candidature.getUtilisateur().getEmploye().getNomEmp();
        }

        LocalDateTime dateEmbauche = now;
        if (proposition.getDateEmbaucheSouhaitee() != null) {
            dateEmbauche = proposition.getDateEmbaucheSouhaitee().atStartOfDay();
        }

        Employe employe = Employe.builder()
                .nomEmp(nomEmp)
                .dateEmbauche(dateEmbauche)
                .poste(posteOffre(candidature.getOffre()) != null ? posteOffre(candidature.getOffre())
                        : posteOffre(proposition.getOffre()))
                .departement(departementOffre(candidature.getOffre()) != null
                        ? departementOffre(candidature.getOffre())
                        : departementOffre(proposition.getOffre()))
                .salaireEmp(proposition.getSalairePropose())
                .build();

        employe = employeRepository.save(employe);
        utilisateurCandidat.setEmploye(employe);
        utilisateurRepository.save(utilisateurCandidat);
    }

    private String posteAvecRepli(String postePropose, PropositionEmbauche precedente, Candidature candidature) {
        String poste = texteOuNull(postePropose);
        if (poste == null && precedente != null) {
            poste = precedente.getPostePropose();
        }
        if (poste == null && posteOffre(candidature.getOffre()) != null) {
            poste = posteOffre(candidature.getOffre()).getNomPoste();
        }
        return poste;
    }

    private String departementAvecRepli(String departementPropose, PropositionEmbauche precedente,
                                        Candidature candidature) {
        String departement = texteOuNull(departementPropose);
        if (departement == null && precedente != null) {
            departement = precedente.getDepartementPropose();
        }
        if (departement == null && departementOffre(candidature.getOffre()) != null) {
            departement = departementOffre(candidature.getOffre()).getNomDept();
        }
        return departement;
    }

    private String lieuAvecRepli(String lieuTravail, PropositionEmbauche precedente) {
        String lieu = texteOuNull(lieuTravail);
        if (lieu == null && precedente != null) {
            lieu = precedente.getLieuTravail();
        }
        return lieu;
    }

    private String typeContratAvecRepli(String typeContrat, PropositionEmbauche precedente, Candidature candidature) {
        String type = texteOuNull(typeContrat);
        if (type == null && precedente != null) {
            type = precedente.getTypeContrat();
        }
        if (type == null && candidature.getOffre() != null) {
            type = candidature.getOffre().getTypeContrat();
        }
        return type;
    }

    private Poste posteOffre(OffreEmploi offre) {
        return offre != null && offre.getDemande() != null ? offre.getDemande().getPoste() : null;
    }

    private Departement departementOffre(OffreEmploi offre) {
        return offre != null && offre.getDemande() != null ? offre.getDemande().getDepartement() : null;
    }

    private String texteOuNull(String valeur) {
        if (valeur == null || valeur.isBlank()) {
            return null;
        }
        return valeur.trim();
    }

    private void verifierPropositionPourCandidat(PropositionEmbauche proposition, Utilisateur candidat) {
        if (proposition.getCandidat() == null || candidat == null
                || !proposition.getCandidat().getIdUser().equals(candidat.getIdUser())) {
            throw new org.springframework.security.access.AccessDeniedException("Cette proposition ne vous appartient pas.");
        }
    }

    private void verifierPropositionValide(PropositionEmbauche proposition) {
        if (!STATUT_EN_ATTENTE.equalsIgnoreCase(proposition.getStatut())) {
            throw new IllegalStateException("Cette proposition a deja ete traitee.");
        }
        if (proposition.getDateExpiration() != null && proposition.getDateExpiration().isBefore(LocalDateTime.now())) {
            proposition.setStatut(STATUT_EXPIREE);
            propositionRepository.save(proposition);
            throw new IllegalStateException("Cette proposition a expire.");
        }
    }
}
