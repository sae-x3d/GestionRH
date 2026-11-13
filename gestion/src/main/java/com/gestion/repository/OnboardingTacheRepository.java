package com.gestion.repository;

import com.gestion.model.OnboardingTache;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OnboardingTacheRepository extends JpaRepository<OnboardingTache, Integer> {

    List<OnboardingTache> findByOnboardingIdOnboardingOrderByOrdreAsc(Integer idOnboarding);

    long countByOnboardingIdOnboardingAndObligatoireTrueAndFaitFalse(Integer idOnboarding);
}
