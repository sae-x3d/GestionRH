package com.gestion.service;

import com.gestion.model.Motif;
import com.gestion.repository.MotifRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MotifService {

    private final MotifRepository motifRepository;

    @Transactional(readOnly = true)
    public List<Motif> listerTous() {
        return motifRepository.findAllByOrderByLibelleAsc();
    }
}
