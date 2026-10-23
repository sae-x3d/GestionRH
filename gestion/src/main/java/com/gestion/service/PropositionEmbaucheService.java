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
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PropositionEmbaucheService {

    public static final String STATUT_EN_ATTENTE = "EN_ATTENTE";
    public static final String STATUT_ACCEPTEE = "ACCEPTEE";
    public static final String STATUT_REFUSEE = "REFUSEE";
    public static final String STATUT_EXPIREE = "EXPIREE";
    public static final String STATUT_ATTENTE_VALIDATION_RH_FINAL = "EN_ATTENTE_VALIDATION_RH_FINAL";

    private final PropositionEmbaucheRepository propositionRepository;
    private final CandidatureRepository candidatureRepository;
    private final CandidatureStatusService candidatureStatusService;
    private final EmployeRepository employeRepository;
    private final UtilisateurRepository utilisateurRepository;

    @Value("${app.candidature.validation-rh-finale:false}")
    private boolean validationRhFinaleRequise;

    @Transactional(readOnly = true)
    public Optional<PropositionEmbauche> trouverParCandidature(Integer idCandidature) {
        return propositionRepository.findByCandidatureIdAvecDetails(idCandidature);
    }

    @Transactional(readOnly = true)
    public Optional<PropositionEmbauche> trouverParCandidat(Utilisateur utilisateur) {
        if (utilisateur == null) {
            return Optional.empty();
        }
        return propositionRepository.findByCandidatIdUserAvecDetails(utilisateur.getIdUser());
    }

    @Transactional
    public PropositionEmbauche genererProposition(Integer idCandidature, Utilisateur rh,
                                                 String postePropose, String departementPropose, String lieuTravail,
                                                 Integer salairePropose, String typeContrat,
                                                 Integer periodeEssaiMois, LocalDate dateEmbaucheSouhaitee,
                                                 Integer delaiReponseJours, String avantages, String autresConditions,
                                                 String message) {
        if (propositionRepository.existsByCandidatureIdCandidature(idCandidature)) {
            throw new IllegalStateException("Une proposition d'embauche existe deja pour cette candidature.");
        }

        Candidature candidature = candidatureRepository.findById(idCandidature)
                .orElseThrow(() -> new RessourceIntrouvableException("Candidature introuvable : " + idCandidature));

        if (candidature.getStatut() == null || !"ACCEPTEE".equalsIgnoreCase(candidature.getStatut().getLibelle())) {
            throw new IllegalStateException("La proposition ne peut etre generee que pour une candidature acceptee.");
        }

        LocalDateTime now = LocalDateTime.now();
        PropositionEmbauche proposition = PropositionEmbauche.builder()
                .candidature(candidature)
                .offre(candidature.getOffre())
                .candidat(candidature.getUtilisateur())
                .rh(rh)
                .postePropose(postePropose != null && !postePropose.isBlank() ? postePropose.trim() : (candidature.getOffre() != null && candidature.getOffre().getPoste() != null ? candidature.getOffre().getPoste().getNomPoste() : null))
                .departementPropose(departementPropose != null && !departementPropose.isBlank() ? departementPropose.trim() : (candidature.getOffre() != null && candidature.getOffre().getDepartement() != null ? candidature.getOffre().getDepartement().getNomDept() : null))
                .lieuTravail(lieuTravail != null && !lieuTravail.isBlank() ? lieuTravail.trim() : null)
                .typeContrat(typeContrat != null && !typeContrat.isBlank() ? typeContrat.trim() : (candidature.getOffre() != null ? candidature.getOffre().getTypeContrat() : null))
                .salairePropose(salairePropose)
                .periodeEssaiMois(periodeEssaiMois)
                .dateEmbaucheSouhaitee(dateEmbaucheSouhaitee)
                .avantages(avantages != null && !avantages.isBlank() ? avantages.trim() : null)
                .autresConditions(autresConditions != null && !autresConditions.isBlank() ? autresConditions.trim() : null)
                .dateProposition(now)
                .dateExpiration(delaiReponseJours == null || delaiReponseJours <= 0 ? now.plusDays(7) : now.plusDays(delaiReponseJours))
                .message(message != null && !message.isBlank() ? message.trim() : "Nous avons le plaisir de vous adresser notre proposition d'embauche. Nous vous invitons à prendre connaissance des conditions ci-dessous et à nous faire connaître votre réponse dans le délai imparti.")
                .statut(STATUT_EN_ATTENTE)
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

                Utilisateur utilisateurCandidat = proposition.getCandidat();
                if (utilisateurCandidat != null) {
                    utilisateurCandidat = utilisateurRepository.findById(utilisateurCandidat.getIdUser()).orElse(utilisateurCandidat);
                    if (utilisateurCandidat.getEmploye() == null) {
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
                                .poste(candidature.getOffre() != null ? candidature.getOffre().getPoste() : (proposition.getOffre() != null ? proposition.getOffre().getPoste() : null))
                                .departement(candidature.getOffre() != null ? candidature.getOffre().getDepartement() : (proposition.getOffre() != null ? proposition.getOffre().getDepartement() : null))
                                .salaireEmp(proposition.getSalairePropose())
                                .build();

                        employe = employeRepository.save(employe);
                        utilisateurCandidat.setEmploye(employe);
                        utilisateurRepository.save(utilisateurCandidat);
                    }
                }
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

        PropositionEmbauche proposition = propositionRepository.findByCandidatureIdAvecDetails(idCandidature)
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

            Utilisateur utilisateurCandidat = proposition.getCandidat();
            if (utilisateurCandidat != null) {
                utilisateurCandidat = utilisateurRepository.findById(utilisateurCandidat.getIdUser()).orElse(utilisateurCandidat);
                if (utilisateurCandidat.getEmploye() == null) {
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
                            .poste(candidature.getOffre() != null ? candidature.getOffre().getPoste() : proposition.getOffre().getPoste())
                            .departement(candidature.getOffre() != null ? candidature.getOffre().getDepartement() : proposition.getOffre().getDepartement())
                            .salaireEmp(proposition.getSalairePropose())
                            .build();

                    employe = employeRepository.save(employe);
                    utilisateurCandidat.setEmploye(employe);
                    utilisateurRepository.save(utilisateurCandidat);
                }
            }
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
