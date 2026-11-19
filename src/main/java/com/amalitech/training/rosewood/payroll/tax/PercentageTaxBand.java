package com.amalitech.training.rosewood.payroll.tax;

import static com.amalitech.training.rosewood.payroll.validation.Inputs.requireNonNegative;

import java.math.BigDecimal;

/**
 * A tax band that charges a flat percentage while the gross amount is below an exclusive upper
 * bound. A null upper bound makes the band unbounded.
 */
public final class PercentageTaxBand implements TaxBand {

    private final String name;
    private final BigDecimal upperBoundExclusive;
    private final BigDecimal rate;

    public PercentageTaxBand(String name, BigDecimal upperBoundExclusive, BigDecimal rate) {
        this.name = validateName(name);
        this.upperBoundExclusive = validateUpperBound(upperBoundExclusive);
        this.rate = validateRate(rate);
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public boolean appliesTo(BigDecimal grossPay) {
        requireNonNegative(grossPay, "Gross pay");
        return upperBoundExclusive == null || grossPay.compareTo(upperBoundExclusive) < 0;
    }

    @Override
    public boolean isUnbounded() {
        return upperBoundExclusive == null;
    }

    @Override
    public BigDecimal upperBoundExclusive() {
        return upperBoundExclusive;
    }

    @Override
    public BigDecimal calculateTax(BigDecimal grossPay) {
        requireNonNegative(grossPay, "Gross pay");
        return grossPay.multiply(rate);
    }

    private static String validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Tax band name must not be null or blank");
        }
        return name;
    }

    private static BigDecimal validateUpperBound(BigDecimal upperBoundExclusive) {
        if (upperBoundExclusive != null && upperBoundExclusive.signum() <= 0) {
            throw new IllegalArgumentException(
                    "Tax band upper bound must be positive: " + upperBoundExclusive);
        }
        return upperBoundExclusive;
    }

    private static BigDecimal validateRate(BigDecimal rate) {
        if (rate == null) {
            throw new IllegalArgumentException("Tax rate must not be null");
        }
        if (rate.signum() < 0 || rate.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("Tax rate must be between 0 and 1: " + rate);
        }
        return rate;
    }
}
