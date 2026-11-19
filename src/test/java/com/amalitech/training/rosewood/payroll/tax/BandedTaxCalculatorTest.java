package com.amalitech.training.rosewood.payroll.tax;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.amalitech.training.rosewood.payroll.exception.InvalidPayrollInputException;
import com.amalitech.training.rosewood.payroll.exception.TaxCalculationException;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class BandedTaxCalculatorTest {

    private TaxCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new BandedTaxCalculator(StandardTaxBands.create());
    }

    @ParameterizedTest(name = "gross {0} -> tax {1}")
    @CsvSource({
            "0, 0.00",
            "800, 40.00",
            "999.99, 50.00",
            "1000, 150.00",
            "4999.99, 750.00",
            "5000, 1250.00",
            "10000, 2500.00"
    })
    void appliesTheFirstMatchingBand(BigDecimal grossPay, BigDecimal expectedTax) {
        assertEquals(expectedTax, calculator.calculateTax(grossPay));
    }

    @Test
    void roundsTaxAtThePayrollScale() {
        assertEquals(new BigDecimal("50.00"), calculator.calculateTax(new BigDecimal("999.99")));
    }

    @Test
    void failsWhenNoBandApplies() {
        TaxCalculator onlyLowBand = new BandedTaxCalculator(List.of(
                new PercentageTaxBand("LOW", new BigDecimal("1000"), new BigDecimal("0.05"))));

        assertThrows(
                TaxCalculationException.class,
                () -> onlyLowBand.calculateTax(new BigDecimal("1000")));
    }

    @Test
    void rejectsMissingOrEmptyBandConfiguration() {
        assertAll(
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> new BandedTaxCalculator(null)),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> new BandedTaxCalculator(List.of())),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> new BandedTaxCalculator(java.util.Arrays.asList(
                                new PercentageTaxBand(
                                        "LOW", new BigDecimal("1000"), new BigDecimal("0.05")),
                                null))));
    }

    @Test
    void rejectsAnUnboundedBandThatIsNotLast() {
        TaxBand unboundedBand =
                new PercentageTaxBand("HIGH", null, new BigDecimal("0.25"));
        TaxBand boundedBand = new PercentageTaxBand(
                "LOW", new BigDecimal("1000"), new BigDecimal("0.05"));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new BandedTaxCalculator(List.of(unboundedBand, boundedBand)));

        assertEquals("Only the last tax band may be unbounded: HIGH", exception.getMessage());
    }

    @Test
    void acceptsAnUnboundedBandAsTheLastBand() {
        TaxBand boundedBand = new PercentageTaxBand(
                "LOW", new BigDecimal("1000"), new BigDecimal("0.05"));
        TaxBand unboundedBand =
                new PercentageTaxBand("HIGH", null, new BigDecimal("0.25"));

        TaxCalculator calculatorWithUnboundedLast =
                new BandedTaxCalculator(List.of(boundedBand, unboundedBand));

        assertEquals(
                new BigDecimal("2500.00"),
                calculatorWithUnboundedLast.calculateTax(new BigDecimal("10000")));
    }

    @Test
    void rejectsNullAndNegativeGrossPay() {
        assertAll(
                () -> assertThrows(
                        InvalidPayrollInputException.class,
                        () -> calculator.calculateTax(null)),
                () -> assertThrows(
                        InvalidPayrollInputException.class,
                        () -> calculator.calculateTax(new BigDecimal("-0.01"))));
    }

    @Test
    void validatesGrossPayBeforeDelegatingToBands() {
        TaxBand permissiveBand = new TaxBand() {
            @Override
            public String name() {
                return "PERMISSIVE";
            }

            @Override
            public boolean appliesTo(BigDecimal grossPay) {
                return true;
            }

            @Override
            public boolean isUnbounded() {
                return true;
            }

            @Override
            public BigDecimal calculateTax(BigDecimal grossPay) {
                return BigDecimal.ZERO;
            }
        };
        TaxCalculator permissiveCalculator =
                new BandedTaxCalculator(List.of(permissiveBand));

        assertAll(
                () -> assertThrows(
                        InvalidPayrollInputException.class,
                        () -> permissiveCalculator.calculateTax(null)),
                () -> assertThrows(
                        InvalidPayrollInputException.class,
                        () -> permissiveCalculator.calculateTax(new BigDecimal("-0.01"))));
    }

    @Test
    void supportsACustomBandThroughTheSameAbstraction() {
        TaxBand flatBand = new TaxBand() {
            @Override
            public String name() {
                return "FLAT";
            }

            @Override
            public boolean appliesTo(BigDecimal grossPay) {
                return grossPay.compareTo(new BigDecimal("10000")) >= 0;
            }

            @Override
            public boolean isUnbounded() {
                return true;
            }

            @Override
            public BigDecimal calculateTax(BigDecimal grossPay) {
                return new BigDecimal("5000");
            }
        };
        TaxCalculator customCalculator = new BandedTaxCalculator(List.of(flatBand));

        assertEquals(
                new BigDecimal("5000.00"),
                customCalculator.calculateTax(new BigDecimal("12000")));
    }
}
