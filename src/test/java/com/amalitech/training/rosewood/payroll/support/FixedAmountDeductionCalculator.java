package com.amalitech.training.rosewood.payroll.support;

import com.amalitech.training.rosewood.payroll.deduction.DeductionCalculator;
import com.amalitech.training.rosewood.payroll.domain.Employee;
import java.math.BigDecimal;

/**
 * Test-only deduction policy that proves deduction amounts flow through the service.
 */
public final class FixedAmountDeductionCalculator implements DeductionCalculator {

    private final BigDecimal amount;

    public FixedAmountDeductionCalculator(BigDecimal amount) {
        this.amount = amount;
    }

    @Override
    public BigDecimal calculateDeductions(Employee employee, BigDecimal grossPay) {
        return amount;
    }
}
