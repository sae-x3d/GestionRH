package com.gestion.repository;

import com.gestion.model.Employe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmployeRepository extends JpaRepository<Employe, Integer> {

    List<Employe> findByDateDepartIsNullOrderByNomEmpAsc();
}
