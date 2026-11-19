package com.amalitech.training.rosewood.payroll.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.amalitech.training.rosewood.payroll.exception.InvalidPayrollInputException;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class StandardGrossPayCalculatorTest {

    private GrossPayCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new StandardGrossPayCalculator();
    }

    @ParameterizedTest(name = "{0} hours at {1} -> {2}")
    @CsvSource({
            "0, 25, 0.00",
            "1, 25, 25.00",
            "39.99, 10, 399.90",
            "40, 20, 800.00",
            "40.01, 20, 800.30",
            "41, 20, 830.00",
            "50, 30, 1650.00",
            "37.5, 13.33, 499.88"
    })
    void calculatesGrossPay(BigDecimal hours, BigDecimal rate, BigDecimal expected) {
        assertEquals(expected, calculator.calculate(hours, rate));
    }

    @Test
    void paysOvertimeOnlyAboveFortyHours() {
        BigDecimal atThreshold = calculator.calculate(new BigDecimal("40"), new BigDecimal("20"));
        BigDecimal aboveThreshold =
                calculator.calculate(new BigDecimal("40.01"), new BigDecimal("20"));

        assertAll(
                () -> assertEquals(new BigDecimal("800.00"), atThreshold),
                () -> assertEquals(new BigDecimal("800.30"), aboveThreshold));
    }

    @Test
    void appliesAnInjectedOvertimePolicy() {
        StandardGrossPayCalculator doubleTimeAfter35 =
                new StandardGrossPayCalculator(new BigDecimal("35"), new BigDecimal("2"));

        assertEquals(
                new BigDecimal("900.00"),
                doubleTimeAfter35.calculate(new BigDecimal("40"), new BigDecimal("20")));
    }

    @Test
    void acceptsAMultiplierOfExactlyOne() {
        StandardGrossPayCalculator noOvertimePremium =
                new StandardGrossPayCalculator(new BigDecimal("40"), BigDecimal.ONE);

        assertEquals(
                new BigDecimal("1000.00"),
                noOvertimePremium.calculate(new BigDecimal("50"), new BigDecimal("20")));
    }

    @Test
    void rejectsAnInvalidOvertimePolicy() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new StandardGrossPayCalculator(BigDecimal.ZERO, new BigDecimal("1.5"))),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new StandardGrossPayCalculator(null, new BigDecimal("1.5"))),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new StandardGrossPayCalculator(new BigDecimal("40"), new BigDecimal("0.99"))),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new StandardGrossPayCalculator(new BigDecimal("40"), null)));
    }

    @Test
    void rejectsNullInputs() {        assertAll(
                () -> assertThrows(
                        InvalidPayrollInputException.class,
                        () -> calculator.calculate(null, new BigDecimal("20"))),
                () -> assertThrows(
                        InvalidPayrollInputException.class,
                        () -> calculator.calculate(new BigDecimal("40"), null)));
    }

    @Test
    void rejectsNegativeInputs() {
        assertAll(
                () -> assertThrows(
                        InvalidPayrollInputException.class,
                        () -> calculator.calculate(new BigDecimal("-0.01"), new BigDecimal("20"))),
                () -> assertThrows(
                        InvalidPayrollInputException.class,
                        () -> calculator.calculate(new BigDecimal("40"), new BigDecimal("-0.01"))));
    }
}
