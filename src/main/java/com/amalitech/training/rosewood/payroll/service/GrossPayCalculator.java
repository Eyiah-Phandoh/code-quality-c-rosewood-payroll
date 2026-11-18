package com.amalitech.training.rosewood.payroll.service;

import java.math.BigDecimal;

/**
 * Calculates gross pay from hours worked and an hourly rate.
 */
public interface GrossPayCalculator {

    /**
     * @param hoursWorked non-negative hours worked
     * @param hourlyRate non-negative hourly rate
     * @return gross pay rounded to the payroll monetary scale
     */
    BigDecimal calculate(BigDecimal hoursWorked, BigDecimal hourlyRate);
}
