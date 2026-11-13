package com.gestion.repository;

import com.gestion.model.HistoriqueCarriere;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistoriqueCarriereRepository extends JpaRepository<HistoriqueCarriere, Integer> {

    List<HistoriqueCarriere> findByEmployeIdEmpOrderByDateEffetDesc(Integer idEmp);
}
