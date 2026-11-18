package com.amalitech.training.rosewood.payroll.exception;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class TaxCalculationExceptionTest {

    @Test
    void preservesMessageAndCause() {
        IllegalStateException cause = new IllegalStateException("configuration missing");

        TaxCalculationException exception =
                new TaxCalculationException("Tax calculation failed", cause);

        assertAll(
                () -> assertEquals("Tax calculation failed", exception.getMessage()),
                () -> assertSame(cause, exception.getCause()));
    }
}
