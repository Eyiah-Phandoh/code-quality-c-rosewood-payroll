package com.amalitech.training.rosewood.payroll.tax;

import static com.amalitech.training.rosewood.payroll.validation.Inputs.requireNonNegative;

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
        for (int index = 0; index < bands.size() - 1; index++) {
            TaxBand band = bands.get(index);
            if (band.isUnbounded()) {
                throw new IllegalArgumentException(
                        "Only the last tax band may be unbounded: " + band.name());
            }
            BigDecimal bound = band.upperBoundExclusive();
            BigDecimal nextBound = bands.get(index + 1).upperBoundExclusive();
            if (bound != null && nextBound != null && nextBound.compareTo(bound) <= 0) {
                throw new IllegalArgumentException(
                        "Tax bands must have strictly increasing upper bounds: " + band.name());
            }
        }
        this.bands = List.copyOf(bands);
    }

    @Override
    public BigDecimal calculateTax(BigDecimal grossPay) {
        requireNonNegative(grossPay, "Gross pay");

        return bands.stream()
                .filter(band -> band.appliesTo(grossPay))
                .findFirst()
                .map(band -> Money.round(band.calculateTax(grossPay)))
                .orElseThrow(() -> new TaxCalculationException(
                        "No tax band applies to gross pay " + grossPay));
    }
}
