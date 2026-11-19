package com.amalitech.training.rosewood.payroll.service;

import static com.amalitech.training.rosewood.payroll.validation.Inputs.requireNonNegative;

import com.amalitech.training.rosewood.payroll.money.Money;
import java.math.BigDecimal;

/**
 * Gross pay under the inherited rules: hours above a standard week are paid at 1.5x.
 */
public final class StandardGrossPayCalculator implements GrossPayCalculator {

    public static final BigDecimal STANDARD_WEEKLY_HOURS = new BigDecimal("40");
    public static final BigDecimal OVERTIME_MULTIPLIER = new BigDecimal("1.5");

    private final BigDecimal standardHours;
    private final BigDecimal overtimeMultiplier;

    public StandardGrossPayCalculator() {
        this(STANDARD_WEEKLY_HOURS, OVERTIME_MULTIPLIER);
    }

    public StandardGrossPayCalculator(BigDecimal standardHours, BigDecimal overtimeMultiplier) {
        if (standardHours == null || standardHours.signum() <= 0) {
            throw new IllegalArgumentException("Standard hours must be positive: " + standardHours);
        }
        if (overtimeMultiplier == null || overtimeMultiplier.compareTo(BigDecimal.ONE) < 0) {
            throw new IllegalArgumentException(
                    "Overtime multiplier must be at least 1: " + overtimeMultiplier);
        }
        this.standardHours = standardHours;
        this.overtimeMultiplier = overtimeMultiplier;
    }

    @Override
    public BigDecimal calculate(BigDecimal hoursWorked, BigDecimal hourlyRate) {
        requireNonNegative(hoursWorked, "Hours worked");
        requireNonNegative(hourlyRate, "Hourly rate");

        BigDecimal regularHours = hoursWorked.min(standardHours);
        BigDecimal overtimeHours = hoursWorked.subtract(standardHours)
                .max(BigDecimal.ZERO);

        BigDecimal regularPay = regularHours.multiply(hourlyRate);
        BigDecimal overtimePay = overtimeHours.multiply(hourlyRate)
                .multiply(overtimeMultiplier);

        return Money.round(regularPay.add(overtimePay));
    }
}
