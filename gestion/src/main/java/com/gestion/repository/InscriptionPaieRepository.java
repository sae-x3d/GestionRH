package com.gestion.repository;

import com.gestion.model.InscriptionPaie;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InscriptionPaieRepository extends JpaRepository<InscriptionPaie, Integer> {

    Optional<InscriptionPaie> findByEmployeIdEmp(Integer idEmp);

    List<InscriptionPaie> findAllByOrderByDateInscriptionDesc();
}
