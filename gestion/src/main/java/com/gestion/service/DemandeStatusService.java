package com.gestion.service;

import com.gestion.model.DemandeStatus;
import com.gestion.repository.DemandeStatusRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class DemandeStatusService {

    private final DemandeStatusRepository demandeStatusRepository;
    private final String libelleStatutInitial;

    public DemandeStatusService(DemandeStatusRepository demandeStatusRepository,
                                @Value("${app.demande.statut-initial:En attente}") String libelleStatutInitial) {
        this.demandeStatusRepository = demandeStatusRepository;
        this.libelleStatutInitial = libelleStatutInitial;
    }

    @Transactional(readOnly = true)
    public List<DemandeStatus> listerTous() {
        return demandeStatusRepository.findAllByOrderByLibelleStatusAsc();
    }

    @Transactional(readOnly = true)
    public Optional<DemandeStatus> trouverStatutInitial() {
        return demandeStatusRepository.findByLibelleStatusIgnoreCase(libelleStatutInitial);
    }

    public String getLibelleStatutInitial() {
        return libelleStatutInitial;
    }

    public String classeStatut(String libelleStatus) {
        if (libelleStatus == null) {
            return "status-neutral";
        }
        return switch (libelleStatus.toLowerCase(Locale.ROOT)) {
            case "en attente" -> "status-pending";
            case "approuvée", "approuvee" -> "status-approved";
            case "refusée", "refusee" -> "status-refused";
            case "en cours" -> "status-progress";
            case "terminée", "terminee" -> "status-done";
            default -> "status-neutral";
        };
    }
}
