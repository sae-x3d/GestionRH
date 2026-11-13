package com.gestion.repository;

import com.gestion.model.Affectation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AffectationRepository extends JpaRepository<Affectation, Integer> {

    List<Affectation> findByEmployeIdEmpOrderByDateDebutDesc(Integer idEmp);

    List<Affectation> findByStatutOrderByDateDebutDesc(String statut);
}
