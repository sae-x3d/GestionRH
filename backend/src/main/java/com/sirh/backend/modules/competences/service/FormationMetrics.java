package com.sirh.backend.modules.competences.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class FormationMetrics {
    private FormationMetrics() {
    }

    public static int gap(int expected, int acquired) {
        return Math.max(0, expected - acquired);
    }

    public static int missing(int expected, int before) {
        return Math.max(0, expected - before);
    }

    public static int acquired(int before, int after, int missing) {
        return Math.max(0, Math.min(after - before, missing));
    }

    public static BigDecimal percentage(int expected, int before, int after) {
        int missing = missing(expected, before);
        if (missing == 0) {
            return new BigDecimal("100.00");
        }
        int acquired = acquired(before, after, missing);
        return BigDecimal.valueOf(acquired)
            .multiply(BigDecimal.valueOf(100))
            .divide(BigDecimal.valueOf(missing), 2, RoundingMode.HALF_UP)
            .min(new BigDecimal("100.00"));
    }
}
