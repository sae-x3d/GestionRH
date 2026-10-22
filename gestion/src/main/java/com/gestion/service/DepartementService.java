package com.gestion.service;

import com.gestion.model.Departement;
import com.gestion.repository.DepartementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DepartementService {

    private final DepartementRepository departementRepository;

    @Transactional(readOnly = true)
    public List<Departement> listerTous() {
        return departementRepository.findAllByOrderByNomDeptAsc();
    }
}
