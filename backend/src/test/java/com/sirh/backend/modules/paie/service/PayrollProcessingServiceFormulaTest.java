package com.sirh.backend.modules.paie.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PayrollProcessingServiceFormulaTest {
    @Test
    void miseAPiedReduitLeBrutEtLesBasesSansChangerLeNet() {
        List<Map<String, Object>> lines = new ArrayList<>();
        line(lines, "SALAIRE_BASE", "GAIN", "1000.00");
        line(lines, "RETENUE_ABSENCE", "RETENUE_SALARIALE", "100.00");
        line(lines, "RETENUE_MISE_A_PIED", "RETENUE_SALARIALE", "200.00");
        line(lines, "CNAPS_SALARIAL", "RETENUE_SALARIALE", "50.00");

        BigDecimal gross = PayrollProcessingService.computeGross(lines);
        BigDecimal net = PayrollProcessingService.computeNet(gross, lines);

        assertEquals(new BigDecimal("700.00"), gross);
        assertEquals(new BigDecimal("650.00"), net);
    }

    private void line(List<Map<String, Object>> lines, String code, String category, String amount) {
        Map<String, Object> line = new LinkedHashMap<>();
        line.put("code", code);
        line.put("category", category);
        line.put("amount", new BigDecimal(amount));
        lines.add(line);
    }
}
