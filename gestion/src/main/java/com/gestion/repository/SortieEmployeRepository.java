package com.gestion.repository;

import com.gestion.model.SortieEmploye;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SortieEmployeRepository extends JpaRepository<SortieEmploye, Integer> {

    List<SortieEmploye> findAllByOrderByDateCreationDesc();

    List<SortieEmploye> findByEmployeIdEmpOrderByDateCreationDesc(Integer idEmp);

    Optional<SortieEmploye> findFirstByEmployeIdEmpOrderByDateCreationDesc(Integer idEmp);
}
