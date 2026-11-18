package com.amalitech.training.rosewood.payroll.support;

import com.amalitech.training.rosewood.payroll.tax.TaxBand;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Test-only tax band that applies at or above an inclusive gross threshold. It exists to show
 * that a band with a different shape can be added without changing payroll orchestration.
 */
public final class MinimumGrossTaxBand implements TaxBand {

    private final String name;
    private final BigDecimal minimumGrossInclusive;
    private final BigDecimal rate;

    public MinimumGrossTaxBand(
            String name, BigDecimal minimumGrossInclusive, BigDecimal rate) {
        this.name = Objects.requireNonNull(name, "Band name");
        this.minimumGrossInclusive =
                Objects.requireNonNull(minimumGrossInclusive, "Minimum gross");
        this.rate = Objects.requireNonNull(rate, "Rate");
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public boolean appliesTo(BigDecimal grossPay) {
        return grossPay.compareTo(minimumGrossInclusive) >= 0;
    }

    @Override
    public BigDecimal calculateTax(BigDecimal grossPay) {
        return grossPay.multiply(rate);
    }
}
