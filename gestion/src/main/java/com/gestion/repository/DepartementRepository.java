package com.gestion.repository;

import com.gestion.model.Departement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DepartementRepository extends JpaRepository<Departement, Integer> {

    List<Departement> findAllByOrderByNomDeptAsc();
}
