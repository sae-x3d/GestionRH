package com.gestion.repository;

import com.gestion.model.DocumentEmploye;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface DocumentEmployeRepository extends JpaRepository<DocumentEmploye, Integer> {

    List<DocumentEmploye> findByEmployeIdEmpOrderByDateUploadDesc(Integer idEmp);

    List<DocumentEmploye> findAllByOrderByDateUploadDesc();

    List<DocumentEmploye> findByDateValiditeBetweenAndAlerteExpirationEnvoyeeFalse(
            LocalDate debut, LocalDate fin);

    boolean existsByEmployeIdEmpAndTypeDocumentIgnoreCase(Integer idEmp, String typeDocument);
}
