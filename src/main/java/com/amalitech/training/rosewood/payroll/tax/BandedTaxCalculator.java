package com.amalitech.training.rosewood.payroll.tax;

import com.amalitech.training.rosewood.payroll.exception.InvalidPayrollInputException;
import com.amalitech.training.rosewood.payroll.exception.TaxCalculationException;
import com.amalitech.training.rosewood.payroll.money.Money;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Selects the first applicable band from an ordered list and applies monetary rounding.
 */
public final class BandedTaxCalculator implements TaxCalculator {

    private final List<TaxBand> bands;

    public BandedTaxCalculator(List<TaxBand> bands) {
        if (bands == null || bands.isEmpty()) {
            throw new IllegalArgumentException("At least one tax band is required");
        }
        if (bands.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Tax bands must not contain null");
        }
        this.bands = List.copyOf(bands);
    }

    @Override
    public BigDecimal calculateTax(BigDecimal grossPay) {
        requireValidGrossPay(grossPay);

        return bands.stream()
                .filter(band -> band.appliesTo(grossPay))
                .findFirst()
                .map(band -> Money.round(band.calculateTax(grossPay)))
                .orElseThrow(() -> new TaxCalculationException(
                        "No tax band applies to gross pay " + grossPay));
    }

    private static void requireValidGrossPay(BigDecimal grossPay) {
        if (grossPay == null) {
            throw new InvalidPayrollInputException("Gross pay must not be null");
        }
        if (grossPay.signum() < 0) {
            throw new InvalidPayrollInputException("Gross pay must not be negative: " + grossPay);
        }
    }
}
