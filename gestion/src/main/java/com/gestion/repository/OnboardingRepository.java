package com.gestion.repository;

import com.gestion.model.Onboarding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OnboardingRepository extends JpaRepository<Onboarding, Integer> {

    Optional<Onboarding> findByContratIdContrat(Integer idContrat);

    List<Onboarding> findByEmployeIdEmpOrderByDateDebutDesc(Integer idEmp);

    List<Onboarding> findByStatutOrderByDateDebutDesc(String statut);

    List<Onboarding> findByStatutInOrderByDateDebutDesc(List<String> statuts);
}
