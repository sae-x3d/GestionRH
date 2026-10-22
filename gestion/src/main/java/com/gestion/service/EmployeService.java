package com.gestion.service;

import com.gestion.model.Employe;
import com.gestion.repository.EmployeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeService {

    private final EmployeRepository employeRepository;

    @Transactional(readOnly = true)
    public List<Employe> listerActifs() {
        return employeRepository.findByDateDepartIsNullOrderByNomEmpAsc();
    }
}
