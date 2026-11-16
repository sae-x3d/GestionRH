package com.sirh.backend.modules.discipline.scheduler;

import com.sirh.backend.modules.discipline.service.DisciplineService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DisciplineDeadlineScheduler {
    private static final Logger log = LoggerFactory.getLogger(DisciplineDeadlineScheduler.class);

    private final DisciplineService disciplineService;

    public DisciplineDeadlineScheduler(DisciplineService disciplineService) {
        this.disciplineService = disciplineService;
    }

    @Scheduled(cron = "0 0 8 * * *", zone = "Indian/Antananarivo")
    public void notifyExpiredDeadlines() {
        int notified = disciplineService.notifyExpiredDeadlines();
        if (notified > 0) {
            log.info("Délais d'explication disciplinaires expirés notifiés : {}", notified);
        }
    }

    @Scheduled(cron = "0 0 8 * * *", zone = "Indian/Antananarivo")
    public void closeCompletedProcedures() {
        int closed = disciplineService.closeCompletedProcedures();
        if (closed > 0) {
            log.info("Procédures disciplinaires clôturées automatiquement : {}", closed);
        }
    }
}
