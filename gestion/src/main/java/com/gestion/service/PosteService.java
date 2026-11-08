package com.gestion.service;

import com.gestion.model.Poste;
import com.gestion.repository.PosteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PosteService {

    private final PosteRepository posteRepository;

    @Transactional(readOnly = true)
    public List<Poste> listerTous() {
        return posteRepository.findAllByOrderByNomPosteAsc();
    }
}
