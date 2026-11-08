package com.gestion.service;

import com.gestion.model.CandidatureStatus;
import com.gestion.repository.CandidatureStatusRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CandidatureStatusService {

    private final CandidatureStatusRepository candidatureStatusRepository;
    private final String libelleStatutInitial;

    public CandidatureStatusService(CandidatureStatusRepository candidatureStatusRepository,
                                    @Value("${app.candidature.statut-initial:RECUE}") String libelleStatutInitial) {
        this.candidatureStatusRepository = candidatureStatusRepository;
        this.libelleStatutInitial = libelleStatutInitial;
    }

    @Transactional(readOnly = true)
    public List<CandidatureStatus> listerTous() {
        return candidatureStatusRepository.findAllByOrderByLibelleAsc();
    }

    @Transactional(readOnly = true)
    public Optional<CandidatureStatus> trouverStatutInitial() {
        return candidatureStatusRepository.findByLibelleIgnoreCase(libelleStatutInitial);
    }

    @Transactional(readOnly = true)
    public Optional<CandidatureStatus> trouverParLibelle(String libelle) {
        return candidatureStatusRepository.findByLibelleIgnoreCase(libelle);
    }

    public String getLibelleStatutInitial() {
        return libelleStatutInitial;
    }
}
