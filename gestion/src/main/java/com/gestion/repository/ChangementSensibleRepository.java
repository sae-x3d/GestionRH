package com.gestion.repository;

import com.gestion.enums.StatutChangementSensible;
import com.gestion.model.ChangementSensible;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChangementSensibleRepository extends JpaRepository<ChangementSensible, Integer> {

    List<ChangementSensible> findByStatutOrderByDateSoumissionAsc(StatutChangementSensible statut);

    List<ChangementSensible> findByEmployeIdEmpOrderByDateSoumissionDesc(Integer idEmp);

    long countByStatut(StatutChangementSensible statut);
}
