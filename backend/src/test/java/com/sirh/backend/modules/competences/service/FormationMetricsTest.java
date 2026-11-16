package com.sirh.backend.modules.competences.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class FormationMetricsTest {
    @Test
    void gapIsZeroWhenExpectedIsReached() {
        assertThat(FormationMetrics.gap(3, 4)).isZero();
        assertThat(FormationMetrics.gap(3, 3)).isZero();
        assertThat(FormationMetrics.gap(3, 1)).isEqualTo(2);
    }

    @Test
    void percentageMeasuresOnlyTheInitialMissingPart() {
        assertThat(FormationMetrics.percentage(4, 2, 3)).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(FormationMetrics.percentage(4, 2, 4)).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(FormationMetrics.percentage(4, 2, 2)).isEqualByComparingTo(new BigDecimal("0.00"));
    }

    @Test
    void percentageIsCappedAndCompleteWhenNothingWasMissing() {
        assertThat(FormationMetrics.percentage(3, 3, 5)).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(FormationMetrics.percentage(3, 5, 8)).isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    void acquiredNeverExceedsTheInitialGap() {
        assertThat(FormationMetrics.acquired(2, 5, 2)).isEqualTo(2);
        assertThat(FormationMetrics.acquired(2, 1, 2)).isZero();
        assertThat(FormationMetrics.missing(4, 6)).isZero();
    }
}
