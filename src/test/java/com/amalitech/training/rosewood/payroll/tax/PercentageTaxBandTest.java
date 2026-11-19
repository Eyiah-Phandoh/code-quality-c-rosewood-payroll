package com.amalitech.training.rosewood.payroll.tax;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amalitech.training.rosewood.payroll.exception.InvalidPayrollInputException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PercentageTaxBandTest {

    private static final BigDecimal LOW_UPPER_BOUND = new BigDecimal("1000");
    private static final BigDecimal FIVE_PERCENT = new BigDecimal("0.05");

    private final PercentageTaxBand lowBand =
            new PercentageTaxBand("LOW", LOW_UPPER_BOUND, FIVE_PERCENT);

    @Test
    void exposesItsName() {
        assertEquals("LOW", lowBand.name());
    }

    @Test
    void acceptsZeroAndFullRatesAtTheirBoundaries() {
        PercentageTaxBand zeroRate =
                new PercentageTaxBand("ZERO", null, BigDecimal.ZERO);
        PercentageTaxBand fullRate =
                new PercentageTaxBand("FULL", null, BigDecimal.ONE);

        assertAll(
                () -> assertEquals(
                        0,
                        zeroRate.calculateTax(new BigDecimal("100")).compareTo(BigDecimal.ZERO)),
                () -> assertEquals(
                        0,
                        fullRate.calculateTax(new BigDecimal("100"))
                                .compareTo(new BigDecimal("100"))));
    }

    @Test
    void appliesBelowItsExclusiveUpperBound() {
        assertAll(
                () -> assertTrue(lowBand.appliesTo(new BigDecimal("0"))),
                () -> assertTrue(lowBand.appliesTo(new BigDecimal("999.99"))),
                () -> assertFalse(lowBand.appliesTo(new BigDecimal("1000"))));
    }

    @Test
    void unboundedBandAppliesToEveryNonNegativeGrossAmount() {
        PercentageTaxBand highBand =
                new PercentageTaxBand("HIGH", null, new BigDecimal("0.25"));

        assertAll(
                () -> assertTrue(highBand.appliesTo(new BigDecimal("1000000"))),
                () -> assertFalse(lowBand.isUnbounded()),
                () -> assertTrue(highBand.isUnbounded()));
    }

    @Test
    void calculatesExactPercentageBeforeRounding() {
        assertEquals(new BigDecimal("49.9995"), lowBand.calculateTax(new BigDecimal("999.99")));
    }

    @Test
    void rejectsInvalidConfiguration() {
        assertAll(
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> new PercentageTaxBand(null, LOW_UPPER_BOUND, FIVE_PERCENT)),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> new PercentageTaxBand(" ", LOW_UPPER_BOUND, FIVE_PERCENT)),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> new PercentageTaxBand("LOW", LOW_UPPER_BOUND, null)),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> new PercentageTaxBand(
                                "LOW", LOW_UPPER_BOUND, new BigDecimal("-0.01"))),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> new PercentageTaxBand(
                                "LOW", LOW_UPPER_BOUND, new BigDecimal("1.01"))),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> new PercentageTaxBand("LOW", BigDecimal.ZERO, FIVE_PERCENT)),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> new PercentageTaxBand(
                                "LOW", new BigDecimal("-1"), FIVE_PERCENT)));
    }

    @Test
    void rejectsNullAndNegativeGrossPay() {
        assertAll(
                () -> assertThrows(
                        InvalidPayrollInputException.class,
                        () -> lowBand.appliesTo(null)),
                () -> assertThrows(
                        InvalidPayrollInputException.class,
                        () -> lowBand.appliesTo(new BigDecimal("-0.01"))),
                () -> assertThrows(
                        InvalidPayrollInputException.class,
                        () -> lowBand.calculateTax(null)),
                () -> assertThrows(
                        InvalidPayrollInputException.class,
                        () -> lowBand.calculateTax(new BigDecimal("-0.01"))));
    }
}
