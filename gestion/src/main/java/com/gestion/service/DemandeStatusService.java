package com.gestion.service;

import com.gestion.model.DemandeStatus;
import com.gestion.repository.DemandeStatusRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class DemandeStatusService {

    public static final String EN_ATTENTE_VALIDATION_RH = "EN_ATTENTE_VALIDATION_RH";
    public static final String EN_ATTENTE_VALIDATION_BUDGET = "EN_ATTENTE_VALIDATION_BUDGET";
    public static final String APPROUVE = "APPROUVE";
    public static final String REFUSE = "REFUSE";

    private static final List<String> ORDRE_STATUTS = List.of(
            EN_ATTENTE_VALIDATION_RH,
            EN_ATTENTE_VALIDATION_BUDGET,
            APPROUVE,
            REFUSE);

    private final DemandeStatusRepository demandeStatusRepository;
    private final String libelleStatutInitial;

    public DemandeStatusService(DemandeStatusRepository demandeStatusRepository,
                                @Value("${app.demande.statut-initial:EN_ATTENTE_VALIDATION_RH}") String libelleStatutInitial) {
        this.demandeStatusRepository = demandeStatusRepository;
        this.libelleStatutInitial = libelleStatutInitial;
    }

    @Transactional(readOnly = true)
    public List<DemandeStatus> listerTous() {
        return demandeStatusRepository.findAllByOrderByLibelleStatusAsc().stream()
                .sorted(Comparator.comparingInt(statut -> {
                    int position = ORDRE_STATUTS.indexOf(statut.getLibelleStatus());
                    return position < 0 ? ORDRE_STATUTS.size() : position;
                }))
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<DemandeStatus> trouverStatutInitial() {
        return demandeStatusRepository.findByLibelleStatusIgnoreCase(libelleStatutInitial);
    }

    @Transactional(readOnly = true)
    public Optional<DemandeStatus> trouverParLibelle(String libelleStatus) {
        return demandeStatusRepository.findByLibelleStatusIgnoreCase(libelleStatus);
    }

    public String getLibelleStatutInitial() {
        return libelleStatutInitial;
    }

    public String libelleAffichage(String libelleStatus) {
        if (libelleStatus == null) {
            return "";
        }
        return switch (libelleStatus.toUpperCase(Locale.ROOT)) {
            case EN_ATTENTE_VALIDATION_RH -> "En attente validation RH";
            case EN_ATTENTE_VALIDATION_BUDGET -> "En attente validation budget";
            case APPROUVE -> "Approuvée";
            case REFUSE -> "Refusée";
            default -> libelleStatus;
        };
    }

    public String classeStatut(String libelleStatus) {
        if (libelleStatus == null) {
            return "status-neutral";
        }
        return switch (libelleStatus.toLowerCase(Locale.ROOT)) {
            case "en_attente_validation_rh", "en attente" -> "status-pending";
            case "en_attente_validation_budget", "en cours" -> "status-progress";
            case "approuve", "approuvée", "approuvee" -> "status-approved";
            case "refuse", "refusée", "refusee" -> "status-refused";
            case "terminée", "terminee" -> "status-done";
            default -> "status-neutral";
        };
    }
}
