package com.sirh.backend.modules.temps.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sirh.backend.modules.notifications.service.NotificationCenterService;
import com.sirh.backend.modules.temps.dto.PunchActionRequest;
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
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class TimeManagementServiceMiseAPiedTest {
    private static final Integer USER_ID = 12;
    private static final Integer EMPLOYEE_ID = 7;
    private static final Integer ACTOR_ID = 1;
    private static final Integer MISE_A_PIED_TYPE_ID = 8;
    private static final Integer TRAVAIL_TYPE_ID = 1;
    private static final ZoneId HR_ZONE = ZoneId.of("Indian/Antananarivo");

    @Mock private JdbcTemplate jdbc;
    @Mock private NotificationCenterService notifications;
    @InjectMocks private TimeManagementService service;

    @Test
    void pointageRefuseUneJourneeDeMiseAPied() {
        when(jdbc.queryForList(contains("FROM employes e JOIN users u"), eq(Integer.class), eq(USER_ID)))
            .thenReturn(List.of(EMPLOYEE_ID));
        when(jdbc.queryForList(contains("FROM evenements_pointage ep JOIN sessions_pointage sp"), anyString(), anyInt()))
            .thenReturn(List.of());
        when(jdbc.queryForObject(contains("ts.code = 'MISE_A_PIED'"), eq(Boolean.class), any(), any(), any()))
            .thenReturn(true);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
            () -> service.recordPunch(USER_ID, new PunchActionRequest("ARRIVEE", "1234567890abcdef")));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("Pointage impossible : une mise à pied est en cours pour cette journée.", exception.getReason());
        verify(jdbc, never()).update(contains("INSERT INTO evenements_pointage"), any(Object[].class));
    }

    @Test
    void appliqueLaMiseAPiedSurLesJourneesTravaillees() {
        LocalDate date = LocalDate.now(HR_ZONE).plusWeeks(2)
            .with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
        when(jdbc.queryForList(contains("SELECT p.departement_id FROM employes e JOIN postes p"), eq(Integer.class), eq(EMPLOYEE_ID)))
            .thenReturn(List.of(3));
        when(jdbc.queryForList(contains("SELECT id FROM types_jour_planning WHERE code = ?"), eq(Integer.class), any()))
            .thenAnswer(invocation -> "MISE_A_PIED".equals(invocation.getArgument(2))
                ? List.of(MISE_A_PIED_TYPE_ID) : List.of(TRAVAIL_TYPE_ID));
        when(jdbc.queryForList(contains("SELECT t.code FROM jours_planning_employe j"), eq(String.class), eq(EMPLOYEE_ID), eq(date)))
            .thenReturn(List.of("TRAVAIL"));
        when(jdbc.queryForObject(contains("SELECT id FROM plannings_equipe WHERE departement_id = ?"), eq(Integer.class), any(), any()))
            .thenReturn(9);
        when(jdbc.queryForList(contains("SELECT j.id, t.code AS type_code"), eq(EMPLOYEE_ID), eq(date)))
            .thenReturn(List.of(Map.of("id", 100, "type_code", "TRAVAIL")));

        service.applyMiseAPiedPlanning(EMPLOYEE_ID, date, date, ACTOR_ID, "Mise à pied test");

        verify(jdbc).update(contains("INSERT INTO historiques_jour_planning"), any(Object[].class));
        verify(jdbc).update(contains("UPDATE jours_planning_employe SET type_jour_id = ?"), eq(MISE_A_PIED_TYPE_ID),
            any(), eq(ACTOR_ID), eq(100), eq(TRAVAIL_TYPE_ID));
    }
}
