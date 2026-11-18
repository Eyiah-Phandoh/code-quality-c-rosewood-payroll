package com.amalitech.training.rosewood.payroll.deduction;

import com.amalitech.training.rosewood.payroll.domain.Employee;
import com.amalitech.training.rosewood.payroll.money.Money;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Preserves the inherited behaviour: no deductions are applied.
 */
public final class NoDeductionsCalculator implements DeductionCalculator {

    @Override
    public BigDecimal calculateDeductions(Employee employee, BigDecimal grossPay) {
        Objects.requireNonNull(employee, "employee must not be null");
        Objects.requireNonNull(grossPay, "grossPay must not be null");
        return Money.ZERO;
    }
}
