package com.amalitech.training.rosewood.payroll.deduction;

import com.amalitech.training.rosewood.payroll.domain.Employee;
import java.math.BigDecimal;

/**
 * Calculates the deductions that apply to an employee's gross pay.
 *
 * <p>The inherited application defines no deduction policy, so production uses a zero-deduction
 * implementation. A future policy can be added without changing payroll orchestration.</p>
 */
public interface DeductionCalculator {

    /**
     * @param employee the employee being paid
     * @param grossPay the calculated gross pay
     * @return the deduction amount, which must not be negative
     */
    BigDecimal calculateDeductions(Employee employee, BigDecimal grossPay);
}
