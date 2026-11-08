package com.gestion.repository;

import com.gestion.model.HistoriqueSalaire;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistoriqueSalaireRepository extends JpaRepository<HistoriqueSalaire, Integer> {

    List<HistoriqueSalaire> findByEmployeIdEmpOrderByDateEffetDesc(Integer idEmp);
}
