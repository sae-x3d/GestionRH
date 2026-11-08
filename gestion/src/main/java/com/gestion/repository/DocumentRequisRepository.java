package com.gestion.repository;

import com.gestion.model.DocumentRequis;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DocumentRequisRepository extends JpaRepository<DocumentRequis, Integer> {
    List<DocumentRequis> findByActifTrueOrderByTypeDocumentAsc();
    boolean existsByTypeDocumentIgnoreCase(String typeDocument);
}
