package com.gestion.repository;

import com.gestion.model.JournalAudit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface JournalAuditRepository extends JpaRepository<JournalAudit, Integer> {

    List<JournalAudit> findAllByOrderByDateActionDesc();

    List<JournalAudit> findByUsernameOrderByDateActionDesc(String username);

    List<JournalAudit> findByDateActionBetweenOrderByDateActionDesc(LocalDateTime debut, LocalDateTime fin);
}
