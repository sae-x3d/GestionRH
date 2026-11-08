package com.gestion.repository;

import com.gestion.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Integer> {

    List<Document> findByOnboardingIdOnboardingOrderByDateUploadAsc(Integer idOnboarding);
}
