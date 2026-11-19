package com.amalitech.training.rosewood.payroll.domain;

import com.amalitech.training.rosewood.payroll.exception.PayrollCalculationException;
import com.amalitech.training.rosewood.payroll.money.Money;
import com.amalitech.training.rosewood.payroll.validation.Inputs;
import java.math.BigDecimal;

/**
 * Immutable breakdown of a completed payroll calculation.
 *
 * <p>Net pay is derived from the reported components rather than stored separately, so a payslip
 * cannot report a net amount that disagrees with its gross, tax, and deductions.</p>
 */
public record PayrollResult(
        String employeeId,
        String employeeName,
        BigDecimal hoursWorked,
        BigDecimal hourlyRate,
        BigDecimal grossPay,
        BigDecimal tax,
        BigDecimal deductions) {

    public PayrollResult {
        employeeId = requireText(employeeId, "Employee id");
        employeeName = requireText(employeeName, "Employee name");
        hoursWorked = Inputs.requireNonNegative(hoursWorked, "Hours worked");
        hourlyRate = Inputs.requireNonNegative(hourlyRate, "Hourly rate");
        grossPay = requireMoney(grossPay, "Gross pay");
        tax = requireMoney(tax, "Tax");
        deductions = requireMoney(deductions, "Deductions");

        if (tax.add(deductions).compareTo(grossPay) > 0) {
            throw new PayrollCalculationException(
                    "Tax and deductions exceed gross pay for employee " + employeeId
                            + ": gross=" + grossPay
                            + ", tax=" + tax
                            + ", deductions=" + deductions);
        }
    }

    /**
     * Calculates net pay from the reported gross, tax, and deduction amounts.
     *
     * @return gross pay minus tax and deductions
     */
    public BigDecimal netPay() {
        return grossPay.subtract(tax).subtract(deductions);
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new PayrollCalculationException(fieldName + " must not be null or blank");
        }
        return value;
    }

    private static BigDecimal requireNonNegative(BigDecimal value, String fieldName) {
        if (value == null) {
            throw new PayrollCalculationException(fieldName + " must not be null");
        }
        if (value.signum() < 0) {
            throw new PayrollCalculationException(fieldName + " must not be negative: " + value);
        }
        return value;
    }

    private static BigDecimal requireMoney(BigDecimal value, String fieldName) {
        return Money.round(requireNonNegative(value, fieldName));
    }
}
