package com.sirh.backend.modules.conges.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.sirh.backend.modules.dossier.service.EmployeeDocumentStorageService;
import com.sirh.backend.modules.notifications.service.NotificationCenterService;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class LeaveManagementServiceMiseAPiedTest {
    private static final Integer USER_ID = 12;
    private static final Integer EMPLOYEE_ID = 7;
    private static final Integer TYPE_ID = 2;
    private static final ZoneId HR_ZONE = ZoneId.of("Indian/Antananarivo");

    @Mock private JdbcTemplate jdbc;
    @Mock private NotificationCenterService notifications;
    @Mock private EmployeeDocumentStorageService documentStorage;
    @InjectMocks private LeaveManagementService service;

    @Test
    void refuseUneDemandeQuiChevaucheUneMiseAPied() {
        LocalDate date = LocalDate.now(HR_ZONE).plusWeeks(2)
            .with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
        when(jdbc.query(contains("SELECT id FROM employes WHERE user_id=?"), any(ResultSetExtractor.class), eq(USER_ID)))
            .thenReturn(EMPLOYEE_ID);
        when(jdbc.queryForList(contains("SELECT t.code FROM jours_planning_employe j"), eq(String.class),
            eq(EMPLOYEE_ID), any(LocalDate.class))).thenReturn(List.of("TRAVAIL"));
        when(jdbc.query(contains("FROM types_conge WHERE id=? AND actif=TRUE"), any(ResultSetExtractor.class), eq(TYPE_ID)))
            .thenReturn(Map.of("id", TYPE_ID, "code", "CA", "libelle", "Congé annuel", "acquisition_active", false));
        when(jdbc.queryForObject(contains("SELECT id FROM employes WHERE id=? FOR UPDATE"), eq(Integer.class), eq(EMPLOYEE_ID)))
            .thenReturn(EMPLOYEE_ID);
        when(jdbc.queryForObject(contains("FROM absences a JOIN types_absence ta"), eq(Boolean.class), any(Object[].class)))
            .thenReturn(false);
        when(jdbc.queryForList(contains("JOIN types_sanction ts ON ts.id = sa.type_id AND ts.code = 'MISE_A_PIED'"),
            any(Object[].class))).thenReturn(List.of(Map.of(
                "date_debut", java.sql.Date.valueOf(date),
                "date_fin", java.sql.Date.valueOf(date))));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
            () -> service.submit(USER_ID, TYPE_ID, date, date, null));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertTrue(exception.getReason().contains("chevauche une mise à pied"));
    }
}
