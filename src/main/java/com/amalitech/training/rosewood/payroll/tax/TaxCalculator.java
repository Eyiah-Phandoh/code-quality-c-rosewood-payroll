package com.amalitech.training.rosewood.payroll.tax;

import java.math.BigDecimal;

/**
 * Calculates the tax owed on a gross payroll amount.
 */
public interface TaxCalculator {

    /**
     * @param grossPay the non-negative gross amount
     * @return the tax amount, rounded to the payroll monetary scale
     */
    BigDecimal calculateTax(BigDecimal grossPay);
}
