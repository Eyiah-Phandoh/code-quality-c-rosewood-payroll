package com.amalitech.training.rosewood.payroll.tax;

import java.math.BigDecimal;
import java.util.List;

/**
 * The inherited Rosewood tax policy: a flat percentage selected from gross pay.
 */
public final class StandardTaxBands {

    private static final BigDecimal LOW_UPPER_BOUND = new BigDecimal("1000");
    private static final BigDecimal MEDIUM_UPPER_BOUND = new BigDecimal("5000");
    private static final BigDecimal LOW_RATE = new BigDecimal("0.05");
    private static final BigDecimal MEDIUM_RATE = new BigDecimal("0.15");
    private static final BigDecimal HIGH_RATE = new BigDecimal("0.25");

    private StandardTaxBands() {
    }

    /**
     * @return the existing low, medium, and high bands in evaluation order
     */
    public static List<TaxBand> create() {
        return List.of(
                new PercentageTaxBand("LOW_BAND", LOW_UPPER_BOUND, LOW_RATE),
                new PercentageTaxBand("MEDIUM_BAND", MEDIUM_UPPER_BOUND, MEDIUM_RATE),
                new PercentageTaxBand("HIGH_BAND", null, HIGH_RATE));
    }
}
