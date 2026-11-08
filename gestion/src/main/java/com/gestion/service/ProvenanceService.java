package com.gestion.service;

import com.gestion.model.Provenance;
import com.gestion.repository.ProvenanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProvenanceService {

    private final ProvenanceRepository provenanceRepository;

    @Transactional(readOnly = true)
    public List<Provenance> listerTous() {
        return provenanceRepository.findAllByOrderByLibelleAsc();
    }
}
