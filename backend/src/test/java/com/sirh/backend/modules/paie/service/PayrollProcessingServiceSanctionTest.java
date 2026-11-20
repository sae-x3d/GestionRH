package com.sirh.backend.modules.paie.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class PayrollProcessingServiceSanctionTest {
    @Mock private JdbcTemplate jdbc;
    @InjectMocks private PayrollProcessingService service;

    @Test
    void rattacheLesRetenuesEnAttenteALaPeriode() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 1, 31);
        Map<String, Object> retenue = new LinkedHashMap<>();
        retenue.put("retenueId", 9);
        retenue.put("sanctionId", 5);
        retenue.put("employeId", 7);
        retenue.put("dateDebut", LocalDate.of(2026, 1, 12));
        retenue.put("dateFin", LocalDate.of(2026, 1, 14));
        retenue.put("nombreJours", 3);
        retenue.put("tauxJournalier", new BigDecimal("20000.00"));
        retenue.put("montant", new BigDecimal("60000.00"));
        retenue.put("decideParId", 1);
        when(jdbc.queryForList(contains("FROM retenues_sanction rs"), eq(start), eq(end)))
            .thenReturn(List.of(retenue));
        when(jdbc.queryForObject(contains("INSERT INTO variables_paie"), eq(Integer.class),
            any(), any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(77);

        service.attachPendingSanctions(42, start, end);

        ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
        verify(jdbc).queryForObject(contains("INSERT INTO variables_paie"), eq(Integer.class), args.capture());
        Object[] values = args.getValue();
        assertEquals(42, values[0]);
        assertEquals(7, values[1]);
        assertEquals(3, values[2]);
        assertEquals(new BigDecimal("20000.00"), values[3]);
        assertEquals(new BigDecimal("60000.00"), values[4]);
        assertEquals(1, values[6]);
        assertEquals(1, values[7]);
        assertEquals(5, values[9]);
        verify(jdbc).update(contains("UPDATE retenues_sanction SET variable_paie_id"), eq(77), eq(9));
    }

    @Test
    void aucuneRetenueEnAttenteNInsereRien() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 1, 31);
        when(jdbc.queryForList(contains("FROM retenues_sanction rs"), eq(start), eq(end))).thenReturn(List.of());

        service.attachPendingSanctions(42, start, end);

        verify(jdbc, never()).queryForObject(contains("INSERT INTO variables_paie"), eq(Integer.class),
            any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
        verify(jdbc, never()).update(contains("UPDATE retenues_sanction SET variable_paie_id"), any(), any());
    }
}
