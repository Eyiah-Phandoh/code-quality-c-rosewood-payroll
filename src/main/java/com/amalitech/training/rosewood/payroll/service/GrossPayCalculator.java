package com.amalitech.training.rosewood.payroll.service;

import com.amalitech.training.rosewood.payroll.exception.InvalidPayrollInputException;
import com.amalitech.training.rosewood.payroll.money.Money;
import java.math.BigDecimal;

/**
 * Calculates gross pay, including the inherited overtime rule.
 */
public final class GrossPayCalculator {

    public static final BigDecimal STANDARD_WEEKLY_HOURS = new BigDecimal("40");
    public static final BigDecimal OVERTIME_MULTIPLIER = new BigDecimal("1.5");

    /**
     * @param hoursWorked non-negative hours worked
     * @param hourlyRate non-negative hourly rate
     * @return gross pay rounded to the payroll monetary scale
     */
    public BigDecimal calculate(BigDecimal hoursWorked, BigDecimal hourlyRate) {
        requireNonNegative(hoursWorked, "Hours worked");
        requireNonNegative(hourlyRate, "Hourly rate");

        BigDecimal regularHours = hoursWorked.min(STANDARD_WEEKLY_HOURS);
        BigDecimal overtimeHours = hoursWorked.subtract(STANDARD_WEEKLY_HOURS)
                .max(BigDecimal.ZERO);

        BigDecimal regularPay = regularHours.multiply(hourlyRate);
        BigDecimal overtimePay = overtimeHours.multiply(hourlyRate)
                .multiply(OVERTIME_MULTIPLIER);

        return Money.round(regularPay.add(overtimePay));
    }

    private static void requireNonNegative(BigDecimal value, String fieldName) {
        if (value == null) {
            throw new InvalidPayrollInputException(fieldName + " must not be null");
        }
        if (value.signum() < 0) {
            throw new InvalidPayrollInputException(fieldName + " must not be negative: " + value);
        }
    }
}
