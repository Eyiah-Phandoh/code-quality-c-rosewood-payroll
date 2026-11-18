package com.amalitech.training.rosewood.payroll.tax;

import java.math.BigDecimal;

/**
 * One tax rule that can decide whether it applies to a gross amount and calculate its tax.
 *
 * <p>Implementations may use any calculation rule; the payroll service depends only on this
 * contract, so a new band does not require changes to payroll orchestration.</p>
 */
public interface TaxBand {

    /**
     * @return a human-readable name for the band
     */
    String name();

    /**
     * @param grossPay the non-negative gross amount to test
     * @return true when this band applies to the amount
     */
    boolean appliesTo(BigDecimal grossPay);

    /**
     * Calculates the exact tax for this band. Rounding is applied by the tax calculator.
     *
     * @param grossPay the non-negative gross amount
     * @return the exact tax before monetary rounding
     */
    BigDecimal calculateTax(BigDecimal grossPay);
}
